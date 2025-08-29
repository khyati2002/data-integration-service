--  Function to ensure partition exists for a given lob and timestamp
CREATE OR REPLACE FUNCTION ensure_partition_for_order(p_lob TEXT, p_created_at TIMESTAMPTZ)
RETURNS VOID AS $$
DECLARE
    bucket_start TIMESTAMPTZ;
    bucket_end   TIMESTAMPTZ;
    time_partition_name TEXT;
    lob_partition_name  TEXT;
    bucket_str TEXT;
BEGIN
    bucket_start := date_trunc('hour', p_created_at) +
                    (floor(date_part('minute', p_created_at) / 30) * interval '30 minutes');
    bucket_end := bucket_start + interval '30 minutes';
    bucket_str := to_char(bucket_start, 'YYYYMMDD_HH24MI');

    time_partition_name := 'orders_p_' || bucket_str;
    lob_partition_name  := time_partition_name || '__' || p_lob;

    -- create time-range partition
    IF NOT EXISTS (
      SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE c.relname = time_partition_name AND n.nspname = 'public'
    ) THEN
      EXECUTE format(
        'CREATE TABLE %I PARTITION OF public.orders FOR VALUES FROM (%L) TO (%L) PARTITION BY LIST (lob)',
        time_partition_name, bucket_start, bucket_end
      );
    END IF;

    -- create lob-specific partition
    IF NOT EXISTS (
      SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE c.relname = lob_partition_name AND n.nspname = 'public'
    ) THEN
      EXECUTE format(
        'CREATE TABLE %I PARTITION OF %I FOR VALUES IN (%L)',
        lob_partition_name, time_partition_name, p_lob
      );
    END IF;
END;
$$ LANGUAGE plpgsql;

--  trigger wrapper
CREATE OR REPLACE FUNCTION trigger_ensure_partition_for_order()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM ensure_partition_for_order(NEW.lob, NEW.created_at);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create the trigger
DROP TRIGGER IF EXISTS before_insert_orders ON public.orders;
CREATE TRIGGER before_insert_orders
  BEFORE INSERT ON public.orders
  FOR EACH ROW
  EXECUTE FUNCTION trigger_ensure_partition_for_order();
