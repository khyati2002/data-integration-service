

CREATE SCHEMA IF NOT EXISTS public;

-- Create insights_metadata table
CREATE TABLE IF NOT EXISTS public.insights_metadata (
    id character varying(255) NOT NULL,
    value character varying(3000),
    key character varying(255) NOT NULL
);

-- Create integration_job table
CREATE TABLE IF NOT EXISTS public.integration_job (
    id character varying(255) NOT NULL,
    creation_time timestamp(6) with time zone NOT NULL,
    last_modified_time timestamp(6) with time zone NOT NULL,
    lob character varying(50) NOT NULL,
    extended_attributes jsonb,
    start_time timestamp(6) with time zone NOT NULL,
    end_time timestamp(6) with time zone,
    consumer_job_uri character varying(255),
    publisher_job_uri character varying(255),
    status character varying(255)
);

-- Create integration_file table
CREATE TABLE IF NOT EXISTS public.integration_file (
    id character varying(255) NOT NULL,
    creation_time timestamp(6) with time zone NOT NULL,
    last_modified_time timestamp(6) with time zone NOT NULL,
    lob character varying(50) NOT NULL,
    extended_attributes jsonb,
    start_time timestamp(6) with time zone NOT NULL,
    end_time timestamp(6) with time zone,
    total_count bigint,
    file_id character varying(255) NOT NULL,
    job_id character varying(255) NOT NULL,
    master character varying(255) NOT NULL,
    mode character varying(255) NOT NULL,
    status character varying(255)
);

-- Create file_stage_metrics table
CREATE TABLE IF NOT EXISTS public.file_stage_metrics (
    id character varying(255) NOT NULL,
    creation_time timestamp(6) with time zone NOT NULL,
    last_modified_time timestamp(6) with time zone NOT NULL,
    lob character varying(50) NOT NULL,
    extended_attributes jsonb,
    start_time timestamp(6) with time zone NOT NULL,
    end_time timestamp(6) with time zone,
    max_processing_time_ms integer,
    min_processing_time_ms integer,
    throughput numeric(10,2),
    logical_failure_count bigint,
    server_failure_count bigint,
    success_count bigint,
    file_id character varying(255) NOT NULL,
    job_id character varying(255) NOT NULL,
    master character varying(255) NOT NULL,
    mode character varying(255) NOT NULL,
    progress_status character varying(255),
    stage_type character varying(255)
);

-- Create stage_metadata table
CREATE TABLE IF NOT EXISTS public.stage_metadata (
    id character varying(255) NOT NULL,
    action_to_take character varying(255) NOT NULL,
    description character varying(255) NOT NULL,
    mode character varying(255) NOT NULL,
    name character varying(255),
    stage_type character varying(255) NOT NULL
);

-- Create file_report table
CREATE TABLE IF NOT EXISTS public.file_report (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    file_id character varying(255) NOT NULL,
    url text,
    name character varying(255),
    status character varying(255),
    error_message text
);

-- Add primary key constraints (with safety checks)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'insights_metadata_pkey') THEN
        ALTER TABLE public.insights_metadata ADD CONSTRAINT insights_metadata_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'integration_job_pkey') THEN
        ALTER TABLE public.integration_job ADD CONSTRAINT integration_job_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'integration_file_pkey') THEN
        ALTER TABLE public.integration_file ADD CONSTRAINT integration_file_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'file_stage_metrics_pkey') THEN
        ALTER TABLE public.file_stage_metrics ADD CONSTRAINT file_stage_metrics_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'stage_metadata_pkey') THEN
        ALTER TABLE public.stage_metadata ADD CONSTRAINT stage_metadata_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'file_report_pkey') THEN
        ALTER TABLE public.file_report ADD CONSTRAINT file_report_pkey PRIMARY KEY (id);
    END IF;
END $$;

