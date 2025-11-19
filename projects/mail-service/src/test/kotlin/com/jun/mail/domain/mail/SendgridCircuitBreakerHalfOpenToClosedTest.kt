package com.jun.mail.domain.mail

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class SendgridCircuitBreakerHalfOpenToClosedTest @Autowired constructor(
    private val sendgridMailService: SendgridMailService,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
) {

    private lateinit var cb: CircuitBreaker

    @BeforeEach
    fun setup() {
        cb = circuitBreakerRegistry.circuitBreaker("sendgrid")
        // ensure a clean start
        cb.transitionToClosedState()
    }

    @Test
    fun `half-open transitions to closed on success`() {
        // 1) Drive the circuit to OPEN with two failed calls (SendgridMailService throws)
        repeat(2) {
            try {
                sendgridMailService.sendMail(1L, "from@example.com", "to@example.com", "hello")
            } catch (_: Exception) {
                // expected failures
            }
        }
        assertThat(cb.state).isEqualTo(CircuitBreaker.State.OPEN)

        // 2) Wait until it automatically transitions to HALF_OPEN
        waitUntilState(cb, CircuitBreaker.State.HALF_OPEN, timeoutMs = 3000)
        assertThat(cb.state).isEqualTo(CircuitBreaker.State.HALF_OPEN)

        // 3) In HALF_OPEN, a single permitted successful call should transition to CLOSED
        // Use programmatic decoration to record a success without throwing
        cb.decorateRunnable { /* success - do nothing */ }.run()

        // 4) Verify state is CLOSED
        assertThat(cb.state).isEqualTo(CircuitBreaker.State.CLOSED)
    }

    private fun waitUntilState(cb: CircuitBreaker, target: CircuitBreaker.State, timeoutMs: Long = 3000) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (cb.state == target) return
            Thread.sleep(100)
        }
    }
}
