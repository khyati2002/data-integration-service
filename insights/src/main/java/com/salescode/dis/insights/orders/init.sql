
-- 1) Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2) Main orders table (partitioned parent)
CREATE TABLE orders (
    id UUID NOT NULL,
    order_number VARCHAR(255) NOT NULL,
    lob TEXT NOT NULL,
    "user" TEXT NOT NULL,  -- using quotes because 'user' is reserved
    operation TEXT NOT NULL CHECK (operation IN ('INSERT', 'UPDATE')),
    read_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (read_status IN ('PENDING', 'SUCCESS', 'FAILURE')),
    process_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (process_status IN ('PENDING', 'SUCCESS', 'FAILURE')),
    save_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (save_status IN ('PENDING', 'SUCCESS', 'FAILURE')),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at, lob)
) PARTITION BY RANGE (created_at);


ALTER TABLE orders
ADD COLUMN publish_status TEXT NOT NULL DEFAULT 'PENDING'
CHECK (publish_status IN ('PENDING', 'SUCCESS', 'FAILURE', 'NA'))

-- 3) LOB retention configuration table
CREATE TABLE lob_retention_config (
    lob TEXT PRIMARY KEY,
    retention_hours INTEGER NOT NULL DEFAULT 24,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by TEXT NOT NULL
);

-- 4) Insert default retention configs for common lobs
INSERT INTO lob_retention_config (lob, retention_hours, updated_by) VALUES
('lbpl', 24, 'system'),
('ckcoe', 48, 'system'),
('kvpl', 24, 'system');


-- 5) create indexes on parent so matching indexes are created on partitions automatically
-- a) lookup by order_number
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON public.orders (order_number);

-- b) composite for lob + created_at (common for dashboards)
CREATE INDEX IF NOT EXISTS idx_orders_lob_created_at ON public.orders (lob, created_at);

-- c) BRIN for created_at (very small, excellent for large time-ordered data)
CREATE INDEX IF NOT EXISTS idx_orders_created_at_brin ON public.orders USING BRIN (created_at);

-- d) user lookup (if you filter/search by user)
CREATE INDEX IF NOT EXISTS idx_orders_user ON public.orders ("user");

-- e) operation (if you filter by operation occasionally)
CREATE INDEX IF NOT EXISTS idx_orders_operation ON public.orders (operation);

-- f) partial indexes for status fields (recommended instead of full indexes)
CREATE INDEX IF NOT EXISTS idx_orders_read_failure ON public.orders (lob, created_at)
  WHERE read_status = 'FAILURE';
CREATE INDEX IF NOT EXISTS idx_orders_process_failure ON public.orders (lob, created_at)
  WHERE process_status = 'FAILURE';
CREATE INDEX IF NOT EXISTS idx_orders_save_failure ON public.orders (lob, created_at)
  WHERE save_status = 'FAILURE';



-- 6) Function to ensure partition exists for a given lob and timestamp
CREATE OR REPLACE FUNCTION ensure_partition_for_order(p_lob TEXT, p_created_at TIMESTAMPTZ)
RETURNS VOID AS $$
DECLARE
    bucket_start TIMESTAMPTZ;
    bucket_end TIMESTAMPTZ;
    time_partition_name TEXT;
    lob_partition_name TEXT;
    bucket_str TEXT;
BEGIN
    bucket_start := date_trunc('hour', p_created_at) +
                   (floor(date_part('minute', p_created_at) / 10) * interval '10 minutes');
    bucket_end := bucket_start + interval '10 minutes';
    bucket_str := to_char(bucket_start, 'YYYYMMDD_HH24MI');

    time_partition_name := 'orders_p_' || bucket_str;
    lob_partition_name := time_partition_name || '__' || p_lob;

    -- Create time-range partition (if missing)
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = time_partition_name AND n.nspname = 'public'
        ) THEN
            EXECUTE format('CREATE TABLE %I PARTITION OF public.orders FOR VALUES FROM (%L) TO (%L) PARTITION BY LIST (lob)',
                          time_partition_name, bucket_start, bucket_end);
            RAISE NOTICE 'Created time partition: %', time_partition_name;
        END IF;
    EXCEPTION WHEN duplicate_table THEN
        NULL;
    END;

    -- Create lob-specific partition (if missing)
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = lob_partition_name AND n.nspname = 'public'
        ) THEN
            EXECUTE format('CREATE TABLE %I PARTITION OF %I FOR VALUES IN (%L)',
                          lob_partition_name, time_partition_name, p_lob);
            RAISE NOTICE 'Created lob partition: %', lob_partition_name;

            -- IMPORTANT: Do NOT create indexes here. Instead, create indexes once on the
            -- parent 'orders' table so Postgres will automatically create matching indexes
            -- on each partition. This avoids duplicate index management and reduces write overhead.
        END IF;
    EXCEPTION WHEN duplicate_table THEN
        NULL;
    END;
