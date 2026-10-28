-- Add an explicit payment deadline column to every order.
ALTER TABLE ticket_orders
    ADD COLUMN pay_deadline DATETIME NULL;

-- Backfill historical rows from their creation time.
UPDATE ticket_orders
SET pay_deadline = DATE_ADD(created_at, INTERVAL 30 MINUTE);

-- Every order must carry a deadline once the backfill is complete.
ALTER TABLE ticket_orders
    MODIFY COLUMN pay_deadline DATETIME NOT NULL;

-- Support the timeout scan by status and deadline.
ALTER TABLE ticket_orders
    ADD KEY idx_ticket_orders_status_deadline (status, pay_deadline);
