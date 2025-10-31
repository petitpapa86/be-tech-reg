# Development README

This file explains how to run the project locally and how to configure your IDE to avoid spurious 'unused' inspections for Spring beans.

## Environment variables
Create a local `.env` (or set env vars in your shell) with the following keys. Do not commit secrets to source control.

- STRIPE_API_KEY=sk_test_xxx
- STRIPE_WEBHOOK_SECRET=whsec_xxx
- SPRING_PROFILES_ACTIVE=development

On Windows cmd.exe:

```cmd
set STRIPE_API_KEY=sk_test_xxx
set STRIPE_WEBHOOK_SECRET=whsec_xxx
set SPRING_PROFILES_ACTIVE=development
```

On PowerShell:

```powershell
$env:STRIPE_API_KEY = "sk_test_xxx"
$env:STRIPE_WEBHOOK_SECRET = "whsec_xxx"
$env:SPRING_PROFILES_ACTIVE = "development"
```

## Avoiding 'unused' inspections in IDEs
Modern IDEs sometimes mark Spring beans or components as 'unused' because they're referenced reflectively by the framework. Prefer to configure your IDE's Spring support rather than adding marker code.

IntelliJ IDEA:
- Enable "Spring" and "Spring Boot" plugins (Settings > Plugins).
- Open the project as a Maven project (right-click `pom.xml` > Add as Maven Project).
- Ensure `spring` facets are enabled (Settings > Languages & Frameworks > Spring). This allows IDEA to recognise `@Component`, `@Bean`, `@EventListener`, etc.
- You can also disable the specific inspection: Settings > Editor > Inspections > Java > Declaration redundancy > Unused declaration. Prefer to scope the suppression to the module only.

VS Code (with Java extensions):
- Install "Language Support for Java(TM) by Red Hat" and "Spring Boot Extension Pack".
- Enable Spring Boot project support via extension settings.

If you still see false positives, add a targeted `@SuppressWarnings("unused")` on the specific class or constructor (last resort).

## Build & run
From project root (Windows cmd.exe):

```cmd
mvn -DskipTests -pl regtech-app -am package
set STRIPE_API_KEY=sk_test_xxx
set STRIPE_WEBHOOK_SECRET=whsec_xxx
mvn -Dspring-boot.run.profiles=development -Dspring.main.web-application-type=none -pl regtech-app spring-boot:run
```

This runs the application without starting the web server (useful when you only need background jobs and event processing in dev).

## Notes
- Do not commit actual secret keys. Use a secrets manager or environment variables in CI/CD.
- If you prefer markers in code to silence inspections, add minimal `@SuppressWarnings("unused")` annotations to specific classes rather than adding marker beans.

