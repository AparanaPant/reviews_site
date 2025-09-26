package com.example.reviews.service;

import com.example.reviews.config.AppProperties;
import com.example.reviews.model.upstream.ReviewInDto;
import com.example.reviews.model.upstream.ReviewsEnvelopeDto;
import com.example.reviews.repository.BulkReviewRepository;
import com.example.reviews.util.HttpClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pulls reviews from the upstream API and upserts them in batches.
 *
 * What this method does:
 * <ol>
 *   <li><b>Set up the run</b> – figure out page size, start page, and log where we’re pulling from.</li>
 *   <li><b>Fetch a page</b> – call the upstream with (page,size) using a small HTTP helper.</li>
 *   <li><b>Parse &amp; validate</b> – turn JSON into DTOs, then apply Bean Validation. Skip bad rows.</li>
 *   <li><b>Write</b> – bulk <i>UPSERT</i> the valid rows for that page.</li>
 *   <li><b>Log progress</b> – received, batched, skipped, and affected counts per page.</li>
 *   <li><b>Fail gracefully</b> – if something goes wrong, stop cleanly so a later retry can continue.</li>
 * </ol>
 *
 * <b>Why UPSERT (insert-or-update) instead of insert-only or skipping duplicates?</b>
 * <ul>
 *   <li>The upstream does not expose a reliable “only-new” signal (e.g., change feed, last-modified cursor,
 *       or a strict monotonic ID). That means we cannot safely assume items we’ve seen before are unchanged.</li>
 *   <li>Reviews can evolve (edits, rating changes, content corrections, tags). For a review product,
 *       reflecting those changes matters. UPSERT keeps our store in sync with upstream truth.</li>
 *   <li>A unique key like <code>(source, external_id)</code> prevents duplicates while allowing updates
 *       when upstream re-sends prior items with new values.</li>
 *   <li>In an ideal world, we’d consume an incremental feed (by timestamp, version, or cursor)
 *       and fetch only new/changed rows. Until that exists, UPSERT is the most robust choice.</li>
 * </ul>
 */
@Service
public class ReviewImportService {

    private static final Logger log = LoggerFactory.getLogger(ReviewImportService.class);

    /**
     * Default page size (500) when app.page-size is not configured.
     * This controls how many reviews are fetched and upserted in a single batch.
     * Can be overridden by setting app.page-size in application properties.
     */
    private static final int DEFAULT_PAGE_SIZE = 500;

    private final AppProperties props;            // config (URL, key, page size, etc.)
    private final ObjectMapper mapper;            // JSON -> Java DTOs
    private final BulkReviewRepository bulkWriter;    // batch upsert into DB
    private final HttpClientUtil httpClientUtil; //  HTTP client helper
    private final Validator validator;            // javax.validation for DTO constraints

    public ReviewImportService(AppProperties props,
                               BulkReviewRepository bulkWriter,
                               ObjectMapper mapper,
                               HttpClientUtil httpClientUtil,
                               Validator validator) {
        this.props = props;
        this.bulkWriter = bulkWriter;
        this.mapper = mapper;
        this.httpClientUtil = httpClientUtil;
        this.validator = validator;
    }

