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

@Component
public class HttpClientHelper {

    private static final Logger log = LoggerFactory.getLogger(HttpClientHelper.class);
    private final RestTemplate restTemplate;

    public HttpClientHelper() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Executes a GET request with optional query params and headers.
     * Returns only if status is 2xx and body is non-empty; otherwise throws.
     *
     * @param url base URL (e.g., https://api.example.com/v1/resource)
     * @param queryParams query parameters (page, size, etc.)
     * @param headers HTTP headers (x-api-key, Authorization, etc.) — optional
     * @return ResponseEntity<String> with a non-null, non-empty body
     * @throws IllegalArgumentException if url is null/blank
     * @throws IllegalStateException if transport fails or response is non-2xx/empty
     */
    public ResponseEntity<String> get(String url,
                                      Map<String, Object> queryParams,
                                      Map<String, String> headers) {

        if (url == null || url.isBlank()) {                 // URL must be provided by caller
            throw new IllegalArgumentException("URL must not be null/blank");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        URI uri = builder.build(true).toUri();

        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }
        HttpEntity<Void> request = new HttpEntity<>(httpHeaders);

        try {
            ResponseEntity<String> resp =
                    restTemplate.exchange(uri, HttpMethod.GET, request, String.class);

            if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
                int code = (resp == null ? -1 : resp.getStatusCodeValue());
                log.error("Non-2xx response: uri={} status={}", uri, code);
                throw new IllegalStateException("Non-2xx response: status=" + code);
            }
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
