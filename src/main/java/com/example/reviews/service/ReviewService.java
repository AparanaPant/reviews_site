package com.example.reviews.service;

import com.example.reviews.model.dto.ReviewDto;
import com.example.reviews.model.dto.ReviewSearchParams;
import com.example.reviews.model.entity.Review;
import com.example.reviews.repository.ReviewRepository;
import com.example.reviews.repository.ReviewSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;
    @Transactional(readOnly = true)
    public Page<ReviewDto> search(ReviewSearchParams p, Pageable pageable) {
        Specification<Review> spec = Specification.where(ReviewSpecifications.q(p.q))
                .and(ReviewSpecifications.source(p.source))
                .and(ReviewSpecifications.minRating(p.minRating))
                .and(ReviewSpecifications.maxRating(p.maxRating))
                .and(ReviewSpecifications.from(p.fromDate))
                .and(ReviewSpecifications.to(p.toDate))
                .and(ReviewSpecifications.tag(p.tag));
        return reviewRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ReviewDto get(Long id) {
        Review r = reviewRepository.findById(id).orElseThrow(() -> new NotFoundException("Review " + id + " not found"));
        return toDto(r);
    }

    @Transactional
    public void delete(Long id) {
        if (reviewRepository.existsById(id)) {
            reviewRepository.deleteById(id);
        }
    }
    @Transactional
    public Review upsert(String source, String externalId,
                         String author, Integer rating, String content, String tag,
                         java.time.LocalDateTime reviewDate) {
        Review r = reviewRepository.findBySourceAndExternalId(source, externalId).orElseGet(Review::new);
        r.setSource(source);
        r.setExternalId(externalId);
        r.setAuthor(author);
        r.setRating(rating);
        r.setTag(tag);
        r.setContent(content);
        r.setReviewDate(reviewDate);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (r.getId() == null) r.setCreatedAt(now);
        r.setUpdatedAt(now);

        return reviewRepository.save(r);
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
