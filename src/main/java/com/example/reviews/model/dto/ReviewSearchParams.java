package com.example.reviews.model.dto;

import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.LocalDateTime;

public class ReviewSearchParams {
    public String q;
    public String source;
    @Min(1) @Max(5)
    public Integer minRating;
    @Min(1) @Max(5)
    public Integer maxRating;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTime fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTime toDate;
    public String tag;
}
