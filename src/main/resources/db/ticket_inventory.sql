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

-- Resolve trip IDs from the existing seed data.
INSERT INTO ticket_inventory (
    train_trip_id,
    total_tickets,
    remaining_tickets
)
SELECT id, 20, 20
FROM train_trips
WHERE (train_number, departure_at) IN (
                                       ('G101', '2026-11-01 08:00:00'),
                                       ('G103', '2026-11-01 11:00:00'),
                                       ('G105', '2026-11-01 09:00:00'),
                                       ('G101', '2026-11-02 08:00:00')
    );