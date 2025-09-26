package com.example.reviews.repository;
import com.example.reviews.model.entity.Review;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface ReviewRepository
        extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Optional<Review> findBySourceAndExternalId(String source, String externalId);
}
//abstract class pass