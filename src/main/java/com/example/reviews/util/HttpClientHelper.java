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
 * Generic helper for making HTTP GET calls with query parameters and headers.
 * Reusable by any service in this application.
 */
@Component
public class HttpClientHelper {

    private static final Logger log = LoggerFactory.getLogger(HttpClientHelper.class);

    private final RestTemplate restTemplate;

    public HttpClientHelper() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Executes a GET request with optional query params and headers.
     *
     * @param url base URL (e.g., https://api.example.com/v1/resource)
     * @param queryParams query parameters (page, size, etc.)
     * @param headers HTTP headers (x-api-key, Authorization, etc.)
     * @return ResponseEntity<String> containing the response body or error status
     */
    public ResponseEntity<String> get(String url,
                                      Map<String, Object> queryParams,
                                      Map<String, String> headers) {
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
            return restTemplate.exchange(uri, HttpMethod.GET, request, String.class);
        } catch (RestClientException ex) {
            log.error("HTTP GET failed: uri={} queryParams={}", uri, queryParams, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
