package com.example.reviews.service;

import com.example.reviews.config.AppProperties;
import com.example.reviews.model.upstream.ReviewInDto;
import com.example.reviews.model.upstream.ReviewsEnvelopeDto;
import com.example.reviews.repository.BulkReviewWriter;
import com.example.reviews.util.HttpClientHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Imports reviews from an upstream HTTP API, page by page, and performs
 * a bulk UPSERT into the database via {@link BulkReviewWriter}.
 *
 * Design goals:
 * - Be resilient to upstream payload shapes (array-at-root OR { reviews, paging }).
 * - Validate/skip malformed rows without failing the whole run.
 * - Keep each page independent (small memory footprint, simple retries).
 */
@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    /** Fallback when app.page-requestedSize is not provided. */
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final AppProperties props;
    private final ObjectMapper mapper;        // Spring Boot's pre-configured ObjectMapper
    private final BulkReviewWriter bulkWriter;
    private final HttpClientHelper httpClientHelper;

    public ImportService(AppProperties props,
                         BulkReviewWriter bulkWriter,
                         ObjectMapper mapper,
                         HttpClientHelper httpClientHelper) {
        this.props = props;
        this.bulkWriter = bulkWriter;
        this.mapper = mapper;
        this.httpClientHelper = httpClientHelper;
    }

    /**
     * Orchestrates a full import run across all pages.
     * @return number of items the writer reported as written (informational)
     */
    public int importAll() {
        // 1) Guard: config must be present
        if (props.getUrl() == null || props.getKey() == null) {
            log.warn("Review API url/key not configured; skipping import");
            return 0;
        }

        int totalAffected = 0;
        int page = 1;
        int totalPages = 1; // unknown until the first successful response
        final int size = props.getPageSize() != null ? props.getPageSize() : DEFAULT_PAGE_SIZE;

        log.info("Starting reviews import from {} (page requestedSize={})", props.getUrl(), size);

        // 2) Page loop
        try {
            do {
                // 2.1) Call the upstream endpoint for this page via reusable helper
                ResponseEntity<String> resp = httpClientHelper.get(
                        props.getUrl(),
                        Map.of("page", page, "size", size),
                        Map.of("x-api-key", props.getKey())
                );

                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                    log.error("Upstream returned non-2xx or empty body for page {}: status={}",
                            page, (resp != null ? resp.getStatusCodeValue() : "null"));
                    break; // stop the run; could be retried by caller or next startup
                }

                // 2.2) Parse payload (supports both shapes)
                ParseResult parsed = parsePayload(resp.getBody());
                if (parsed.items().isEmpty()) {
                    log.warn("Page {}: upstream returned no reviews; stopping.", page);
                    break;
                }
                if (parsed.totalPages() != null) {
                    totalPages = Math.max(1, parsed.totalPages());
                }

                // 2.3) Validate/filter current page
                ValidationResult vr = validateAndFilter(parsed.items());

                // 2.4) Bulk UPSERT this page
                int affected = bulkWriter.upsertBatch(vr.good());
                totalAffected += affected;

                // 2.5) Progress log
                log.info("Page {}/{}: received={}, batched={}, skipped={}, affected={}",
                        page, totalPages, parsed.items().size(), vr.good().size(), vr.skipped(), affected);

                page++;
            } while (page <= totalPages);

            log.info("Import finished. Total rows affected (insert/update): {}", totalAffected);
        } catch (Exception e) {
            // Catch-all to prevent a crash loop on startup; logs the root cause.
            log.error("Import failed with exception", e);
        }

        return totalAffected;
    }

    // ------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------

    /**
     * Parses either:
     *  1) Wrapped: {"reviews":[...], "paging":{"totalPages":N}}  OR
     *  2) Array-at-root: [ ... ]
     */
    private ParseResult parsePayload(String body) {
        try {
            // Try wrapped payload first
            ReviewsEnvelopeDto env = mapper.readValue(body, ReviewsEnvelopeDto.class);
            List<ReviewInDto> items = env.reviews() != null ? env.reviews() : List.of();
            Integer total = env.paging() != null ? env.paging().totalPages() : null;
            return new ParseResult(items, total);
        } catch (Exception notWrapped) {
            // Fallback: array-at-root
            try {
                List<ReviewInDto> items = mapper.readValue(body, new TypeReference<List<ReviewInDto>>() {});
                return new ParseResult(items != null ? items : List.of(), null);
            } catch (Exception badFormat) {
                log.warn("Unexpected upstream format; unable to parse payload. Falling back to empty list.", badFormat);
                return new ParseResult(List.of(), null);
            }
        }
    }

    /**
     * Applies minimal business validation rules:
     *  - id and source are required
     *  - rating, if present, must be in [1..5]
     * Skips invalid rows and returns the valid subset.
     */
    private ValidationResult validateAndFilter(List<ReviewInDto> items) {
        List<ReviewInDto> good = new ArrayList<>(items.size());
        int skipped = 0;

        for (ReviewInDto in : items) {
            if (in == null || in.id() == null || in.source() == null) {
                skipped++;
                continue;
            }
            Integer rating = in.rating();
            if (rating != null && (rating < 1 || rating > 5)) {
                skipped++;
                continue;
            }
            good.add(in);
        }
        return new ValidationResult(good, skipped);
    }

    // ------------------------------------------------------------------------------------
    // Small internal carriers to keep method signatures tidy/readable
    // ------------------------------------------------------------------------------------

    /** Holds the result of parsing a page: the items plus (optional) totalPages. */
    private record ParseResult(List<ReviewInDto> items, Integer totalPages) {}

    /** Holds the result of validation: filtered items and the skip count. */
    private record ValidationResult(List<ReviewInDto> good, int skipped) {}
}
