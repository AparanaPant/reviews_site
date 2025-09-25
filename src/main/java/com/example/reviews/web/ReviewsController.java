package com.example.reviews.web;

import com.example.reviews.model.dto.PaginationDto;
import com.example.reviews.model.dto.ReviewDto;
import com.example.reviews.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
public class ReviewsController {

    private final ReviewService reviewService;

    public ReviewsController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }
    @GetMapping
    public PaginationDto<ReviewDto> list(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,   // client sends 1-based
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReviewDto> p = reviewService.search(source, tag, page, size);

        return new PaginationDto<>(
                page,                       // 1-based page (what client expects)
                size,                       // requested requestedSize
                p.getTotalPages(),          // total number of pages
                p.getTotalElements(),       // total matching records
                p.getNumberOfElements(),    // number of elements in this page
                p.getContent()              // actual items
        );
    }

    @GetMapping("/{id}")
    public ReviewDto get(@PathVariable Long id) {
        return reviewService.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        reviewService.delete(id);
    }
}
