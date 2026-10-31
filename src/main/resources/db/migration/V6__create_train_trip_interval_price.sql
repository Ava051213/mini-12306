CREATE TABLE train_trip_interval_prices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    train_trip_id BIGINT NOT NULL,
    departure_sequence INT NOT NULL,
    arrival_sequence INT NOT NULL,
    seat_type INT NOT NULL,
    price_cents INT NOT NULL,
    PRIMARY KEY (id),
    -- One price per trip, boarding position, alighting position and seat type
    UNIQUE KEY uk_train_trip_interval_prices
        (train_trip_id, departure_sequence, arrival_sequence, seat_type),
    -- Required by the arrival-side foreign key below.
    KEY idx_train_trip_interval_prices_arrival (train_trip_id, arrival_sequence),
    -- Both stops must belong to this very trip.
    CONSTRAINT fk_train_trip_interval_prices_departure
        FOREIGN KEY (train_trip_id, departure_sequence)
        REFERENCES train_trip_stops (train_trip_id, stop_sequence),
    CONSTRAINT fk_train_trip_interval_prices_arrival
        FOREIGN KEY (train_trip_id, arrival_sequence)
        REFERENCES train_trip_stops (train_trip_id, stop_sequence),
    CONSTRAINT chk_train_trip_interval_prices_seat_type
        CHECK ( seat_type IN (0, 1, 2) ),
    CONSTRAINT chk_train_trip_interval_prices_price
        CHECK ( price_cents > 0 ),
    CONSTRAINT chk_train_trip_interval_prices_order
        CHECK ( arrival_sequence > departure_sequence )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
