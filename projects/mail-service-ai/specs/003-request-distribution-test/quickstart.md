# Quickstart: Request Distribution Test

This guide explains how to build and run the application.

## Prerequisites

-   Java 21 or later
-   Gradle 8.5 or later

## Build

To build the application, run the following command from the root of the project:

```bash
./gradlew build
```

## Run

To run the application, use the following command:

```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`.

## Admin Interface

The admin interface is available at `http://localhost:8080/admin`. You can use this interface to:

-   Configure service provider ratios
-   Trigger a test send of 100 emails
-   View the results of the test send
