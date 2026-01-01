-- =====================================================
-- Insert Regulations Data
-- =====================================================
-- This migration inserts the regulation records that
-- are referenced by business rules.
-- =====================================================

-- Insert BCBS 239 Data Quality Regulation
INSERT INTO dataquality.regulations (
    regulation_id,
    regulation_name,
    description,
    effective_date,
    version,
    status,
    created_at,
    updated_at
) VALUES (
    'BCBS_239_DATA_QUALITY',
    'BCBS 239 - Data Quality Requirements',
    'Basel Committee on Banking Supervision Principle 239: Strengthening the risk data aggregation capabilities and risk reporting practices of banks - Data Quality Requirements',
    '2024-01-01',
    '1.0',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (regulation_id) DO NOTHING;
