package com.example.reviews.service;

import com.example.reviews.model.dto.ReviewDto;
import com.example.reviews.model.entity.Review;
import com.example.reviews.repository.ReviewRepository;
import com.example.reviews.repository.ReviewSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.reviews.util.PaginationUtils.createPageable;
import com.example.reviews.mapper.ReviewMapper;
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    public ReviewService(ReviewRepository reviewRepository,ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.reviewMapper = reviewMapper;
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> search(String source, String tag, int page, int size) {
        Specification<Review> spec = getSpec(source, tag);
        Pageable pageable = createPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepository.findAll(spec, pageable)
                .map(reviewMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ReviewDto get(Long id) {
        Review r = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review " + id + " not found"));
        return reviewMapper.toDto(r); // <--- call here
    }

    @Transactional
    public void delete(Long id) {
        if (reviewRepository.existsById(id)) {
            reviewRepository.deleteById(id);
        }
    }

    private static Specification<Review> getSpec(String source, String tag) {
        return Specification
                .where(ReviewSpecifications.source(source))
                .and(ReviewSpecifications.tag(tag));
    }
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String m) { super(m); }
    }
}
