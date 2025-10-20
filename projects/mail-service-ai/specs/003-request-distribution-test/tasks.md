# Tasks: Request Distribution Test

**Input**: Design documents from `specs/003-request-distribution-test/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create the Spring Boot project structure with Gradle using `spring initializr`.
- [X] T002 Add required dependencies to `build.gradle.kts`: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-security`, `h2`, `jackson-module-kotlin`, and test dependencies.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

- [X] T003 Configure the H2 in-memory database in `src/main/resources/application.properties`.
- [X] T004 [P] Set up basic Spring Security with an in-memory user for the admin section in a `SecurityConfig.kt` file.
- [X] T005 [P] Create a main layout template `layout.html` in `src/main/resources/templates/` using Thymeleaf for a consistent admin UI.

---

## Phase 3: User Story 1 - Configure Service Ratios (Priority: P1) 🎯 MVP

**Goal**: Allow an admin to configure the email distribution ratio.

**Independent Test**: An admin can navigate to the `/admin/providers` page, add/edit/delete service providers, and the form validation for ratios summing to 100% works correctly.

### Tests for User Story 1

- [X] T006 [P] [US1] Write unit tests for `ProviderService` in `src/test/kotlin/com/mailservice/service/ProviderServiceTest.kt`.
- [X] T007 [P] [US1] Write integration tests for the `AdminController` endpoints related to provider management in `src/test/kotlin/com/mailservice/controller/AdminControllerIntegrationTest.kt`.

### Implementation for User Story 1

- [X] T008 [P] [US1] Create the `EmailServiceProvider` data class in `src/main/kotlin/com/mailservice/domain/EmailServiceProvider.kt`.
- [X] T009 [P] [US1] Create the `EmailServiceProviderRepository` interface in `src/main/kotlin/com/mailservice/infrastructure/EmailServiceProviderRepository.kt`.
- [X] T010 [US1] Implement the `ProviderService` in `src/main/kotlin/com/mailservice/service/ProviderService.kt` to handle CRUD operations and ratio validation.
- [X] T011 [US1] Implement the `AdminController` in `src/main/kotlin/com/mailservice/controller/AdminController.kt` with GET and POST mappings for `/admin/providers`.
- [X] T012 [US1] Create the Thymeleaf template `providers.html` in `src/main/resources/templates/admin/` to list, add, and edit providers.

---

## Phase 4: User Story 2 - Trigger Test Send (Priority: P2)

**Goal**: Allow an admin to trigger a test send of 100 emails.

**Independent Test**: An admin can click a "Send Test" button on the providers page, and the system will start sending 100 emails in the background according to the configured ratio.

### Tests for User Story 2

- [X] T013 [P] [US2] Write unit tests for `TestSendService` in `src/test/kotlin/com/mailservice/service/TestSendServiceTest.kt`.

### Implementation for User Story 2

- [X] T014 [P] [US2] Create the `TestSendRun` and `TestSendResult` data classes in `src/main/kotlin/com/mailservice/domain/TestRun.kt`.
- [X] T015 [P] [US2] Create repositories for `TestSendRun` and `TestSendResult` in `src/main/kotlin/com/mailservice/infrastructure/TestRunRepository.kt`.
- [X] T016 [US2] Implement the `TestSendService` in `src/main/kotlin/com/mailservice/service/TestSendService.kt` with an async method to send emails.
- [X] T017 [US2] Add the POST mapping for `/admin/test-send` to `AdminController.kt`.
- [X] T018 [US2] Add a "Send Test" button to the `providers.html` template.

---

## Phase 5: User Story 3 - Verify Distribution Results (Priority: P3)

**Goal**: Allow an admin to see the results of a test send.

**Independent Test**: After a test send, an admin can navigate to a results page and see the correct distribution of the 100 emails.

### Tests for User Story 3

- [X] T019 [P] [US3] Add unit tests to `TestSendServiceTest.kt` for the result retrieval logic.

### Implementation for User Story 3

- [X] T020 [US3] Update `TestSendService.kt` to save the results of the test run to the database.
- [X] T021 [US3] Add a GET mapping to `AdminController.kt` for `/admin/test-results`.
- [X] T022 [US3] Create the Thymeleaf template `results.html` in `src/main/resources/templates/admin/` to display the test results.

---

## Phase N: Polish & Cross-Cutting Concerns

- [X] T023 [P] Add global error handling using `@ControllerAdvice`.
- [X] T024 [P] Configure structured logging with SLF4J and Logback.
- [X] T025 Refine the UI/UX of the admin pages.
- [X] T026 Write comprehensive Javadoc/KDoc for all public methods and classes.
