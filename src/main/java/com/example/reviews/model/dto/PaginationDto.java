package com.example.reviews.model.dto;

import java.util.List;

public record PaginationDto<T>(
        int page,
        int size,
        int totalPages,
        List<T> items
) {}
