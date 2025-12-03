-- Migration to increase event_type column length in event_processing_failures table
-- This is needed to accommodate longer fully-qualified class names for domain events
-- Error: "il valore è troppo lungo per il tipo character varying(255)"

ALTER TABLE event_processing_failures 
ALTER COLUMN event_type TYPE VARCHAR(500);
