
ALTER TABLE public.orders
ADD COLUMN  if not exists error_message TEXT NULL;