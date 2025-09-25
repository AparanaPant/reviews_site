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

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> search(String source, String tag, int page, int size) {
        Specification<Review> spec = getSpec(source, tag);
        Pageable pageable = createPageable(page, size,Sort.by(Sort.Direction.DESC, "createdAt")   // or null for unsorted
        );

        return reviewRepository.findAll(spec, pageable).map(this::toDto);
    }

    private static Specification<Review> getSpec(String source, String tag) {
        return Specification
                .where(ReviewSpecifications.source(source))  // ignored if null
                .and(ReviewSpecifications.tag(tag));         // ignored if null
    }

    @Transactional(readOnly = true)
    public ReviewDto get(Long id) {
        Review r = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review " + id + " not found"));
        return toDto(r);
    }

    @Transactional
    public void delete(Long id) {
        if (reviewRepository.existsById(id)) {
            reviewRepository.deleteById(id);
        }
    }

    private ReviewDto toDto(Review r) {
        ReviewDto d = new ReviewDto();
        d.id = r.getId();
        d.source = r.getSource();
        d.externalId = r.getExternalId();
        d.author = r.getAuthor();
        d.rating = r.getRating();
        d.content = r.getContent();
        d.reviewDate = r.getReviewDate();
        d.tag = r.getTag();
        return d;
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String m) { super(m); }
    }
}
