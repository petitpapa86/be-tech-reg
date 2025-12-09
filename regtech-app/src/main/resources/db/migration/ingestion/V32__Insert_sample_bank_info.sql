-- V32__Insert_sample_bank_info.sql
-- Insert sample bank information for testing
-- This mirrors the bank data from iam.banks for testing purposes

INSERT INTO ingestion.bank_info (bank_id, bank_name, bank_country, bank_status, last_updated) VALUES
    ('bank-unicredit', 'UniCredit S.p.A.', 'IT', 'ACTIVE', CURRENT_TIMESTAMP),
    ('bank-intesa-sanpaolo', 'Intesa Sanpaolo S.p.A.', 'IT', 'ACTIVE', CURRENT_TIMESTAMP),
    ('bank-monte-paschi', 'Banca Monte dei Paschi di Siena S.p.A.', 'IT', 'ACTIVE', CURRENT_TIMESTAMP),
    ('bank-banco-bpm', 'Banco BPM S.p.A.', 'IT', 'ACTIVE', CURRENT_TIMESTAMP),
    ('bank-ubi-banca', 'UBI Banca S.p.A.', 'IT', 'ACTIVE', CURRENT_TIMESTAMP),
    ('bank-bper', 'BPER Banca S.p.A.', 'IT', 'ACTIVE', CURRENT_TIMESTAMP),
    ('bank-mediobanca', 'Mediobanca S.p.A.', 'IT', 'ACTIVE', CURRENT_TIMESTAMP),
    ('bank-credem', 'Credito Emiliano S.p.A.', 'IT', 'ACTIVE', CURRENT_TIMESTAMP),
    ('bank-banca-carige', 'Banca Carige S.p.A.', 'IT', 'ACTIVE', CURRENT_TIMESTAMP),
    ('bank-banca-popolare-sondrio', 'Banca Popolare di Sondrio', 'IT', 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT (bank_id) DO NOTHING;
