CREATE TABLE train_trips (
    id BIGINT NOT NULL AUTO_INCREMENT,
    train_number VARCHAR(20) NOT NULL,
    departure_station VARCHAR(50) NOT NULL,
    arrival_station varchar(50) NOT NULL,
    departure_at DATETIME NOT NULL,
    arrival_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_train_trips_train_departure (train_number, departure_at),
    KEY idx_train_trips_route_departure(departure_station, arrival_station, departure_at),
    CONSTRAINT chk_train_trips_arrival_after_departure CHECK (arrival_at > departure_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO train_trips(
                        train_number,
                        departure_station,
                        arrival_station,
                        departure_at,
                        arrival_at
) VALUES
      ('G101', '上海虹桥', '杭州东', '2026-11-01 08:00:00', '2026-11-01 09:00:00'),
      ('G103', '上海虹桥', '杭州东', '2026-11-01 11:00:00', '2026-11-01 12:00:00'),
      ('G105', '上海虹桥', '南京南', '2026-11-01 09:00:00', '2026-11-01 10:00:00'),
      ('G101', '上海虹桥', '杭州东', '2026-11-02 08:00:00', '2026-11-02 09:00:00');
