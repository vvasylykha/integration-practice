CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    exchange_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_audit_log_exchange_event ON audit_log(exchange_id, event_type);
CREATE INDEX idx_audit_exchange_id ON audit_log(exchange_id);
CREATE INDEX idx_audit_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_event_type ON audit_log(event_type);