    /**
     * Runs bulk data import across  pages.
     *
     * @return total rows inserted/updated
     */
    public int importAll() {
        // Step 0: set up run state (these don’t change during the loop)
        final int size = (props.getPageSize() != null) ? props.getPageSize() : DEFAULT_PAGE_SIZE; // items per page
        final String baseUrl = props.getUrl();    // endpoint to hit (required)
        final String apiKey  = props.getKey();    // may be null; upstream might not require it

        int page = 1;               // assume upstream is 1-based
        int totalPages = 1;         // will be updated after the first successful page
        int totalAffected = 0;      // total rows upserted this run
        int totalSkipped = 0;       // total invalid rows skipped (across all pages)

        log.info("Starting reviews import from {} (startingPage={}, pageSize={})", baseUrl, page, size);

        // Outer guard: never let an unexpected bug crash the app
        try {
            // Step 1..5: loop pages until done or something forces us to stop
            do {
                // Per-page guard: keep context if a single page blows up
                try {
                    // Step 1: fetch one page (HttpClientHelper throws if non-2xx or empty)
                    ResponseEntity<String> resp = httpClientUtil.get(
                            baseUrl,
                            Map.of("page", page, "size", size),
                            Map.of("x-api-key", apiKey)
                    );

                    // Step 2: parse + validate in one pass
                    PageProcessResult pr = parseAndValidate(resp.getBody());
                    if (pr.received() == 0) {
                        log.warn("Page {}: upstream returned no reviews; stopping.", page);
                        break;
                    }
                    if (pr.totalPages() != null) {
                        totalPages = Math.max(1, pr.totalPages());
                    }

                    // Step 3: write valid rows
                    int affected = bulkWriter.upsertBatch(pr.good());
                    totalAffected += affected;
                    totalSkipped  += pr.skipped();

                    // Step 4: progress log
                    log.info("Page {}/{}: received={}, batched={}, skipped={}, affected={}",
                            page, totalPages, pr.received(), pr.good().size(), pr.skipped(), affected);

                    page++; // next page after a successful cycle
                } catch (Exception pageEx) {
                    // Step 5: fail gracefully per page
                    log.error("Unhandled exception while processing page {}. Stopping this run.", page, pageEx);
                    break;
                }
            } while (page <= totalPages);

            log.info("Import finished. Total affected: {} (skipped: {})", totalAffected, totalSkipped);
        } catch (Exception runEx) {
            log.error("Import run failed with an unexpected exception.", runEx);
        }

        return totalAffected;
    }

    /**
     * Parse the wrapped payload and validate each DTO with javax.validation.
     *
     * <p><b>Why validate here?</b> We batch-write an entire page in a single JDBC transaction.
     * If one row is malformed and hits a constraint, the whole batch can fail and roll back.
     * By validating first and skipping bad rows, we protect the batch and still save the good ones.
     */
    private PageProcessResult parseAndValidate(String body) {
        try {
            // Expect: {"reviews":[...], "paging":{"totalPages":N}}
            ReviewsEnvelopeDto env = mapper.readValue(body, ReviewsEnvelopeDto.class);

            List<ReviewInDto> raw = (env.reviews() != null) ? env.reviews() : List.of();
            int received = raw.size();

            // Single pass: validate and collect the good rows
            List<ReviewInDto> good = new ArrayList<>(received);
            int skipped = 0;

            for (ReviewInDto in : raw) {
                Set<ConstraintViolation<ReviewInDto>> violations = validator.validate(in);
                if (!violations.isEmpty()) {
                    skipped++;
                    // log one violation per invalid row
                    ConstraintViolation<ReviewInDto> first = violations.iterator().next();
                    log.debug("Skipping invalid review (source={}, id={}): {}",
                            in != null ? in.source() : "?",
                            in != null ? in.id() : "?",
                            first.getMessage());
                    continue;
                }
                good.add(in);
            }

            Integer tp = (env.paging() != null) ? env.paging().totalPages() : null;
            return new PageProcessResult(good, skipped, received, tp);

        } catch (Exception ex) {
            // Treat parse failures as an empty page so the caller can stop cleanly
            log.error("Failed to parse upstream response as ReviewsEnvelopeDto; treating page as empty.", ex);
            return new PageProcessResult(List.of(), 0, 0, null);
        }
    }

    /** Carrier for one page’s outcome. */
    private record PageProcessResult(
            List<ReviewInDto> good,  // rows that passed validation
            int skipped,             // how many rows we dropped
            int received,            // how many rows upstream sent us
            Integer totalPages       // page count from upstream (may be null)
    ) {}
}
