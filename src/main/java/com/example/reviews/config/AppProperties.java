package com.example.reviews.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Holds configuration for the external Reviews API.
 * Values are bound from application.yml/properties using prefix "reviews.api".
 * Example: reviews.api.url, reviews.api.key, reviews.api.page-size
 */

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "reviews.api")
public class AppProperties {
    private String url;
    private String key;
    private Integer itemsPerPage = 50;
}
