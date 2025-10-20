# Feature Specification: Email Service Request Ratio Splitting

**Feature Branch**: `002-email-service-ratio`
**Created**: 2025-10-20
**Status**: Draft
**Input**: User description: "이메일 서비스에는 센드그리드, 메일건 등등이 있어. 이 서비스에 요청 비율에 따라 요청을 나눌 수 있는 기능을 만들거야. 그 이유는 특정 서비스 장애시 요청을 다른 서비스를 활용하여 리스크를 분산시킬 수 있고, 특정 기능이 한쪽에만 있다면 그 기능을 쓰기 위함이고, 마지막으로 서비스마다 사용료가 달라서 어떤 기능은 센드그리드, 어떤 기능은 메일건을 활용할 수 있기 때문이야."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configure Service Ratios (Priority: P1)

As an administrator, I want to define the percentage-based traffic distribution across multiple email service providers so that I can control costs, mitigate risks, and leverage provider-specific features.

**Why this priority**: This is the foundational step; without a configured ratio, the core feature is unusable.

**Independent Test**: An admin can log in, navigate to the service configuration page, set ratios for at least two providers that sum to 100%, and see a success message.

**Acceptance Scenarios**:

1.  **Given** I am on the "Service Provider Configuration" page, **When** I set SendGrid to 70% and Mailgun to 30% and click "Save", **Then** the system confirms that the new ratio is saved.
2.  **Given** I am on the "Service Provider Configuration" page, **When** I try to set the ratios to 50% for SendGrid and 40% for Mailgun and click "Save", **Then** the system shows an error message stating that the ratios must sum to 100%.

---

### User Story 2 - Trigger Test Send (Priority: P2)

As an administrator, I want to trigger a batch of 100 test emails to be sent, so I can validate that the request-splitting logic is working correctly in a real-world scenario.

**Why this priority**: This allows for immediate, on-demand validation of the configuration set in User Story 1.

**Independent Test**: After a ratio is configured, the admin can click a "Send Test" button and receive confirmation that 100 emails have been queued for sending.

**Acceptance Scenarios**:

1.  **Given** a valid ratio is configured, **When** I click the "Send 100 Test Emails" button, **Then** a confirmation message appears, and the system begins dispatching emails in the background.

---

### User Story 3 - Verify Distribution Results (Priority: P3)

As an administrator, I want to view a report that shows the actual number of emails sent through each service provider after a test run, so I can confirm that the distribution matches the configured ratio.

**Why this priority**: This provides the necessary feedback to confirm the feature is working as expected and builds trust in the system.

**Independent Test**: After a test send is complete, the admin can navigate to a "Test Results" page and see a breakdown of the email distribution for the most recent test.

**Acceptance Scenarios**:

1.  **Given** a test send has completed with a 70/30 ratio, **When** I navigate to the "Test Results" page, **Then** I see a report showing approximately 70 emails sent via SendGrid and 30 emails sent via Mailgun.

---

### Edge Cases

-   **Service Unavailability**: If a service provider is down during a test send, the system should send the email through the next available service provider in the list.
-   **Invalid Configuration**: The system must prevent a test from running if the ratios do not sum to 100% or if no services are active.
-   **Zero Ratio**: If a service is configured with a 0% ratio, it MUST NOT receive any emails.

## Requirements *(mandatory)*

### Functional Requirements

-   **FR-001**: The system MUST provide a UI to define percentage-based ratios for multiple email service providers.
-   **FR-002**: The sum of the ratios for all active providers MUST equal 100%. The UI must enforce this rule.
-   **FR-003**: The system MUST provide a single-action button to trigger the sending of exactly 100 test emails.
-   **FR-004**: The system MUST distribute the 100 emails across the active service providers based on the configured integer ratios.
-   **FR-005**: After a test run, the system MUST display a results page showing the actual count of emails sent through each provider.
-   **FR-006**: The results page MUST be linked from the main configuration page or be easily discoverable after a test run.

### Key Entities *(include if feature involves data)*

-   **Email Service Provider**:
    -   `name`: The unique name of the service provider (e.g., "SendGrid").
    -   `ratio`: The integer percentage (0-100) of requests to be sent.
    -   `is_active`: A boolean to include the service in the distribution.
-   **Test Send Run**:
    -   `run_id`: A unique identifier for the test.
    -   `timestamp`: The date and time the test was initiated.
    -   `total_requests`: The number of emails sent (always 100).
    -   `distribution_results`: A list of objects, each containing a service provider name and the count of emails sent.

## Success Criteria *(mandatory)*

### Measurable Outcomes

-   **SC-001**: An administrator can successfully configure a new ratio distribution and save it in under 1 minute.
-   **SC-002**: The system dispatches all 100 test emails within 60 seconds of the "Send Test" button being clicked.
-   **SC-003**: The final distribution of emails on the results page matches the configured ratio with a maximum deviation of ±2 emails for any single provider, ensuring the total is exactly 100.
-   **SC-004**: 95% of administrators can successfully configure, run, and verify a test on their first attempt without consulting documentation.
