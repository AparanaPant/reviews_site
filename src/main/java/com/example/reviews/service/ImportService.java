package com.example.reviews.service;

import com.example.reviews.config.AppProperties;
import com.example.reviews.model.entity.Review;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Service
public class ImportService {
    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final AppProperties props;
    private final ReviewService reviewService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public ImportService(AppProperties props, ReviewService reviewService) {
        this.props = props;
        this.reviewService = reviewService;
    }

    public int importAll() {
        if (props.getUrl() == null || props.getKey() == null) {
            log.warn("Review API url/key not configured; skipping import");
            return 0;
        }
        int count = 0;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("x-api-key", props.getKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(props.getUrl(), HttpMethod.GET, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("Upstream returned non-200: {}", resp.getStatusCodeValue());
                return 0;
            }
            JsonNode root = mapper.readTree(resp.getBody());

            // Accept either an array of reviews or an object with "data"
            JsonNode list = root.isArray() ? root : root.get("reviews");
            if (list == null || !list.isArray()) {
                log.warn("Unexpected upstream format; skipping");
                return 0;
            }

            for (JsonNode n : list) {
                String externalId = text(n, "id");
                String source = text(n, "source");
                String author = text(n, "author");
                Integer rating = n.hasNonNull("rating") ? n.get("rating").asInt() : null;
                String content = text(n, "content");
                LocalDateTime reviewDate = parseDate(text(n, "reviewDate"));
                Set<String> tags = new HashSet<>();
                if (n.has("tags") && n.get("tags").isArray()) {
                    for (JsonNode t : n.get("tags")) {
                        if (t != null && !t.isNull()) tags.add(t.asText());
                    }
                }
                if (externalId == null || source == null) continue;
                if (rating != null && (rating < 1 || rating > 5)) continue;

                Review saved = reviewService.upsert(source, externalId, author, rating, content, reviewDate, tags);
                count++;
            }
            log.info("Import finished. {} reviews processed.", count);
        } catch (Exception e) {
            log.error("Import failed", e);
        }
        return count;
    }

    private String text(JsonNode n, String field) {
        return (n != null && n.hasNonNull(field)) ? n.get(field).asText() : null;
    }

    private LocalDateTime parseDate(String s) {
        try {
            if (s == null) return null;
            // try OffsetDateTime first, then LocalDateTime
            try {
                return OffsetDateTime.parse(s).toLocalDateTime();
            } catch (Exception ignore) {
                return LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
            }
        } catch (Exception ex) {
            return null;
        }
    }
}
