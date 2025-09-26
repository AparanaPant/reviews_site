package com.example.reviews.mapper;

import com.example.reviews.model.dto.ReviewDto;
import com.example.reviews.model.entity.Review;
import org.mapstruct.Mapper;

/**
 * Maps Review entities to ReviewDto objects for API responses.
 * Uses MapStruct to generate the implementation at build time.
 */

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    ReviewDto toDto(Review review);
}
