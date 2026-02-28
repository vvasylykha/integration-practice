CREATE TABLE exchanges (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    converted_amount DECIMAL(19,4) NOT NULL,
    exchange_rate DECIMAL(19,6) NOT NULL,
    commission DECIMAL(19,4),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_id ON exchanges(user_id);
CREATE INDEX idx_created_at ON exchanges(created_at);
CREATE INDEX idx_status ON exchanges(status);
