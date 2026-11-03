-- A run sells either quantity stock or concrete seats, never both.
ALTER TABLE train_trips
    ADD COLUMN sales_mode VARCHAR(16) NOT NULL DEFAULT 'QUANTITY';

ALTER TABLE train_trips
    ADD CONSTRAINT chk_train_trips_sales_mode
        CHECK ( sales_mode IN ('QUANTITY', 'SEAT') );

-- Keep the assigned seat on the order that bought it.
ALTER TABLE ticket_orders
    ADD COLUMN carriage_number VARCHAR(8) NULL,
    ADD COLUMN seat_number VARCHAR(8) NULL,
    ADD COLUMN seat_id BIGINT NULL,
    ADD KEY idx_ticket_orders_seat (train_trip_id, seat_id);

-- The seat must belong to the same run as the order.
ALTER TABLE ticket_orders
    ADD CONSTRAINT fk_ticket_orders_seat
        FOREIGN KEY (train_trip_id, seat_id)
        REFERENCES train_seats (train_trip_id, id);

-- Seat orders carry the same interval snapshot as priced orders.
ALTER TABLE ticket_orders
    DROP CHECK chk_ticket_orders_priced_snapshot;

ALTER TABLE ticket_orders
    DROP CHECK chk_ticket_orders_purchase_mode;

ALTER TABLE ticket_orders
    ADD CONSTRAINT chk_ticket_orders_purchase_mode
        CHECK ( purchase_mode IN ('LEGACY', 'PRICED', 'SEAT') ),
    ADD CONSTRAINT chk_ticket_orders_interval_snapshot
        CHECK ( purchase_mode = 'LEGACY'
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
                    )),
    ADD CONSTRAINT chk_ticket_orders_seat_snapshot
        CHECK (
            purchase_mode <> 'SEAT'
            OR (carriage_number IS NOT NULL
                AND seat_number IS NOT NULL
                AND seat_id IS NOT NULL)
            ),
    ADD CONSTRAINT chk_ticket_orders_quantity_snapshot
        CHECK (
            purchase_mode = 'SEAT'
            OR (carriage_number IS NULL
                AND seat_number IS NULL
                AND seat_id IS NULL
                )
            );

ALTER TABLE order_idempotency
    ADD COLUMN departure_station_code VARCHAR(20) NULL,
    ADD COLUMN arrival_station_code VARCHAR(20) NULL;

-- Replace the old mode-specific seat checks with one mode table.
ALTER TABLE order_idempotency
    DROP CHECK chk_order_idempotency_legacy_seat,
    DROP CHECK chk_order_idempotency_priced_seat,
    DROP CHECK chk_order_idempotency_seat_type,
    DROP CHECK chk_order_idempotency_purchase_mode;

ALTER TABLE order_idempotency
    ADD CONSTRAINT chk_order_idempotency_purchase_mode
        CHECK ( purchase_mode IN ('LEGACY', 'PRICED', 'SEAT') ),
    ADD CONSTRAINT chk_order_idempotency_seat_type
        CHECK (
            (purchase_mode = 'LEGACY' AND seat_type IS NULL)
                OR (purchase_mode IN ('PRICED', 'SEAT'))
                AND seat_type IS NOT NULL
                AND seat_type IN (0, 1, 2)
            ),
    ADD CONSTRAINT chk_order_idempotency_interval
        CHECK (
            (purchase_mode = 'SEAT'
                AND departure_station_code IS NOT NULL
                AND arrival_station_code IS NOT NULL)
                OR (purchase_mode <> 'SEAT'
                AND departure_station_code IS NULL
                AND arrival_station_code IS NULL)
            );

-- Every occupied segment must belong to exactly one order.
ALTER TABLE train_seat_segments
    ADD COLUMN order_id BIGINT NULL,
    ADD KEY idx_train_seat_segments_order (order_id);

-- No order owned a segment before this version; clear manual occupancy.
UPDATE train_seat_segments
SET seat_status = 'AVAILABLE'
WHERE seat_status = 'OCCUPIED';

ALTER TABLE train_seat_segments
    ADD CONSTRAINT fk_train_seat_segments_order
        FOREIGN KEY (order_id) REFERENCES ticket_orders (id);

ALTER TABLE train_seat_segments
    ADD CONSTRAINT chk_train_seat_segments_order
        CHECK (
            (seat_status = 'OCCUPIED' AND order_id IS NOT NULL)
                OR (seat_status = 'AVAILABLE' AND order_id IS NULL)
            );
