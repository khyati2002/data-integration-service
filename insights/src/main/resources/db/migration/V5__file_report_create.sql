CREATE TABLE if not exists file_report (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id VARCHAR(255) NOT NULL UNIQUE,
    url VARCHAR(1024),
    name VARCHAR(255),
    status VARCHAR(255),
    error_message VARCHAR(1024)
);
