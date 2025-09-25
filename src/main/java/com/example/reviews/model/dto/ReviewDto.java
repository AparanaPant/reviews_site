package com.example.reviews.model.dto;
import java.time.LocalDateTime;


public class ReviewDto {
    public Long id;
    public String source;
    public String externalId;
    public String author;
    public Integer rating;
    public String content;
    public LocalDateTime reviewDate;
    public String tag;
}