END;
$$ LANGUAGE plpgsql;


-- 7) Trigger function to auto-create partitions
CREATE OR REPLACE FUNCTION trigger_ensure_partition_for_order()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM ensure_partition_for_order(NEW.lob, NEW.created_at);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- 8) Create the trigger
CREATE TRIGGER before_insert_orders
    BEFORE INSERT ON orders
    FOR EACH ROW
    EXECUTE FUNCTION trigger_ensure_partition_for_order();


-- 9) Helper function to find partitions older than cutoff for a specific lob
CREATE OR REPLACE FUNCTION find_expired_partitions(
    p_lob TEXT,
    cutoff_time TIMESTAMPTZ
)
RETURNS TABLE(partition_name TEXT, bucket_start TIMESTAMPTZ) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.relname::TEXT AS partition_name,
        to_timestamp(
            (regexp_match(c.relname, 'orders_p_(\d{8})_(\d{4})__'))[1] ||
            (regexp_match(c.relname, 'orders_p_(\d{8})_(\d{4})__'))[2],
            'YYYYMMDDHH24MI'
        ) AS bucket_start
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relname ~ ('^orders_p_\d{8}_\d{4}__' || p_lob || '$')
      AND to_timestamp(
            (regexp_match(c.relname, 'orders_p_(\d{8})_(\d{4})__'))[1] ||
            (regexp_match(c.relname, 'orders_p_(\d{8})_(\d{4})__'))[2],
            'YYYYMMDDHH24MI'
          ) + interval '10 minutes' <= cutoff_time;
END;
$$ LANGUAGE plpgsql;




-- 10 ) Alternative: PL/pgSQL function for database-side retention cleanup (for pg_cron option)
CREATE OR REPLACE FUNCTION cleanup_expired_partitions()
RETURNS TEXT AS $$
DECLARE
    lob_config RECORD;
    partition_rec RECORD;
    cutoff_time TIMESTAMPTZ;
    dropped_count INTEGER := 0;
    result_text TEXT := '';
BEGIN
    FOR lob_config IN SELECT lob, retention_hours FROM lob_retention_config LOOP
        cutoff_time := NOW() - (lob_config.retention_hours || ' hours')::INTERVAL;

        FOR partition_rec IN
            SELECT partition_name FROM find_expired_partitions(lob_config.lob, cutoff_time)
        LOOP
            -- Safety check: ensure partition name matches exact pattern
            IF partition_rec.partition_name ~ '^orders_p_\d{8}_\d{4}__[a-zA-Z0-9_]+$' THEN
                EXECUTE 'DROP TABLE IF EXISTS ' || partition_rec.partition_name;
                dropped_count := dropped_count + 1;
                result_text := result_text || 'Dropped partition: ' || partition_rec.partition_name || E'\n';
            END IF;
        END LOOP;
    END LOOP;

    IF dropped_count = 0 THEN
        result_text := 'No expired partitions found to drop.';
    ELSE
        result_text := result_text || 'Total partitions dropped: ' || dropped_count;
    END IF;

    RETURN result_text;
END;
$$ LANGUAGE plpgsql;




-- 11 to handle same oder and lob.
CREATE OR REPLACE FUNCTION replace_existing_order_on_conflict()
RETURNS TRIGGER AS $$
BEGIN
  -- Delete any existing rows with same order_number and lob
  -- This will remove rows in child partitions as well.
  DELETE FROM public.orders
  WHERE order_number = NEW.order_number
    AND lob = NEW.lob;

  -- allow the new row to be inserted
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 12 - trigger for replace_existing_order
CREATE TRIGGER before_insert_replace_order
  BEFORE INSERT ON public.orders
  FOR EACH ROW
  EXECUTE FUNCTION replace_existing_order_on_conflict();

-- Comments and instructions:
-- 1. This schema supports 10-minute time-range partitions with per-lob child partitions
-- 2. Partition naming: orders_p_YYYYMMDD_HHMM__<lob>
-- 3. Auto-creation happens via trigger before each insert
-- 4. Indexes are automatically created on new partitions
-- 5. For pg_cron alternative: SELECT cron.schedule('cleanup-partitions', '*/10 * * * *', 'SELECT cleanup_expired_partitions();');
-- 6. Required privileges: CREATE TABLE, DROP TABLE, CREATE FUNCTION, CREATE TRIGGER

