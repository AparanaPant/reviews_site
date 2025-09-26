package com.example.reviews.model.upstream;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Single review record coming from the upstream API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewInDto(
        @NotBlank String id,             // must have an id
        @NotBlank String source,         // must have a source (used with id for uniqueness)
        String author,                   // reviewer’s name (optional)
        @Min(1) @Max(5) Integer rating,  // rating must be between 1–5 if present
        String content,                  // review text
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][.SS][.S]['Z'][XXX][X]")
        LocalDateTime reviewDate,        // date/time the review was created
        String tags                      // optional tag/category
) {}
