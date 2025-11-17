package com.bcbs239.regtech.app.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Standalone database migration utility.
 * Run this to update the database schema for batch_id and bank_id columns.
 * 
 * Usage: java DatabaseMigrationTool
 */
public class DatabaseMigrationTool {

    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/regtech";
        String user = "postgres";
        String password = "dracons86";

        System.out.println("========================================");
        System.out.println("Database Schema Migration Tool");
        System.out.println("Fix: Increase batch_id and bank_id column lengths");
        System.out.println("========================================\n");

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("✓ Connected to database: " + url);

            // Create schemas if they don't exist
            System.out.println("\nCreating schemas if needed...");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS ingestion");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS dataquality");
            System.out.println("✓ Schemas ready");

            // Migrate ingestion schema
            System.out.println("\nMigrating ingestion schema...");
            try {
                stmt.execute(
                    "ALTER TABLE IF EXISTS ingestion.ingestion_batches " +
                    "ALTER COLUMN batch_id TYPE VARCHAR(255), " +
                    "ALTER COLUMN bank_id TYPE VARCHAR(255)"
                );
                System.out.println("✓ Ingestion schema migrated successfully");
            } catch (Exception e) {
                System.out.println("⚠ Ingestion schema: " + e.getMessage());
            }

            // Migrate data quality schema
            System.out.println("\nMigrating data quality schema...");
            try {
                stmt.execute(
                    "ALTER TABLE IF EXISTS dataquality.quality_reports " +
                    "ALTER COLUMN batch_id TYPE VARCHAR(255), " +
                    "ALTER COLUMN bank_id TYPE VARCHAR(255)"
                );
                System.out.println("✓ Quality reports table migrated");
            } catch (Exception e) {
                System.out.println("⚠ Quality reports: " + e.getMessage());
            }

            try {
                stmt.execute(
                    "ALTER TABLE IF EXISTS dataquality.quality_error_summaries " +
                    "ALTER COLUMN batch_id TYPE VARCHAR(255), " +
                    "ALTER COLUMN bank_id TYPE VARCHAR(255)"
                );
                System.out.println("✓ Quality error summaries table migrated");
            } catch (Exception e) {
                System.out.println("⚠ Quality error summaries: " + e.getMessage());
            }

            // Verify changes
            System.out.println("\nVerifying changes...");
            var rs = stmt.executeQuery(
                "SELECT table_schema, table_name, column_name, character_maximum_length " +
                "FROM information_schema.columns " +
                "WHERE table_schema IN ('ingestion', 'dataquality') " +
                "AND column_name IN ('batch_id', 'bank_id') " +
                "ORDER BY table_schema, table_name, column_name"
            );

            System.out.println("\nCurrent column lengths:");
            System.out.println("Schema\t\t\tTable\t\t\t\tColumn\t\tLength");
            System.out.println("------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%s\t\t%s\t\t%s\t\t%d%n",
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4)
                );
            }

            System.out.println("\n========================================");
            System.out.println("Migration completed successfully!");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("Migration failed!");
            System.err.println("========================================");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