-- Add check constraints (with safety checks)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'integration_job_status_check') THEN
        ALTER TABLE public.integration_job ADD CONSTRAINT integration_job_status_check
        CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'RUNNING'::character varying, 'COMPLETED_SUCCESSFULLY'::character varying, 'COMPLETED_UNSUCCESSFULLY'::character varying, 'FAILED'::character varying, 'ABORTED'::character varying])::text[])));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'integration_file_status_check') THEN
        ALTER TABLE public.integration_file ADD CONSTRAINT integration_file_status_check
        CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'RUNNING'::character varying, 'COMPLETED_SUCCESSFULLY'::character varying, 'COMPLETED_UNSUCCESSFULLY'::character varying, 'FAILED'::character varying, 'ABORTED'::character varying])::text[])));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'file_stage_metrics_progress_status_check') THEN
        ALTER TABLE public.file_stage_metrics ADD CONSTRAINT file_stage_metrics_progress_status_check
        CHECK (((progress_status)::text = ANY ((ARRAY['PENDING'::character varying, 'RUNNING'::character varying, 'COMPLETED_SUCCESSFULLY'::character varying, 'COMPLETED_UNSUCCESSFULLY'::character varying, 'FAILED'::character varying, 'ABORTED'::character varying])::text[])));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'file_stage_metrics_stage_type_check') THEN
        ALTER TABLE public.file_stage_metrics ADD CONSTRAINT file_stage_metrics_stage_type_check
        CHECK (((stage_type)::text = ANY ((ARRAY['read'::character varying, 'PUBLISH'::character varying, 'QUEUE'::character varying, 'PROCESS'::character varying, 'SAVE'::character varying])::text[])));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'stage_metadata_mode_check') THEN
        ALTER TABLE public.stage_metadata ADD CONSTRAINT stage_metadata_mode_check
        CHECK (((mode)::text = ANY ((ARRAY['CK_FILE'::character varying, 'CK_API'::character varying, 'CK_API_CLIENT'::character varying])::text[])));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'stage_metadata_stage_type_check') THEN
        ALTER TABLE public.stage_metadata ADD CONSTRAINT stage_metadata_stage_type_check
        CHECK (((stage_type)::text = ANY ((ARRAY['read'::character varying, 'PUBLISH'::character varying, 'QUEUE'::character varying, 'PROCESS'::character varying, 'SAVE'::character varying])::text[])));
    END IF;
END $$;

-- Add unique constraints (with safety checks)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'integration_file_file_id_master_key') THEN
        ALTER TABLE public.integration_file ADD CONSTRAINT integration_file_file_id_master_key UNIQUE (file_id, master);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukg3qhmv2pw8c6axljfkcy5y5pf') THEN
        ALTER TABLE public.integration_file ADD CONSTRAINT ukg3qhmv2pw8c6axljfkcy5y5pf UNIQUE (file_id, master);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'file_stage_metrics_file_id_stage_type_key') THEN
        ALTER TABLE public.file_stage_metrics ADD CONSTRAINT file_stage_metrics_file_id_stage_type_key UNIQUE (file_id, stage_type);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk4brla8wsd5bk8jcoitpyw61t3') THEN
        ALTER TABLE public.file_stage_metrics ADD CONSTRAINT uk4brla8wsd5bk8jcoitpyw61t3 UNIQUE (file_id, stage_type);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'stage_metadata_mode_stage_type_key') THEN
        ALTER TABLE public.stage_metadata ADD CONSTRAINT stage_metadata_mode_stage_type_key UNIQUE (mode, stage_type);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukgb13the76ugmnsa5bdtsfbqbv') THEN
        ALTER TABLE public.stage_metadata ADD CONSTRAINT ukgb13the76ugmnsa5bdtsfbqbv UNIQUE (mode, stage_type);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'file_report_file_id_key') THEN
        ALTER TABLE public.file_report ADD CONSTRAINT file_report_file_id_key UNIQUE (file_id);
    END IF;
END $$;

-- Create index (with safety check)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_job_lob_master') THEN
        CREATE INDEX idx_job_lob_master ON public.integration_job USING btree (lob, start_time DESC);
    END IF;
END $$;

-- Add foreign key constraints (with safety checks)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk7ct7hcm52qqv7722p9jpmu27r') THEN
        ALTER TABLE public.integration_file ADD CONSTRAINT fk7ct7hcm52qqv7722p9jpmu27r
        FOREIGN KEY (job_id) REFERENCES public.integration_job(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkfvdr1c4ucv6l2xla7b3x99fts') THEN
        ALTER TABLE public.file_stage_metrics ADD CONSTRAINT fkfvdr1c4ucv6l2xla7b3x99fts
        FOREIGN KEY (job_id) REFERENCES public.integration_job(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkrlbgf6sgxaixfeyh7scuwxitp') THEN
        ALTER TABLE public.file_stage_metrics ADD CONSTRAINT fkrlbgf6sgxaixfeyh7scuwxitp
        FOREIGN KEY (file_id) REFERENCES public.integration_file(id);
    END IF;
END $$;