-- 1a) replace existing order on conflict (order_number + lob)
CREATE OR REPLACE FUNCTION replace_existing_order_on_conflict()
RETURNS TRIGGER AS $$
BEGIN
  DELETE FROM public.orders
   WHERE order_number = NEW.order_number
     AND lob = NEW.lob;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1b) create trigger for replacing exisiting order on conflict
DROP TRIGGER IF EXISTS before_insert_replace_order ON public.orders;
CREATE TRIGGER before_insert_replace_order
  BEFORE INSERT ON public.orders
  FOR EACH ROW
  EXECUTE FUNCTION replace_existing_order_on_conflict();


--  2a)Function to ensure that lob is present in retention config
CREATE OR REPLACE FUNCTION ensure_lob_in_retention_config()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if lob already exists
    IF NOT EXISTS (
        SELECT 1 FROM public.lob_retention_config lrc
        WHERE lrc.lob = NEW.lob
    ) THEN
        -- Insert with defaults
        INSERT INTO public.lob_retention_config (lob, retention_hours, updated_by)
        VALUES (NEW.lob, 24, 'system');
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2b) trigger to run function to check whether lob is present in retention config
DROP TRIGGER IF EXISTS trg_orders_ensure_lob ON public.orders;

CREATE TRIGGER trg_orders_ensure_lob
AFTER INSERT ON public.orders
FOR EACH ROW
EXECUTE FUNCTION ensure_lob_in_retention_config();

