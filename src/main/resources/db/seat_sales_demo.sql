-- 区间座位售票演示实例（开发库手工执行，不属于 Flyway 迁移）
--
-- 前提：开发库已迁到 V9，应用未在运行。
-- 用法：整段执行一次；重复执行会因 stations.code 唯一键失败。
-- 出错时执行 ROLLBACK，成功时 COMMIT。
-- 只新增本演示车次的数据，不修改既有车次、订单或库存。

START TRANSACTION;

-- 1. 演示站点：code 加 D 前缀，避免与既有站点冲突
INSERT INTO stations (code, name, spell, region_name) VALUES
    ('DSHH', '上海虹桥', 'shanghaihongqiao', '上海'),
    ('DSZB', '苏州北', 'suzhoubei', '江苏'),
    ('DWXD', '无锡东', 'wuxidong', '江苏'),
    ('DNJN', '南京南', 'nanjingnan', '江苏');

-- 2. 运行实例：四站三段
INSERT INTO train_trips (
    train_number, departure_station, arrival_station,
    departure_at, arrival_at
) VALUES (
    'D9001', '上海虹桥', '南京南',
    '2026-11-10 08:00:00', '2026-11-10 10:30:00'
);

SET @trip_id = (
    SELECT id
    FROM train_trips
    WHERE train_number = 'D9001'
      AND departure_at = '2026-11-10 08:00:00'
);

-- 3. 经停：站序连续，座位段按相邻段生成
INSERT INTO train_trip_stops (
    train_trip_id, station_id, stop_sequence, arrival_at, departure_at
)
SELECT @trip_id, id, 1, NULL, '2026-11-10 08:00:00'
FROM stations WHERE code = 'DSHH';

INSERT INTO train_trip_stops (
    train_trip_id, station_id, stop_sequence, arrival_at, departure_at
)
SELECT @trip_id, id, 2, '2026-11-10 08:45:00', '2026-11-10 08:50:00'
FROM stations WHERE code = 'DSZB';

INSERT INTO train_trip_stops (
    train_trip_id, station_id, stop_sequence, arrival_at, departure_at
)
SELECT @trip_id, id, 3, '2026-11-10 09:30:00', '2026-11-10 09:35:00'
FROM stations WHERE code = 'DWXD';

INSERT INTO train_trip_stops (
    train_trip_id, station_id, stop_sequence, arrival_at, departure_at
)
SELECT @trip_id, id, 4, '2026-11-10 10:30:00', NULL
FROM stations WHERE code = 'DNJN';

-- 4. 车厢与座位：01 车三个二等座，02 车一个一等座
INSERT INTO train_carriages (train_trip_id, carriage_number)
VALUES (@trip_id, '01'), (@trip_id, '02');

SET @carriage_two = (
    SELECT id FROM train_carriages
    WHERE train_trip_id = @trip_id AND carriage_number = '01'
);

SET @carriage_one = (
    SELECT id FROM train_carriages
    WHERE train_trip_id = @trip_id AND carriage_number = '02'
);

INSERT INTO train_seats (
    train_trip_id, carriage_id, seat_number, seat_type
) VALUES
    (@trip_id, @carriage_two, '01A', 2),
    (@trip_id, @carriage_two, '01B', 2),
    (@trip_id, @carriage_two, '01C', 2),
    (@trip_id, @carriage_one, '01A', 1);

-- 5. 每个座位三条相邻段，初始全部 AVAILABLE
INSERT INTO train_seat_segments (
    train_trip_id, seat_id, segment_sequence, seat_status
)
SELECT @trip_id, s.id, seq.n, 'AVAILABLE'
FROM train_seats s
JOIN (
    SELECT 1 AS n
    UNION ALL SELECT 2
    UNION ALL SELECT 3
) seq
WHERE s.train_trip_id = @trip_id;

-- 6. 区间票价，单位分：1→2、2→3、3→4 与跨段组合
INSERT INTO train_trip_interval_prices (
    train_trip_id, departure_sequence, arrival_sequence,
    seat_type, price_cents
) VALUES
    (@trip_id, 1, 2, 2, 5000),
    (@trip_id, 2, 3, 2, 4000),
    (@trip_id, 3, 4, 2, 6000),
    (@trip_id, 1, 3, 2, 9000),
    (@trip_id, 2, 4, 2, 10000),
    (@trip_id, 1, 4, 2, 15000),
    (@trip_id, 1, 3, 1, 15000),
    (@trip_id, 1, 4, 1, 25000);

-- 7. 守卫式切换：存在待支付或已支付订单时影响 0 行
UPDATE train_trips
SET sales_mode = 'SEAT'
WHERE id = @trip_id
  AND sales_mode = 'QUANTITY'
  AND NOT EXISTS (
      SELECT 1
      FROM ticket_orders o
      WHERE o.train_trip_id = @trip_id
        AND o.status IN ('PENDING_PAYMENT', 'PAID')
  );

COMMIT;

-- 8. 核对：sales_mode 应为 SEAT，座位 4 行，段 12 行，票价 8 行
SELECT id, train_number, sales_mode
FROM train_trips
WHERE id = @trip_id;

SELECT COUNT(*) AS seat_rows
FROM train_seats
WHERE train_trip_id = @trip_id;

SELECT COUNT(*) AS segment_rows
FROM train_seat_segments
WHERE train_trip_id = @trip_id;

SELECT COUNT(*) AS price_rows
FROM train_trip_interval_prices
WHERE train_trip_id = @trip_id;

-- 需要重跑演示时，确认这是演示车次后，按下列顺序清理再重新执行本脚本：
-- START TRANSACTION;
-- SET @trip_id = (
--     SELECT id FROM train_trips
--     WHERE train_number = 'D9001'
--       AND departure_at = '2026-11-10 08:00:00'
-- );
-- DELETE FROM train_seat_segments WHERE train_trip_id = @trip_id;
-- DELETE FROM order_idempotency
--     WHERE order_id IN (
--         SELECT id FROM ticket_orders WHERE train_trip_id = @trip_id
--     );
-- DELETE FROM ticket_orders WHERE train_trip_id = @trip_id;
-- DELETE FROM train_trip_interval_prices WHERE train_trip_id = @trip_id;
-- DELETE FROM train_seats WHERE train_trip_id = @trip_id;
-- DELETE FROM train_carriages WHERE train_trip_id = @trip_id;
-- DELETE FROM train_trip_stops WHERE train_trip_id = @trip_id;
-- DELETE FROM train_trips WHERE id = @trip_id;
-- DELETE FROM stations WHERE code IN ('DSHH', 'DSZB', 'DWXD', 'DNJN');
-- COMMIT;
