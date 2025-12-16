-- V62__Insert_italian_banks.sql
-- Insert Italian banks for testing and development
-- These are major Italian banking institutions

INSERT INTO iam.banks (id, name, country_code, status, created_at, updated_at) VALUES
    ('bank-unicredit', 'UniCredit S.p.A.', 'ITA', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bank-intesa-sanpaolo', 'Intesa Sanpaolo S.p.A.', 'ITA', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bank-monte-paschi', 'Banca Monte dei Paschi di Siena S.p.A.', 'ITA', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bank-banco-bpm', 'Banco BPM S.p.A.', 'ITA', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bank-ubi-banca', 'UBI Banca S.p.A.', 'ITA', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bank-bper', 'BPER Banca S.p.A.', 'ITA', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bank-mediobanca', 'Mediobanca S.p.A.', 'ITA', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bank-credem', 'Credito Emiliano S.p.A.', 'ITA', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bank-banca-carige', 'Banca Carige S.p.A.', 'ITA', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bank-banca-popolare-sondrio', 'Banca Popolare di Sondrio', 'IT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

