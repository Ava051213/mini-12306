CREATE TABLE order_idempotency (
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    train_trip_id BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Scope each key to its owning account.
    PRIMARY KEY (user_id, idempotency_key),

    -- Associate each successful order with at most one key.
    UNIQUE KEY uk_order_idempotency_order (order_id),

    CONSTRAINT fk_order_idempotency_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_order_idempotency_order FOREIGN KEY (order_id) REFERENCES ticket_orders (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;