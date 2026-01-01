CREATE TABLE IF NOT EXISTS metrics.metrics_file (
    filename VARCHAR(255) PRIMARY KEY,
    date VARCHAR(255),
    score DOUBLE,
    status VARCHAR(255),
    batch_id VARCHAR(255),
    bank_id VARCHAR(255)
);

-- Insert mock rows
INSERT INTO metrics.metrics_file (filename, date, score, status, batch_id, bank_id) VALUES
('esposizioni_settembre.xlsx', '15 Set 2025', 87.2, 'VIOLATIONS', 'batch-202509', 'BANK-XYZ'),
('grandi_esposizioni_agosto.xlsx', '28 Ago 2025', 94.1, 'COMPLIANT', 'batch-202508', 'BANK-XYZ');
