package com.example.reviews.repository;

import com.example.reviews.model.upstream.ReviewInDto;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Performs bulk UPSERTs into the "reviews" table using MySQL
 * {@code INSERT ... ON DUPLICATE KEY UPDATE}.
 *
 * Requirements:
 * - A UNIQUE constraint on (source, external_id).
 * - For best batching performance on MySQL: add
 *   {@code rewriteBatchedStatements=true} to the JDBC URL.
 *
 * Notes on counts:
 * - In batch mode, JDBC may return {@link Statement#SUCCESS_NO_INFO} (-2) per item,
 *   which means "it worked but exact row count is unknown".
 *   We normalize those to 1 so logs remain meaningful.
 */
@Repository
public class BulkReviewWriter {

    private final JdbcTemplate jdbc;

    public BulkReviewWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Upserts the provided rows in a single JDBC batch.
     * Each page from the importer should call this once (transactional boundary).
     *
     * @param rows items to write; ignored if null/empty
     * @return a best-effort count for logging (normalized; see class docs)
     */
    @Transactional
    public int upsertBatch(List<ReviewInDto> rows) {
        if (rows == null || rows.isEmpty()) return 0;

        final String sql =
                "INSERT INTO reviews " +
                        "(source, external_id, author, rating, content, review_date, tag, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "author=VALUES(author), " +
                        "rating=VALUES(rating), " +
                        "content=VALUES(content), " +
                        "review_date=VALUES(review_date), " +
                        "tag=VALUES(tag), " +
                        "updated_at=VALUES(updated_at)";

        // Single timestamp reused across all rows in this batch for consistency
        final Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());

        int[] counts = jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ReviewInDto r = rows.get(i);

                ps.setString(1, r.source());
                ps.setString(2, r.id());                 // external_id
                ps.setString(3, r.author());

                if (r.rating() == null) ps.setNull(4, Types.INTEGER);
                else ps.setInt(4, r.rating());

                ps.setString(5, r.content());

                if (r.reviewDate() == null) ps.setNull(6, Types.TIMESTAMP);
                else ps.setTimestamp(6, Timestamp.valueOf(r.reviewDate()));

                ps.setString(7, r.tags());
                ps.setTimestamp(8, nowTs);               // created_at
                ps.setTimestamp(9, nowTs);               // updated_at
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        // Normalize JDBC batch result codes for logging
        int normalized = getNormalized(counts);
        return normalized;
    }

    private static int getNormalized(int[] counts) {
        int normalized = 0;
        for (int c : counts) {
            if (c == Statement.SUCCESS_NO_INFO) {
                normalized += 1;           // executed, exact count unknown
            } else if (c == Statement.EXECUTE_FAILED) {
                // Could log details here; we skip adding to the total
                // log.warn("One batch item failed during upsert");
            } else if (c >= 0) {
                normalized += c;  // exact affected rows (1 on insert, 2 on upsert update)
            }
        }
        return normalized;
    }
}
