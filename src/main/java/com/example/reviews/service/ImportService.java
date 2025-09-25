package com.example.reviews.service;

import com.example.reviews.config.AppProperties;
import com.example.reviews.repository.BulkReviewWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportService {
    private static final Logger log = LoggerFactory.getLogger(ImportService.class);
    private static final int DEFAULT_PAGE_SIZE = 50;
    private final AppProperties props;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final BulkReviewWriter bulkWriter;

    public ImportService(AppProperties props, BulkReviewWriter bulkWriter) {
        this.props = props;
        this.bulkWriter = bulkWriter;
    }
    /**
     * Imports all pages from the upstream API.
     * Uses DB-side UPSERT in batches; no per-row existence checks in Java.
     */
    public int importAll() {
        if (props.getUrl() == null || props.getKey() == null) {
            log.warn("Review API url/key not configured; skipping import");
            return 0;
        }

        int totalAffected = 0;
        int page = 1;
        int totalPages = 1;
        final int size = props.getPageSize() != null ? props.getPageSize() : DEFAULT_PAGE_SIZE;

        log.info("Starting reviews import from {} (page size={})", props.getUrl(), size);

        try {
            do {
                String url = props.getUrl() + "?page=" + page + "&size=" + size;
                ResponseEntity<String> resp = exchange(url);
                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                    log.error("Upstream returned non-2xx for page {}: {}", page, resp.getStatusCodeValue());
                    break;
                }

                JsonNode root = mapper.readTree(resp.getBody());
                // Determine totalPages if provided
                JsonNode paging = root.path("paging");
                if (paging.hasNonNull("totalPages")) {
                    totalPages = Math.max(1, paging.get("totalPages").asInt());
                }

                // Accept either array payload or object with "reviews"
                JsonNode list = root.isArray() ? root : root.get("reviews");
                if (list == null || !list.isArray()) {
                    log.warn("Unexpected upstream format on page {}: no 'reviews' array found", page);
                    break;
                }

                // Build one batch for this page
                List<BulkReviewWriter.Row> batch = new ArrayList<>(list.size());
                int skipped = 0;

                for (JsonNode n : list) {
                    String externalId = text(n, "id");
                    String source     = text(n, "source");
                    Integer rating    = n.hasNonNull("rating") ? n.get("rating").asInt() : null;

                    if (externalId == null || source == null) {
                        skipped++;
                        continue;
                    }
                    if (rating != null && (rating < 1 || rating > 5)) {
                        skipped++;
                        continue;
                    }

                    BulkReviewWriter.Row r = new BulkReviewWriter.Row();
                    r.externalId = externalId;
                    r.source     = source;
                    r.author     = text(n, "author");
                    r.rating     = rating;
                    r.content    = text(n, "content");
                    r.reviewDate = parseDate(text(n, "reviewDate"));
                    r.tag        = extractSingleTag(n.get("tags"));
                    batch.add(r);
                }

                int affected = bulkWriter.upsertBatch(batch);
                totalAffected += affected;

                log.info("Page {}/{}: received={}, batched={}, skipped={}, affected={}",
                        page, totalPages, list.size(), batch.size(), skipped, affected);

                page++;
            } while (page <= totalPages);

            log.info("Import finished. Total rows affected (insert/update): {}", totalAffected);
        } catch (Exception e) {
            log.error("Import failed with exception", e);
        }

        return totalAffected;
    }

    // ---------- helpers ----------

    private ResponseEntity<String> exchange(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-api-key", props.getKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    private String text(JsonNode n, String field) {
        return (n != null && n.hasNonNull(field)) ? n.get(field).asText() : null;
    }

    private String extractSingleTag(JsonNode tn) {
        if (tn == null || tn.isNull()) return null;
        if (tn.isTextual()) return tn.asText(); // "SERVICE"
        if (tn.isArray()) {                     // ["SERVICE", "SALES"]
            for (JsonNode t : tn) if (t != null && t.isTextual()) return t.asText();
        }
        return null;
    }

    private LocalDateTime parseDate(String s) {
        try {
            if (s == null) return null;
            try {
                return OffsetDateTime.parse(s).toLocalDateTime();
            } catch (Exception ignore) {
                return LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
            }
        } catch (Exception ex) {
            // swallow bad dates; importer continues
            return null;
        }
    }
}
