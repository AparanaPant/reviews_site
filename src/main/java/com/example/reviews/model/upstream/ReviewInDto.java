package com.example.reviews.model.upstream;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewInDto(
        @NotBlank String id,        // required: upstream must provide an id
        @NotBlank String source,    // required: needed for UNIQUE(source, external_id)
        String author,
        @Min(1) @Max(5) Integer rating, // optional; if present must be in [1..5]
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][.SS][.S]['Z'][XXX][X]")
        LocalDateTime reviewDate,
        String tags
) {}
