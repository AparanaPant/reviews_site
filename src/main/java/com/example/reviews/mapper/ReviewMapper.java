package com.example.reviews.mapper;

import com.example.reviews.model.dto.ReviewDto;
import com.example.reviews.model.entity.Review;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    ReviewDto toDto(Review review);
}
