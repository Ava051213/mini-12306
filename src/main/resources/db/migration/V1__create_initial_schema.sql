CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       username VARCHAR(50) NOT NULL,
                       password_hash VARCHAR(100) NOT NULL,
                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       PRIMARY KEY (id),
    -- Enforce username uniqueness in the database.
                       UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE passengers (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            user_id BIGINT NOT NULL,
                            real_name VARCHAR(50) NOT NULL,
                            id_card VARCHAR(18) NOT NULL,
                            phone VARCHAR(11) NOT NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
    -- Prevent duplicate identity documents within the same account.
                            UNIQUE KEY uk_passengers_user_id_card (user_id, id_card),
    -- Require an existing account for every passenger.
                            CONSTRAINT fk_passengers_user
                                FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

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

CREATE TABLE ticket_inventory
(
    train_trip_id     BIGINT NOT NULL,
    total_tickets     INT    NOT NULL,
    remaining_tickets INT    NOT NULL,
    PRIMARY KEY (train_trip_id),
    CONSTRAINT fk_ticket_inventory_train_trip
        FOREIGN KEY (train_trip_id) REFERENCES train_trips (id),
    CONSTRAINT
        chk_ticket_inventory_counts
        CHECK (
            total_tickets >= 0
                AND remaining_tickets >= 0
                AND remaining_tickets <= total_tickets
            )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ticket_orders (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               user_id BIGINT NOT NULL,
                               train_trip_id BIGINT NOT NULL,
                               passenger_id BIGINT NOT NULL,
                               passenger_name VARCHAR(50) NOT NULL,
                               passenger_id_card VARCHAR(18) NOT NULL,
                               status VARCHAR(20) NOT NULL,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               PRIMARY KEY (id),
                               KEY idx_ticket_orders_user_id (user_id, id),
                               CONSTRAINT fk_ticket_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
                               CONSTRAINT fk_ticket_orders_train_trip FOREIGN KEY (train_trip_id) REFERENCES train_trips(id),
                               CONSTRAINT fk_ticket_orders_passenger FOREIGN KEY (passenger_id) REFERENCES passengers(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
