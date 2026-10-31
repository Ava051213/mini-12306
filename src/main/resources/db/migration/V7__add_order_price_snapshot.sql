-- Distinguish legacy orders from orders priced by the server.
ALTER TABLE ticket_orders
    ADD COLUMN purchase_mode VARCHAR(16) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN departure_sequence INT NULL,
    ADD COLUMN arrival_sequence INT NULL,
    ADD COLUMN departure_station_code VARCHAR(20) NULL,
    ADD COLUMN departure_station_name VARCHAR(50) NULL,
    ADD COLUMN departure_at DATETIME NULL,
    ADD COLUMN arrival_station_code VARCHAR(20) NULL,
    ADD COLUMN arrival_station_name VARCHAR(50) NULL,
    ADD COLUMN arrival_at DATETIME NULL,
    ADD COLUMN seat_type INT NULL,
    ADD COLUMN amount_cents INT NULL;

-- Historical rows keep an empty snapshot; priced rows must be complete.
ALTER TABLE ticket_orders
    ADD CONSTRAINT chk_ticket_orders_purchase_mode
        CHECK ( purchase_mode IN ('LEGACY', 'PRICED') ),
    ADD CONSTRAINT chk_ticket_orders_legacy_snapshot
        CHECK (
            purchase_mode <> 'LEGACY'
                OR (
                departure_sequence IS NULL
                    AND arrival_sequence IS NULL
                    AND departure_station_code IS NULL
                    AND departure_station_name IS NULL
                    AND departure_at IS NULL
                    AND arrival_station_code IS NULL
                    AND arrival_station_name IS NULL
                    AND arrival_at IS NULL
                    AND seat_type IS NULL
                    AND amount_cents IS NULL
                )
            ),
    ADD CONSTRAINT chk_ticket_orders_priced_snapshot
        CHECK (
            purchase_mode <> 'PRICED'
                OR (
                departure_sequence IS NOT NULL
                    AND arrival_sequence IS NOT NULL
                    AND departure_station_code IS NOT NULL
                    AND departure_station_name IS NOT NULL
                    AND departure_at IS NOT NULL
                    AND arrival_station_code IS NOT NULL
                    AND arrival_station_name IS NOT NULL
                    AND arrival_at IS NOT NULL
                    AND seat_type IS NOT NULL
                    AND amount_cents IS NOT NULL
                )
            ),
    ADD CONSTRAINT chk_ticket_orders_seat_type
        CHECK ( seat_type IS NULL OR seat_type IN (0, 1, 2) ),
    ADD CONSTRAINT chk_ticket_orders_amount
        CHECK ( amount_cents IS NULL OR amount_cents > 0 ),
    ADD CONSTRAINT chk_ticket_orders_sequence_order
        CHECK (
            departure_sequence IS NULL
                OR (departure_sequence > 0 AND arrival_sequence > departure_sequence)
            );

-- The idempotency record must remember which purchase mode created it.
ALTER TABLE order_idempotency
    ADD COLUMN purchase_mode VARCHAR(16) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN seat_type INT NULL;

ALTER TABLE order_idempotency
    ADD CONSTRAINT chk_order_idempotency_purchase_mode
        CHECK ( purchase_mode IN ('LEGACY', 'PRICED') ),
    ADD CONSTRAINT chk_order_idempotency_seat_type
        CHECK ( seat_type IS NULL OR seat_type IN (0, 1, 2) ),
    ADD CONSTRAINT chk_order_idempotency_legacy_seat
        CHECK ( purchase_mode <> 'LEGACY' OR seat_type IS NULL ),
    ADD CONSTRAINT chk_order_idempotency_priced_seat
        CHECK ( purchase_mode <> 'PRICED' OR seat_type IS NOT NULL );
