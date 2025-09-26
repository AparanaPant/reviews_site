package com.example.reviews.api;

import com.example.reviews.model.dto.PaginationDto;
import com.example.reviews.model.dto.ReviewDto;
import com.example.reviews.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for accessing and managing reviews.
 *
 * Exposes three endpoints:
 * - GET /reviews        → search reviews with optional filters + pagination
 * - GET /reviews/{id}   → fetch a single review by its ID
 * - DELETE /reviews/{id} → remove a review by its ID
 *
 * This controller is thin: all logic lives in ReviewService.
 */
@RestController
@RequestMapping("/reviews")
public class ReviewsController {

    private final ReviewService reviewService;

    public ReviewsController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * List/search reviews with optional filters.
     * Only "source" and "tag" are supported filters in this version.
     * Clients send 1-based page numbers; we keep that consistent here.
     */
    @GetMapping
    public PaginationDto<ReviewDto> list(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReviewDto> p = reviewService.search(source, tag, page, size);

        return new PaginationDto<>(
                page,                       // 1-based page (matches client expectations)
                p.getTotalPages(),          // how many total pages exist
                p.getTotalElements(),       // total matching reviews
                p.getNumberOfElements(),    // how many reviews in this page
                p.getContent()              // actual review DTOs
        );
    }

    /**
     * Fetch one review by ID.
     * Throws 404 if not found.
     */
    @GetMapping("/{id}")
    public ReviewDto get(@PathVariable Long id) {
        return reviewService.get(id);
    }

    /**
     * Delete a review by ID.
     * Returns 204 No Content if successful.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        reviewService.delete(id);
    }
}
