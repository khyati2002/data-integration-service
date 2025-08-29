--  Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Main orders table (partitioned parent)
CREATE TABLE IF NOT EXISTS public.orders (
    id UUID NOT NULL,
    order_number VARCHAR(255) NOT NULL,
    lob TEXT NOT NULL,
    "user" TEXT NOT NULL,
    operation TEXT NOT NULL CHECK (operation IN ('INSERT', 'UPDATE')),
    publish_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (publish_status IN ('SUCCESS','NA', 'PENDING')),
    read_status    TEXT NOT NULL DEFAULT 'PENDING' CHECK (read_status IN ('PENDING','SUCCESS','FAILURE')),
    process_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (process_status IN ('PENDING','SUCCESS','FAILURE')),
    save_status    TEXT NOT NULL DEFAULT 'PENDING' CHECK (save_status IN ('PENDING','SUCCESS','FAILURE')),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at, lob)
) PARTITION BY RANGE (created_at);

-- indexes on parent (auto-inherited by partitions)
CREATE INDEX IF NOT EXISTS idx_orders_order_number   ON public.orders (order_number);
CREATE INDEX IF NOT EXISTS idx_orders_lob_created_at ON public.orders (lob, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_created_at_brin ON public.orders USING BRIN (created_at);
CREATE INDEX IF NOT EXISTS idx_orders_user           ON public.orders ("user");
CREATE INDEX IF NOT EXISTS idx_orders_operation      ON public.orders (operation);

-- partial indexes for common failure lookups
CREATE INDEX IF NOT EXISTS idx_orders_read_failure ON public.orders (lob, created_at) WHERE read_status = 'FAILURE';
CREATE INDEX IF NOT EXISTS idx_orders_process_failure ON public.orders (lob, created_at) WHERE process_status = 'FAILURE';
CREATE INDEX IF NOT EXISTS idx_orders_save_failure ON public.orders (lob, created_at) WHERE save_status = 'FAILURE';

--  LOB retention configuration table
CREATE TABLE IF NOT EXISTS public.lob_retention_config (
    lob TEXT PRIMARY KEY,
    retention_hours INTEGER NOT NULL DEFAULT 24,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by TEXT
);

----  defaults (use ON CONFLICT to be idempotent)
--INSERT INTO public.lob_retention_config (lob, retention_hours, updated_by) VALUES
--  ('lbpl', 24, 'system'),
--  ('ckcoe', 48, 'system'),
--  ('kvpl', 24, 'system')
--ON CONFLICT (lob) DO NOTHING;

