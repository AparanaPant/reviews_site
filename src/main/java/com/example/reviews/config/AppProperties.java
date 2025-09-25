package com.example.reviews.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "reviews.api")
public class AppProperties {
    private String url;
    private String key;
    private Integer pageSize = 50;
}
