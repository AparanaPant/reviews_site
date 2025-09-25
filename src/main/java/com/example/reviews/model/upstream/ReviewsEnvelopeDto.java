package com.example.reviews.model.upstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewsEnvelopeDto(List<ReviewInDto> reviews, PagingDto paging) {}
