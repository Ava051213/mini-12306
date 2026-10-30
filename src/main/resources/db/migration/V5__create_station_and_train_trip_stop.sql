CREATE TABLE stations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(50) NOT NULL,
    spell VARCHAR(50) NOT NULL,
    region_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    -- A station code identifies one station in the dictionary.
    UNIQUE KEY uk_stations_code (code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE train_trip_stops (
    id BIGINT NOT NULL AUTO_INCREMENT,
    train_trip_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    stop_sequence INT NOT NULL,
    arrival_at DATETIME NULL,
    departure_at DATETIME NULL,
    PRIMARY KEY (id),
    -- One position in the route can be used only once per trip.
    UNIQUE KEY uk_train_trip_stops_trip_sequence (train_trip_id, stop_sequence),
    KEY idx_train_trip_stops_station (station_id),
    CONSTRAINT fk_train_trip_stops_trip FOREIGN KEY (train_trip_id) REFERENCES train_trips(id),
    CONSTRAINT fk_train_trip_stops_station FOREIGN KEY (station_id) REFERENCES stations (id),
    CONSTRAINT chk_train_trip_stops_sequence CHECK (stop_sequence > 0),
    -- A stop needs at least one time, and arrival cannot be later than departure.
    CONSTRAINT chk_train_trip_stops_times CHECK (
        (arrival_at IS NOT NULL OR departure_at IS NOT NULL)
        AND (arrival_at IS NULL OR departure_at IS NULL OR arrival_at <= departure_at)
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
