package com.example.reviews.web;

import com.example.reviews.model.dto.ReviewDto;
import com.example.reviews.model.dto.ReviewSearchParams;
import com.example.reviews.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/reviews")
@Validated
public class ReviewsController {

    private final ReviewService reviewService;

    public ReviewsController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public Page<ReviewDto> search(@Valid ReviewSearchParams params,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(defaultValue = "reviewDate,desc") String sort) {
        String[] s = sort.split(",");
        Sort.Direction dir = s.length > 1 && "asc".equalsIgnoreCase(s[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0,page), Math.min(200, Math.max(1, size)), Sort.by(dir, s[0]));
        return reviewService.search(params, pageable);
    }

    @GetMapping("/{id}")
    public ReviewDto get(@PathVariable Long id) {
        return reviewService.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        reviewService.delete(id);
    }
}
