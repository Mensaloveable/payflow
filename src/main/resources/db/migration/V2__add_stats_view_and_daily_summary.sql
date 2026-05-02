-- ─────────────────────────────────────────────────────────────
-- V2__add_stats_view_and_daily_summary.sql
-- Analytics: materialized view + daily rollup table
-- ─────────────────────────────────────────────────────────────

-- Summary stats per payment method
CREATE OR REPLACE VIEW payment_method_stats AS
SELECT
    payment_method,
    COUNT(*)                                          AS total_count,
    COUNT(*) FILTER (WHERE success = TRUE)            AS success_count,
    COUNT(*) FILTER (WHERE success = FALSE)           AS failure_count,
    ROUND(
        COUNT(*) FILTER (WHERE success = TRUE)::NUMERIC
        / NULLIF(COUNT(*), 0) * 100, 2
    )                                                 AS success_rate_pct,
    SUM(amount) FILTER (WHERE success = TRUE)         AS total_volume,
    AVG(amount) FILTER (WHERE success = TRUE)         AS avg_amount,
    MAX(amount)                                       AS max_amount,
    MIN(created_at)                                   AS first_transaction,
    MAX(created_at)                                   AS last_transaction
FROM transactions
GROUP BY payment_method;

COMMENT ON VIEW payment_method_stats IS 'Live aggregated stats per payment method — cached in Redis by application layer';

-- Daily volume rollup (populated by app or cron)
CREATE TABLE IF NOT EXISTS daily_transaction_summary (
    id             BIGSERIAL PRIMARY KEY,
    summary_date   DATE           NOT NULL,
    payment_method VARCHAR(32)    NOT NULL,
    total_count    INT            NOT NULL DEFAULT 0,
    success_count  INT            NOT NULL DEFAULT 0,
    failure_count  INT            NOT NULL DEFAULT 0,
    total_volume   NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    UNIQUE (summary_date, payment_method)
);

CREATE INDEX idx_daily_summary_date   ON daily_transaction_summary (summary_date DESC);
CREATE INDEX idx_daily_summary_method ON daily_transaction_summary (payment_method);

COMMENT ON TABLE daily_transaction_summary IS 'Pre-aggregated daily rollups for fast dashboard queries';
