package com.example.reviews.repository.spec;

import com.example.reviews.model.entity.Review;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * JPA Specifications for filtering Review entities.
 * Keeps filter logic reusable and composable.
 */
public final class ReviewSpecifications {

    private ReviewSpecifications() {} // utility class

    /** Case-insensitive filter by source */
    public static Specification<Review> source(String source) {
        if (!StringUtils.hasText(source)) return null;
        String s = source.trim().toLowerCase();
        return (root, cq, cb) -> cb.equal(cb.lower(root.get("source")), s);
    }

    /** Case-insensitive filter by tag */
    public static Specification<Review> tag(String tag) {
        if (!StringUtils.hasText(tag)) return null;
        String t = tag.trim().toLowerCase();
        return (root, cq, cb) -> cb.equal(cb.lower(root.get("tag")), t);
    }
}
