package com.example.reviews.model.upstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PagingDto(Integer page, Integer size, Integer totalPages) {}
