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
