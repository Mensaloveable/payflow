-- ─────────────────────────────────────────────────────────────
-- V1__create_transactions_table.sql
-- Initial schema: payment transactions
-- ─────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS transactions (
    id                 BIGSERIAL PRIMARY KEY,
    transaction_id     VARCHAR(64)    NOT NULL UNIQUE,
    payment_method     VARCHAR(32)    NOT NULL,
    status             VARCHAR(16)    NOT NULL,
    success            BOOLEAN        NOT NULL DEFAULT FALSE,

    -- Financial details
    amount             NUMERIC(19, 4) NOT NULL,
    currency           VARCHAR(8)     NOT NULL DEFAULT 'USD',
    recipient          VARCHAR(255)   NOT NULL,
    description        VARCHAR(512),

    -- Processor output
    message            VARCHAR(512),
    processor_details  TEXT,

    -- Timestamps
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for common query patterns
CREATE INDEX idx_transactions_payment_method ON transactions (payment_method);
CREATE INDEX idx_transactions_status         ON transactions (status);
CREATE INDEX idx_transactions_created_at     ON transactions (created_at DESC);
CREATE INDEX idx_transactions_recipient      ON transactions (recipient);

-- Trigger: keep updated_at fresh on every row update
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE  transactions                  IS 'Immutable ledger of all payment attempts';
COMMENT ON COLUMN transactions.transaction_id   IS 'Application-generated TXN-XXXXXXXXXXXX identifier';
COMMENT ON COLUMN transactions.processor_details IS 'Strategy-specific result string (masked card, wallet hash, etc.)';
