package com.example.reviews.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * A small helper around RestTemplate for making GET calls to external APIs.
 *
 * Why we have this:
 * - Keeps all the boilerplate (query params, headers, logging, error handling) in one place.
 * - Guarantees: only returns if the response is 2xx AND body is non-empty.
 * - Reusable: not tied to "reviews", can be used anywhere we need to hit an external service.
 */
@Component
public class HttpClientUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    /** Shared RestTemplate instance (Spring manages connection handling). */
    private final RestTemplate restTemplate;

    public HttpClientUtil() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Perform a GET request against a given URL.
     *
     * Things this enforces:
     * - URL must be provided.
     * - Non-2xx or empty body is treated as an error.
     * - Logs enough info to debug if something goes wrong.
     *
     * Example:
     * <pre>
     * ResponseEntity<String> resp = httpClientUtil.get(
     *     "https://api.example.com/v1/resource",
     *     Map.of("page", 1, "size", 50),
     *     Map.of("x-api-key", "secret-key")
     * );
     * String body = resp.getBody();
     * </pre>
     */
    public ResponseEntity<String> get(String url,
                                      Map<String, Object> queryParams,
                                      Map<String, String> headers) {

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be null/blank");
        }

        // Build the full URI with query params if provided
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        URI uri = builder.build(true).toUri();

        // Add headers if caller passed any (like auth keys, etc.)
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }
        HttpEntity<Void> request = new HttpEntity<>(httpHeaders);

        try {
            ResponseEntity<String> resp =
                    restTemplate.exchange(uri, HttpMethod.GET, request, String.class);

            // Fail fast if not a success status
            if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
                int code = (resp == null ? -1 : resp.getStatusCodeValue());
                log.error("Non-2xx response: uri={} status={}", uri, code);
                throw new IllegalStateException("Non-2xx response: status=" + code);
            }

            // Fail if body is missing or blank
            String body = resp.getBody();
            if (body == null || body.isBlank()) {
                log.error("Empty body from upstream: uri={} status={}", uri, resp.getStatusCodeValue());
                throw new IllegalStateException("Empty body from upstream");
            }

            return resp;

        } catch (RestClientException ex) {
            log.error("HTTP GET failed: uri={} queryParams={}", uri, queryParams, ex);
            throw new IllegalStateException("HTTP GET transport failure", ex);
        }
    }
}
