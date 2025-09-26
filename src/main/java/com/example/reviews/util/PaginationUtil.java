package com.example.reviews.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtil {
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 200;

    private PaginationUtil() {}

    /** Client passes 1-based page;  clamped size and handle optional sort. */
    public static Pageable createPageable(int page, int size, Sort sort) {
        int zeroBased = Math.max(0, page - 1);
        int clampedSize = Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
        return PageRequest.of(
                zeroBased,
                clampedSize,
                (sort == null) ? Sort.unsorted() : sort
        );
    }

    /** Overload without sort. */
    public static Pageable createPageable(int page, int size) {
        return createPageable(page, size, Sort.unsorted());
    }
}
