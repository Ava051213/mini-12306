CREATE TABLE train_carriages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    train_trip_id BIGINT NOT NULL,
    carriage_number VARCHAR(8) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_train_carriages_number (train_trip_id, carriage_number),
    UNIQUE KEY uk_train_carriages_trip_id (train_trip_id, id),
    CONSTRAINT fk_train_carriages_trip FOREIGN KEY (train_trip_id) REFERENCES train_trips (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE train_seats (
    id BIGINT NOT NULL AUTO_INCREMENT,
    train_trip_id BIGINT NOT NULL,
    carriage_id BIGINT NOT NULL,
    seat_number VARCHAR(8) NOT NULL,
    seat_type INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_train_seats_number (train_trip_id, carriage_id, seat_number),
    UNIQUE KEY uk_train_seats_trip_id (train_trip_id, id),
    CONSTRAINT fk_train_seats_carriage
        FOREIGN KEY (train_trip_id, carriage_id)
        REFERENCES train_carriages (train_trip_id, id),
    CONSTRAINT chk_train_seats_seat_type CHECK ( seat_type IN (0, 1, 2) )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE train_seat_segments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    train_trip_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    segment_sequence INT NOT NULL,
    seat_status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_train_seat_segments (train_trip_id, seat_id, segment_sequence),
    KEY idx_train_seat_segments_trip (train_trip_id, seat_status, seat_id),
    KEY idx_train_seat_segments_stop (train_trip_id, segment_sequence),
    CONSTRAINT fk_train_seat_segments_seat
        FOREIGN KEY (train_trip_id, seat_id)
        REFERENCES train_seats(train_trip_id, id),
    CONSTRAINT fk_train_seat_segments_stop
        FOREIGN KEY (train_trip_id, segment_sequence)
        REFERENCES train_trip_stops (train_trip_id, stop_sequence),
    CONSTRAINT chk_train_seat_segments_sequence CHECK ( segment_sequence > 0 ),
    CONSTRAINT chk_train_seat_segments_status CHECK ( seat_status IN ('AVAILABLE', 'OCCUPIED') )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
