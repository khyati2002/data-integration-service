-- find expired partitions
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
    ) + interval '30 minutes' <= cutoff_time;
END;
$$ LANGUAGE plpgsql;

--  cleanup function
CREATE OR REPLACE FUNCTION cleanup_expired_partitions()
RETURNS TEXT AS $$
DECLARE
  lob_config   RECORD;
  partition_rec RECORD;
  cutoff_time  TIMESTAMPTZ;
  dropped_count INTEGER := 0;
  result_text  TEXT := '';
BEGIN
  FOR lob_config IN SELECT lob, retention_hours FROM public.lob_retention_config LOOP
    cutoff_time := NOW() - (lob_config.retention_hours || ' hours')::INTERVAL;
    FOR partition_rec IN
      SELECT partition_name FROM find_expired_partitions(lob_config.lob, cutoff_time)
    LOOP
      IF partition_rec.partition_name ~ '^orders_p_\d{8}_\d{4}__[a-zA-Z0-9_]+' THEN
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
