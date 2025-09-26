package com.example.reviews.repository;

import com.example.reviews.model.upstream.ReviewInDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * BulkReviewWriter
 *
 * What’s going on here?
 * ---------------------
 * Every time the importer pulls in a “page” of reviews, we need to write them
 * to the database — fast. Some of those reviews are brand new, some may already
 * exist but have been updated upstream. Instead of guessing which is which,
 * we just “upsert”: insert new rows, update existing ones.
 *
 * How we do it:
 * -------------
 * - Use a single JDBC batch with MySQL’s
 *   {@code INSERT ... ON DUPLICATE KEY UPDATE}.
 * - This means one roundtrip per batch instead of one per row.
 * - We reuse a single timestamp for `created_at` and `updated_at`
 *   so everything in a batch has consistent timing.
 *
 * Why raw SQL (and not JPA)?
 * --------------------------
 * At first glance, this looks a little “too raw.” Wouldn’t it be nicer
 * to just call `saveAll()` on a Spring Data repository? Sure, but:
 * - JPA/Hibernate can’t do vendor-neutral upserts. Every DB has its own syntax
 *   (`ON DUPLICATE KEY`, `ON CONFLICT`, `MERGE`…), and Spring doesn’t abstract that.
 * - `saveAll()` checks every entity to see if it’s new or existing, which means
 *   lots of unnecessary queries when you’re pushing thousands of rows.
 * - For imports like this, performance matters more than abstraction.
 *
 * And we’re not alone here — this Medium article says the same:
 * <a href="https://medium.com/analytics-vidhya/bulk-rdbms-upserts-with-spring-506edc9cea19">
 * Bulk RDBMS Upserts with Spring</a>.
 * Quote:
 *   “Native SQL queries will give the most performant result,
 *    use solutions as close to the database as possible where performance is critical.”
 *
 * Looking ahead:
 * --------------
 * This approach is simple and scales well for now. If volumes explode, we could
 * think about staging tables, partitioned imports, or bulk loaders like
 * MySQL’s `LOAD DATA`. But for day-to-day syncing of reviews, this pattern
 * hits the sweet spot: clear, reliable, and fast.
 */

@Repository
public class BulkReviewWriter {

    private static final Logger log = LoggerFactory.getLogger(BulkReviewWriter.class);

    private final JdbcTemplate jdbc;

    public BulkReviewWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Upserts the provided rows in a single JDBC batch.
     * Transaction boundary: one transaction per page.
     *
     * @param rows items to write; ignored if null/empty
     * @return best-effort affected count for logs (normalized)
     */
    @Transactional
    public int upsertBatch(List<ReviewInDto> rows) {
        if (rows == null || rows.isEmpty()) return 0;

        // MySQL UPSERT. If a row with the same (source, external_id) exists, selected columns are updated.
        final String sql =
                "INSERT INTO reviews " +
                        "  (source, external_id, author, rating, content, review_date, tag, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  author=VALUES(author), " +
                        "  rating=VALUES(rating), " +
                        "  content=VALUES(content), " +
                        "  review_date=VALUES(review_date), " +
                        "  tag=VALUES(tag), " +
                        "  updated_at=VALUES(updated_at)";

        // One timestamp reused across all rows in this batch for consistency
        final Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());

        int[] counts = jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ReviewInDto r = rows.get(i);

                // NOTE: all values are bound via PreparedStatement placeholders (safe from SQL injection)
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

        // Turn driver-specific batch result codes into a friendly total for logs/metrics
        int normalized = getNormalized(counts);
        return normalized;
    }

    /**
     * Normalize JDBC batch results into a human-friendly "affected rows" count.
     *
     * Rules:
     * - SUCCESS_NO_INFO (-2): treat as 1 (we know it executed, just not how many).
     * - EXECUTE_FAILED (-3): ignore in totals but keep an eye on logs; the transaction will still reflect reality.
     * - >= 0: trust the exact count (usually 1 on insert, 2 on upsert update in MySQL).
     *
     * This number is for observability only. The database remains the source of truth.
     */
    private static int getNormalized(int[] counts) {
        int normalized = 0;
        int failed = 0;

        for (int c : counts) {
            if (c == Statement.SUCCESS_NO_INFO) {
                normalized += 1;           // executed, exact count unknown → count as 1 for logging purposes
            } else if (c == Statement.EXECUTE_FAILED) {
                failed++;
                // We don’t add to total; the batch/transaction outcome reflects the real state.
            } else if (c >= 0) {
                normalized += c;  // exact affected rows (often 1 on insert, 2 on update due to ON DUPLICATE KEY)
            }
        }

        if (failed > 0) {
            log.warn("Batch contained {} failed item(s). Consider enabling per-row retry or staging-table checks if this is frequent.", failed);
        }

        return normalized;
    }
}
