package com.example.reviews.model.upstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Full response wrapper from upstream:
 * contains a list of reviews + paging info.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewsEnvelopeDto(List<ReviewInDto> reviews, PagingDto paging) {}
