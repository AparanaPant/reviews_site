package com.example.reviews.model.dto;

import java.util.List;

public record PaginationDto<T>(
        int page,              // current page index (1-based)
        int totalPages,        // total number of pages
        long totalElements,    // total number of matching records
        int pageSize,  // number of elements in the current page
        List<T> items          // actual items
) {}

