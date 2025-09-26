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

    Long id;              // our internal id for the review
    String source;        // where it came from (Google, Yelp, etc.)
    String externalId;    // id from that external system if they provide one
    String author;        // who wrote it
    Integer rating;       // star rating, 1–5 or whatever scale we're using
    String content;       // the actual text the person wrote
    LocalDateTime reviewDate; // when the review was left
    String tag;           // extra label we can attach (like "positive" or "complaint")
}
