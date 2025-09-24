package com.example.reviews.repository;

import com.example.reviews.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface ReviewRepository
        extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Optional<Review> findBySourceAndExternalId(String source, String externalId);

//    // Override with EntityGraph to load tags
//    Optional<Review> findById(Long id); // default, no tags
//    @EntityGraph(attributePaths = "tags")
//    Optional<Review> findByIdWithTags(Long id);
//
//
//    // Override with EntityGraph to load tags safely with pagination
//    Page<Review> findAll(Specification<Review> spec, Pageable pageable); // default
//    @EntityGraph(attributePaths = "tags")
//    Page<Review> findAllWithTags(Specification<Review> spec, Pageable pageable);
}
