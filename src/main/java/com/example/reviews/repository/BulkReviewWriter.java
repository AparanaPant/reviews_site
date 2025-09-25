package com.example.reviews.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BulkReviewWriter {

    private final JdbcTemplate jdbc;

    public BulkReviewWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    /**
     * Upserts rows in batches using MySQL ON DUPLICATE KEY UPDATE.
     * Requires a unique index on (source, external_id).
     */
    @Transactional
    public int upsertBatch(List<Row> rows) {
        if (rows == null || rows.isEmpty()) return 0;

        String sql = "INSERT INTO reviews " +
                "(source, external_id, author, rating, content, review_date, tag, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "author=VALUES(author), " +
                "rating=VALUES(rating), " +
                "content=VALUES(content), " +
                "review_date=VALUES(review_date), " +
                "tag=VALUES(tag), " +
                "updated_at=VALUES(updated_at)";

        LocalDateTime now = LocalDateTime.now();
        Timestamp ts = Timestamp.valueOf(now);

        int[] counts = jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                Row r = rows.get(i);
                ps.setString(1, r.source);
                ps.setString(2, r.externalId);
                ps.setString(3, r.author);
                if (r.rating == null) ps.setObject(4, null); else ps.setInt(4, r.rating);
                ps.setString(5, r.content);
                if (r.reviewDate == null) ps.setObject(6, null);
                else ps.setTimestamp(6, Timestamp.valueOf(r.reviewDate));
                ps.setString(7, r.tag);
                ps.setTimestamp(8, ts); // created_at
                ps.setTimestamp(9, ts); // updated_at
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        int total = 0;
        for (int c : counts) total += c;
        return total;
    }

    /** Batch row DTO */
    public static class Row {
        public String source;
        public String externalId;
        public String author;
        public Integer rating;
        public String content;
        public LocalDateTime reviewDate;
        public String tag;
    }
}
