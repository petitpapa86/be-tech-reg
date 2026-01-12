-- V47: Create quality thresholds table
CREATE TABLE IF NOT EXISTS dataquality.quality_thresholds (
    id BIGSERIAL PRIMARY KEY,
    bank_id VARCHAR(100) NOT NULL,
    completeness_min_percent DECIMAL(5, 2) NOT NULL,
    accuracy_max_error_percent DECIMAL(5, 2) NOT NULL,
    timeliness_days INTEGER NOT NULL,
    consistency_percent DECIMAL(5, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL
);
