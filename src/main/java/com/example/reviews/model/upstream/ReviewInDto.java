package com.example.reviews.model.upstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewInDto(
        String id,
        String source,
        String author,
        Integer rating,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][.SS][.S]['Z'][XXX][X]")
        LocalDateTime reviewDate,
        String tags
) {}
