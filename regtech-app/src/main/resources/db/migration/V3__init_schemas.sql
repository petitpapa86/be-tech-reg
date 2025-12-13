-- V1__init_schemas.sql
-- Initial migration to create all database schemas for the RegTech application
-- This migration establishes the schema structure for all bounded contexts

-- Core Schema
-- Purpose: Core Infrastructure - stores outbox messages, inbox messages, and event processing failures for the transactional outbox pattern
CREATE SCHEMA IF NOT EXISTS core;

-- IAM Schema
-- Purpose: Identity and Access Management - stores users, roles, permissions, authentication tokens, and bank assignments
CREATE SCHEMA IF NOT EXISTS iam;

-- Billing Schema
-- Purpose: Billing and Subscription Management - handles billing accounts, subscriptions, invoices, and payment processing
CREATE SCHEMA IF NOT EXISTS billing;

-- Ingestion Schema
-- Purpose: Data Ingestion - manages batch uploads, file processing, and raw data intake
CREATE SCHEMA IF NOT EXISTS ingestion;

-- Data Quality Schema
-- Purpose: Data Quality Management - stores quality reports, business rules, rule violations, and validation results
CREATE SCHEMA IF NOT EXISTS dataquality;

-- Risk Calculation Schema
-- Purpose: Risk Calculation and Analysis - handles exposures, mitigations, portfolio analysis, and risk metrics
CREATE SCHEMA IF NOT EXISTS riskcalculation;

-- Report Generation Schema
-- Purpose: Report Generation - manages generated reports, report metadata, and regulatory reporting outputs
CREATE SCHEMA IF NOT EXISTS reportgeneration;
