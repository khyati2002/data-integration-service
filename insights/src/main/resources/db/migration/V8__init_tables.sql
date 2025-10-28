-- Flyway Migration Script
-- Description: Creates all tables, indexes, constraints, and triggers with proper ordering

-- ============================================================================
-- SECTION 1: CREATE TABLES (WITHOUT FOREIGN KEYS)
-- ============================================================================

-- Create integration_job table first (referenced by other tables)
CREATE TABLE IF NOT EXISTS integration_job (
    id character varying(255) NOT NULL,
    creation_time timestamp(6) with time zone NOT NULL,
    last_modified_time timestamp(6) with time zone NOT NULL,
    lob character varying(50) NOT NULL,
    extended_attributes jsonb,
    start_time timestamp(6) with time zone NOT NULL,
    end_time timestamp(6) with time zone,
    consumer_job_uri character varying(255),
    publisher_job_uri character varying(255),
    status character varying(255),
    CONSTRAINT integration_job_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'RUNNING'::character varying, 'COMPLETED_SUCCESSFULLY'::character varying, 'COMPLETED_UNSUCCESSFULLY'::character varying, 'FAILED'::character varying, 'ABORTED'::character varying])::text[])))
);

-- Create integration_file table (referenced by file_stage_metrics)
CREATE TABLE IF NOT EXISTS integration_file (
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
    status character varying(255),
    CONSTRAINT integration_file_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'RUNNING'::character varying, 'COMPLETED_SUCCESSFULLY'::character varying, 'COMPLETED_UNSUCCESSFULLY'::character varying, 'FAILED'::character varying, 'ABORTED'::character varying])::text[])))
);

-- Create file_stage_metrics table
CREATE TABLE IF NOT EXISTS file_stage_metrics (
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
    stage_type character varying(255),
    CONSTRAINT file_stage_metrics_progress_status_check CHECK (((progress_status)::text = ANY ((ARRAY['PENDING'::character varying, 'RUNNING'::character varying, 'COMPLETED_SUCCESSFULLY'::character varying, 'COMPLETED_UNSUCCESSFULLY'::character varying, 'FAILED'::character varying, 'ABORTED'::character varying])::text[]))),
    CONSTRAINT file_stage_metrics_stage_type_check CHECK (((stage_type)::text = ANY ((ARRAY['READ'::character varying, 'PUBLISH'::character varying, 'QUEUE'::character varying, 'PROCESS'::character varying, 'SAVE'::character varying])::text[])))
);

-- Create insights_metadata table
CREATE TABLE IF NOT EXISTS insights_metadata (
    id character varying(255) NOT NULL,
    value character varying(3000),
    key character varying(255) NOT NULL
);

-- Create stage_metadata table
CREATE TABLE IF NOT EXISTS stage_metadata (
    id character varying(255) NOT NULL,
    action_to_take character varying(255) NOT NULL,
    description character varying(255) NOT NULL,
    mode character varying(255) NOT NULL,
    name character varying(255),
    stage_type character varying(255) NOT NULL,
    CONSTRAINT stage_metadata_stage_type_check CHECK (((stage_type)::text = ANY ((ARRAY['READ'::character varying, 'PUBLISH'::character varying, 'QUEUE'::character varying, 'PROCESS'::character varying, 'SAVE'::character varying])::text[])))
);

-- Create lob_retention_config table
CREATE TABLE IF NOT EXISTS lob_retention_config (
    lob character varying(255) NOT NULL,
    retention_hours integer DEFAULT 24 NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by character varying(255)
);

-- Create file_report table
CREATE TABLE IF NOT EXISTS file_report (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    file_id character varying(255) NOT NULL,
    url text,
    name character varying(255),
    status character varying(255),
    error_message text
);

