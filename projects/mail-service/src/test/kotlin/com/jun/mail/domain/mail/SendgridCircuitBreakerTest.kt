package com.jun.mail.domain.mail

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class SendgridCircuitBreakerTest @Autowired constructor(
    private val sendgridMailService: SendgridMailService,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
) {

    private lateinit var cb: CircuitBreaker

    @BeforeEach
    fun setup() {
        cb = circuitBreakerRegistry.circuitBreaker("sendgrid")
        // Ensure a clean state each test
        cb.transitionToClosedState()
    }

    @Test
    fun `circuit opens after consecutive failures and short-circuits subsequent calls`() {
        // First two calls fail due to SendgridMailService throwing RuntimeException
        repeat(2) {
            try {
                sendgridMailService.sendMail(1L, "from@example.com", "to@example.com", "hello")
            } catch (_: Exception) {
                // ignore
            }
        }

        // With test config: slidingWindowSize=2, minimumNumberOfCalls=2, failureRateThreshold=50
        // After two failures, failure rate is 100% -> circuit should be OPEN
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Next call should be short-circuited by CircuitBreaker
        assertThrows(CallNotPermittedException::class.java) {
            sendgridMailService.sendMail(2L, "from@example.com", "to@example.com", "world")
        }
    }
}
