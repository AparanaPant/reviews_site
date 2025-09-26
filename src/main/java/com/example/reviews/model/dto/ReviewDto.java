package com.example.reviews.model.dto;

import lombok.Value;
import lombok.Builder;
import java.time.LocalDateTime;

// Simple data holder for reviews.
// Marked with @Value so it's immutable (once created, it can't change).
// @Builder lets us create it with a nice fluent API instead of a huge constructor.
@Value
@Builder
public class ReviewDto {
    Long id;
    String source;
    String externalId;
    String author;
    Integer rating;
    String content;
    LocalDateTime reviewDate;
    String tag;
}
