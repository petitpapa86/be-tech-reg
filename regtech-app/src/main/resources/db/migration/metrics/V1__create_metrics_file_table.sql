CREATE TABLE IF NOT EXISTS metrics.metrics_file (
    filename VARCHAR(255) PRIMARY KEY,
    date VARCHAR(255),
    score DOUBLE PRECISION,
    status VARCHAR(255),
    batch_id VARCHAR(255),
    bank_id VARCHAR(255)
);
