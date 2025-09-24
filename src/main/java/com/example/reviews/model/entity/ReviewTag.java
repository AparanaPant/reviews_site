package com.example.reviews.model.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "review_tags")
@IdClass(ReviewTag.PK.class)
public class ReviewTag {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, foreignKey = @ForeignKey(name="fk_reviewtag_review"))
    private Review review;

    @Id
    @Column(name = "tag", length = 64, nullable = false)
    private String tag;

    public ReviewTag() {}
    public ReviewTag(Review review, String tag) {
        this.review = review;
        this.tag = tag;
    }
    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public static class PK implements Serializable {
        private Long review;
        private String tag;
        public PK() {}
        public PK(Long review, String tag) { this.review = review; this.tag = tag; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PK pk = (PK) o;
            return Objects.equals(review, pk.review) && Objects.equals(tag, pk.tag);
        }
        @Override public int hashCode() { return Objects.hash(review, tag); }
    }
}
