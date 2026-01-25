# Observability Implementation Guide

This document tracks the implementation of the observability stack for the RegTech platform. It covers the infrastructure setup, application integration, and the event-driven metrics strategy.

## 1. Overview

We have implemented a comprehensive observability layer to monitor the health, performance, and business metrics of the application. The stack includes:

*   **Metrics**: Prometheus (Collection) + Micrometer (Instrumentation)
*   **Visualization**: Grafana
*   **Tracing**: Zipkin (Distributed Tracing)
*   **Logging**: Loki (Aggregation) + Promtail (Collection)

## 2. Infrastructure Setup

The infrastructure is containerized using Docker Compose. The configuration is located in the `observability` directory.

### Services

| Service | Port | Description |
| :--- | :--- | :--- |
| **Prometheus** | `9090` | Scrapes metrics from the application's `/actuator/prometheus` endpoint. |
| **Grafana** | `3000` | Visualizes metrics. Default login: `admin` / `admin`. |
| **Zipkin** | `9411` | Collects and visualizes distributed traces. |
| **Loki** | `3100` | Aggregates logs. |
| **Promtail** | - | Ships logs from `./logs` to Loki. |

### Configuration Files

*   **[docker-compose.yml](docker-compose.yml)**: Defines the services and network.
*   **[prometheus.yml](prometheus.yml)**: Configures Prometheus.
*   **[loki.yaml](loki.yaml)**: Configures Loki.
*   **[promtail-config.yaml](promtail-config.yaml)**: Configures Promtail to scrape logs from `../logs` (relative to observability folder).
*   **[grafana/](grafana/)**: Contains Grafana provisioning configuration (Dashboards and Datasources).

### Running the Infrastructure

```bash
cd observability
docker-compose up -d
```

## 3. Application Integration

### Dependencies

We added the following dependencies to the `pom.xml` (managed via Spring Boot 4 BOM):

*   `spring-boot-starter-actuator`: Exposes operational endpoints.
*   `io.micrometer:micrometer-registry-prometheus`: Exposes metrics in Prometheus format.
*   `io.micrometer:micrometer-tracing-bridge-brave`: Adds tracing support.
*   `io.zipkin.reporter2:zipkin-reporter-brave`: Reports traces to Zipkin.

### Event-Driven Metrics Strategy

To keep the application and domain layers clean, we adopted an **Event-Driven Architecture** for metrics. Instead of recording metrics directly in business logic (Services/Handlers), we emit **Domain Events** and listen to them in the **Infrastructure Layer**.

#### Key Components

1.  **Domain Events**:
    *   `BatchUploadedEvent`: File uploaded.
    *   `BatchProcessingStartedEvent`: Processing began.
    *   `BatchValidatedEvent`: Validation complete (includes exposure count).
    *   `BatchStoredEvent`: Batch saved to DB.
    *   `BatchProcessingCompletedEvent`: Full cycle complete.
    *   `BatchProcessingFailedEvent`: Errors occurred.

2.  **Listener**:
    *   `IngestionMetricsListener` (Infrastructure Layer): Listens to the above events and records metrics via `MeterRegistry`.
    *   `@Observed` Annotation: Used on `UploadAndProcessFileCommandHandler` to automatically record execution duration and errors (`ingestion.batch.upload`).

#### Metrics Recorded

| Metric Name | Type | Tags | Description |
| :--- | :--- | :--- | :--- |
| `ingestion.batch.uploaded` | Counter | `bank_id` | Number of files uploaded. |
| `ingestion.batch.upload` | Timer | `error`, `status` | Execution time of the upload handler. |
| `ingestion.batch.started` | Counter | `bank_id` | Number of batches started processing. |
| `ingestion.batch.validated` | Counter | `bank_id` | Number of batches successfully validated. |
| `ingestion.batch.exposures` | Summary | - | Distribution of exposure counts per batch. |
| `ingestion.batch.stored` | Counter | `bank_id` | Number of batches persisted. |
| `ingestion.batch.completed` | Counter | `bank_id` | Number of batches fully processed. |
| `ingestion.file.size` | Summary | - | Distribution of processed file sizes. |
| `ingestion.batch.failed` | Counter | `bank_id`, `error_type` | Number of failed batches. |

## 4. Verification

1.  **Start Infrastructure**: Run `docker-compose up -d` in the `observability` folder.
2.  **Start Application**: Run the Spring Boot app.
3.  **Trigger Actions**: Upload a file or process a batch.
4.  **Check Prometheus**: Visit `http://localhost:9090/targets` to ensure the app is UP.
5.  **Check Grafana**: Visit `http://localhost:3000` and query for `ingestion_batch_started_total`.

## 5. Dashboard

The **Ingestion Dashboard** (`ingestion-dashboard.json`) provides real-time visibility into the ingestion process:

*   **Total Batches Uploaded**: Counter of files successfully uploaded (`ingestion_batch_uploaded_total`).
*   **Total Batches Completed**: Counter of batches successfully processed (`ingestion_batch_completed_total`).
*   **Total Batches Failed**: Counter of failed batches (`ingestion_batch_failed_total`).
*   **Ingestion Rate**: Time-series graph showing the rate of uploads, completions, and failures per second.

## 6. Future Steps

*   [x] Create a standard Grafana Dashboard for Ingestion.
*   [ ] Add Alerts for high failure rates.
*   [ ] Extend pattern to other modules (Risk, Reporting).
