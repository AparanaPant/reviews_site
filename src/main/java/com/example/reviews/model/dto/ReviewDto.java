package com.example.reviews.model.dto;
import lombok.Value;
import java.time.LocalDateTime;

@Value
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
