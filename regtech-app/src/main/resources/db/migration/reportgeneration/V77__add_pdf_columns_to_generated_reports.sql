-- V77__add_pdf_columns_to_generated_reports.sql
-- Add PDF generation metadata columns to generated_reports table

ALTER TABLE reportgeneration.generated_reports
ADD COLUMN IF NOT EXISTS pdf_s3_uri TEXT,
ADD COLUMN IF NOT EXISTS pdf_file_size BIGINT,
ADD COLUMN IF NOT EXISTS pdf_presigned_url TEXT;

COMMENT ON COLUMN reportgeneration.generated_reports.pdf_s3_uri IS 'S3 URI for the PDF report';
COMMENT ON COLUMN reportgeneration.generated_reports.pdf_file_size IS 'Size of the PDF report in bytes';
COMMENT ON COLUMN reportgeneration.generated_reports.pdf_presigned_url IS 'Presigned URL for accessing the PDF report';
