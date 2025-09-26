package com.example.reviews.model.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * This class represents how a single review is stored inside our database.
 *
 * Key design notes:
 * - I used a numeric `Long` ID as the primary key instead of UUID:
 *   • BIGINT is only 8 bytes compared to UUID’s 16 bytes → smaller index, faster joins and lookups.
 *   • Sequential IDs are more storage-friendly for MySQL’s B-Trees than random UUIDs.
 *   • We don’t need cross-data-center uniqueness here — a single DB instance is enough.
 *   • If in the future we want opaque API IDs, we can always expose a UUID or hash externally
 *     while still keeping the database efficient with BIGINT.
 *
 * - Uniqueness is guaranteed by `(source, external_id)`.
 *   • The external review site already gives each review an identifier.
 *   • By combining it with the source (Google, Yelp, etc.), we can safely upsert —
 *     meaning if a review is updated upstream, we just overwrite the old record here.
 *
 * - Timestamps are handled by the importer, not by the DB.
 *   • `created_at` and `updated_at` are set consistently in batch, so a whole page of reviews
 *     gets the same “imported at” time.
 *   • `review_date` is the time when the user actually wrote the review on the source site.
 *
 * - Text fields:
 *   • `content` uses `TEXT` so we don’t need to guess the max review size.
 *   • `tag`, `author`, etc. are capped with reasonable lengths to protect storage and indexing.
 */
@Setter
@Getter
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name="uk_source_external", columnNames = {"source", "external_id"})
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // internal PK: efficient BIGINT auto-increment

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId; // ID from upstream provider

    @Column(name = "source", nullable = false, length = 32)
    private String source; // which site the review came from (part of UNIQUE key)

    @Column(name = "author", length = 255)
    private String author; // optional reviewer name

    @Column(name = "rating")
    private Integer rating; // optional star rating, validated between 1–5

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // main review body

    @Column(name = "tag", length = 64)
    private String tag; // optional categorization/tag

    @Column(name = "review_date")
    private LocalDateTime reviewDate; // when the review was written on the source site

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // importer’s timestamp for record creation

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // importer’s timestamp for last update

    public Review() {}
}
