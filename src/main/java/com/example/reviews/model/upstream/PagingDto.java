package com.example.reviews.model.upstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Paging info returned by the upstream API.
 * (page number, page size, total pages)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagingDto(Integer page, Integer size, Integer totalPages) {}
