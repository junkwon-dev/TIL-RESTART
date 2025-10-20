# Data Model: Request Distribution Test

## 1. EmailServiceProvider

Represents a third-party email service provider that can be used to send emails.

**Fields**:

-   `id`: `Long` (Primary Key, Auto-generated)
-   `name`: `String` (Unique, Not Null) - The name of the service provider (e.g., "SendGrid").
-   `ratio`: `Int` (Not Null) - The integer percentage of requests to send through this service.
-   `is_active`: `Boolean` (Not Null, Default: `true`) - Whether the service is currently active in the distribution.

**Validation Rules**:

-   `name` must not be blank.
-   `ratio` must be between 0 and 100 (inclusive).
-   The sum of `ratio` for all `active` providers must equal 100.

## 2. TestSendRun

Represents a single test run of sending 100 emails.

**Fields**:

-   `id`: `Long` (Primary Key, Auto-generated)
-   `timestamp`: `LocalDateTime` (Not Null) - The date and time the test was initiated.
-   `total_requests`: `Int` (Not Null, Always 100) - The total number of emails sent.
-   `distribution_results`: `List<TestSendResult>` (One-to-Many relationship) - The results of the distribution.

## 3. TestSendResult

Represents the result for a single service provider within a `TestSendRun`.

**Fields**:

-   `id`: `Long` (Primary Key, Auto-generated)
-   `service_provider_name`: `String` (Not Null) - The name of the service provider.
-   `sent_count`: `Int` (Not Null) - The number of emails sent through this provider.
-   `test_send_run`: `TestSendRun` (Many-to-One relationship)
