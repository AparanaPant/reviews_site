package com.example.reviews.repository;

import com.example.reviews.model.entity.Review;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import java.time.LocalDateTime;

public class ReviewSpecifications {
    public static Specification<Review> q(String q) {
        if (q == null || q.isBlank()) return null;
        String like = "%" + q.toLowerCase() + "%";
        return (root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("author")), like),
                cb.like(cb.lower(root.get("content")), like)
        );
    }
    public static Specification<Review> source(String source) {
        if (source == null || source.isBlank()) return null;
        return (root, cq, cb) -> cb.equal(root.get("source"), source);
    }
    public static Specification<Review> minRating(Integer min) {
        if (min == null) return null;
        return (root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), min);
    }
    public static Specification<Review> maxRating(Integer max) {
        if (max == null) return null;
        return (root, cq, cb) -> cb.lessThanOrEqualTo(root.get("rating"), max);
    }
    public static Specification<Review> from(LocalDateTime from) {
        if (from == null) return null;
        return (root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("reviewDate"), from);
    }
    public static Specification<Review> to(LocalDateTime to) {
        if (to == null) return null;
        return (root, cq, cb) -> cb.lessThanOrEqualTo(root.get("reviewDate"), to);
    }
    public static Specification<Review> tag(String tag) {
        if (tag == null || tag.isBlank()) return null;
        return (root, cq, cb) -> {
            Join<Object, Object> join = root.join("tags", JoinType.LEFT);
            return cb.equal(join.get("tag"), tag);
        };
    }
}
