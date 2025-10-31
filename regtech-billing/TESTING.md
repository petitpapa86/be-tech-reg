PaymentVerificationSaga integration testing

This module contains two E2E/integration style tests for the PaymentVerification saga flow:

- PaymentVerificationSagaManualE2ETest.java
  - A lightweight, in-memory end-to-end test that simulates startSaga -> persist (in-memory) -> dispatch -> handler -> compensation flow.
  - Does not start Spring or a DB. This is the recommended quick E2E test to run locally.

- PaymentVerificationSagaE2ETest.java
  - A more ambitious Spring-managed integration test that was causing JPA/auto-configuration issues in the CI environment and has been disabled.

How to run the in-memory E2E test only:

Windows (cmd.exe):

mvn -f "C:\\Users\\alseny\\Desktop\\react projects\\regtech\\pom.xml" -pl regtech-billing -Dtest=PaymentVerificationSagaManualE2ETest test

Notes:
- The in-memory test uses Mockito to stub external dependencies (Stripe service, billing repository) and verifies the compensation event publishing.
- If you need a full Spring-managed test (H2/Testcontainers) we can add one in a separate change; it requires providing DB schema initialization or disabling automatic DDL for some schemas.

