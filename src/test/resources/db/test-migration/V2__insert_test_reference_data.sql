INSERT INTO train_trips (
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