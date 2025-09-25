package com.example.reviews.model.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;


@Setter
@Getter
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name="uk_source_external", columnNames = {"source", "external_id"})
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "author", length = 255)
    private String author;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "tag", length = 64)
    private String tag;

    @Column(name = "review_date")
    private LocalDateTime reviewDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Review() {}

}