-- Create orders partitioned table
CREATE TABLE IF NOT EXISTS orders (
    id uuid NOT NULL,
    order_number character varying(255) NOT NULL,
    lob text NOT NULL,
    "user" text NOT NULL,
    operation text NOT NULL,
    publish_status text DEFAULT 'PENDING'::text NOT NULL,
    read_status text DEFAULT 'PENDING'::text NOT NULL,
    process_status text DEFAULT 'PENDING'::text NOT NULL,
    save_status text DEFAULT 'PENDING'::text NOT NULL,
    created_at timestamp with time zone NOT NULL,
    error_message text,
    CONSTRAINT orders_operation_check CHECK ((operation = ANY (ARRAY['INSERT'::text, 'UPDATE'::text, 'UNKNOWN'::text]))),
    CONSTRAINT orders_process_status_check CHECK ((process_status = ANY (ARRAY['PENDING'::text, 'SUCCESS'::text, 'FAILURE'::text]))),
    CONSTRAINT orders_publish_status_check CHECK ((publish_status = ANY (ARRAY['SUCCESS'::text, 'NA'::text, 'PENDING'::text]))),
    CONSTRAINT orders_read_status_check CHECK ((read_status = ANY (ARRAY['PENDING'::text, 'SUCCESS'::text, 'FAILURE'::text]))),
    CONSTRAINT orders_save_status_check CHECK ((save_status = ANY (ARRAY['PENDING'::text, 'SUCCESS'::text, 'FAILURE'::text])))
) PARTITION BY RANGE (created_at);

-- ============================================================================
-- SECTION 2: ADD PRIMARY KEYS
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'integration_job_pkey') THEN
        ALTER TABLE integration_job ADD CONSTRAINT integration_job_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'integration_file_pkey') THEN
        ALTER TABLE integration_file ADD CONSTRAINT integration_file_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'file_stage_metrics_pkey') THEN
        ALTER TABLE file_stage_metrics ADD CONSTRAINT file_stage_metrics_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'insights_metadata_pkey') THEN
        ALTER TABLE insights_metadata ADD CONSTRAINT insights_metadata_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'stage_metadata_pkey') THEN
        ALTER TABLE stage_metadata ADD CONSTRAINT stage_metadata_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'lob_retention_config_pkey') THEN
        ALTER TABLE lob_retention_config ADD CONSTRAINT lob_retention_config_pkey PRIMARY KEY (lob);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'file_report_pkey') THEN
        ALTER TABLE file_report ADD CONSTRAINT file_report_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'orders_pkey') THEN
        ALTER TABLE ONLY orders ADD CONSTRAINT orders_pkey PRIMARY KEY (id, created_at, lob);
    END IF;
END $$;

-- ============================================================================
-- SECTION 3: ADD UNIQUE CONSTRAINTS
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'integration_file_file_id_master_key') THEN
        ALTER TABLE integration_file ADD CONSTRAINT integration_file_file_id_master_key UNIQUE (file_id, master);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukg3qhmv2pw8c6axljfkcy5y5pf') THEN
        ALTER TABLE integration_file ADD CONSTRAINT ukg3qhmv2pw8c6axljfkcy5y5pf UNIQUE (file_id, master);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'file_stage_metrics_file_id_stage_type_key') THEN
        ALTER TABLE file_stage_metrics ADD CONSTRAINT file_stage_metrics_file_id_stage_type_key UNIQUE (file_id, stage_type);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk4brla8wsd5bk8jcoitpyw61t3') THEN
        ALTER TABLE file_stage_metrics ADD CONSTRAINT uk4brla8wsd5bk8jcoitpyw61t3 UNIQUE (file_id, stage_type);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'stage_metadata_mode_stage_type_key') THEN
        ALTER TABLE stage_metadata ADD CONSTRAINT stage_metadata_mode_stage_type_key UNIQUE (mode, stage_type);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukgb13the76ugmnsa5bdtsfbqbv') THEN
        ALTER TABLE stage_metadata ADD CONSTRAINT ukgb13the76ugmnsa5bdtsfbqbv UNIQUE (mode, stage_type);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'file_report_file_id_key') THEN
        ALTER TABLE file_report ADD CONSTRAINT file_report_file_id_key UNIQUE (file_id);
    END IF;
END $$;

-- ============================================================================
-- SECTION 4: ADD FOREIGN KEYS (After all PKs are created)
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk7ct7hcm52qqv7722p9jpmu27r') THEN
        ALTER TABLE integration_file ADD CONSTRAINT fk7ct7hcm52qqv7722p9jpmu27r
        FOREIGN KEY (job_id) REFERENCES integration_job(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkfvdr1c4ucv6l2xla7b3x99fts') THEN
        ALTER TABLE file_stage_metrics ADD CONSTRAINT fkfvdr1c4ucv6l2xla7b3x99fts
        FOREIGN KEY (job_id) REFERENCES integration_job(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkrlbgf6sgxaixfeyh7scuwxitp') THEN
        ALTER TABLE file_stage_metrics ADD CONSTRAINT fkrlbgf6sgxaixfeyh7scuwxitp
        FOREIGN KEY (file_id) REFERENCES integration_file(id);
    END IF;
END $$;