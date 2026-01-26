-- Create compliance reports table for metrics dashboard

CREATE TABLE IF NOT EXISTS metrics.compliance_reports (
    report_id               VARCHAR(64) PRIMARY KEY,
    batch_id                VARCHAR(128) NOT NULL,
    bank_id                 VARCHAR(128) NOT NULL,

    report_type              VARCHAR(64) NOT NULL,
    reporting_date           DATE NOT NULL,

    status                   VARCHAR(32) NOT NULL,
    generated_at             TIMESTAMPTZ NOT NULL,

    html_s3_uri              TEXT,
    xbrl_s3_uri              TEXT,

    html_presigned_url       TEXT,
    xbrl_presigned_url       TEXT,

    html_file_size           BIGINT,
    xbrl_file_size           BIGINT,

    overall_quality_score    NUMERIC(10, 2),
    compliance_status        VARCHAR(64),

    generation_duration_millis BIGINT,

    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_compliance_reports_bank_date
    ON metrics.compliance_reports (bank_id, reporting_date);

CREATE INDEX IF NOT EXISTS idx_compliance_reports_bank_generated
    ON metrics.compliance_reports (bank_id, generated_at DESC);
