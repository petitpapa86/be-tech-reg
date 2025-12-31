# METRICS Context

This module contains the METRICS bounded context for the project. It is organized as a multi-module Maven project with four layers:

- `metrics-domain`: Domain layer (Dashboard statistics, Compliance state domain models)
- `metrics-application`: Application layer (use-cases and ports)
- `metrics-infrastructure`: Infrastructure layer (adapters, DB entities, exporters)
- `metrics-presentation`: Presentation layer (REST endpoints, dashboard APIs)

Next steps:

1. Add domain model classes to `metrics-domain`.
2. Implement use cases in `metrics-application`.
3. Add adapters and persistence in `metrics-infrastructure`.
4. Expose endpoints in `metrics-presentation`.

To build the whole context:

```bash
mvn -f "c:/Users/alseny/Desktop/react projects/regtech/metrics-context/pom.xml" clean install
```
