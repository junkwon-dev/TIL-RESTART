package com.jun.mail.application

import com.jun.mail.domain.command.SendMailCommand
import com.jun.mail.domain.entity.FeatureFlagConfig
import com.jun.mail.domain.entity.MailServiceType
import com.jun.mail.infrastructure.FeatureFlagConfigRepository
import com.jun.mail.infrastructure.MailSentLogRepository
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MailingApplicationCircuitBreakerFailoverTest @Autowired constructor(
    private val mailingApplication: MailingApplication,
    private val featureFlagConfigRepository: FeatureFlagConfigRepository,
    private val mailSentLogRepository: MailSentLogRepository,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
) {
    private lateinit var sendgridCB: CircuitBreaker

    @BeforeEach
    fun setup() {
        // Clean DB
        mailSentLogRepository.deleteAll()
        featureFlagConfigRepository.deleteAll()

        // Ensure Sendgrid CB is OPEN to simulate outage
        sendgridCB = circuitBreakerRegistry.circuitBreaker("sendgrid")
        sendgridCB.transitionToOpenState()

        // Configure Feature Flag to route first attempt to sendgrid so that retry path executes
        featureFlagConfigRepository.save(
            FeatureFlagConfig(
                id = null,
                feature = "MAIL_SERVICE",
                options = listOf("sendgrid", "mailgun", "directSend"),
                isActive = true
            )
        )
    }

    @Test
    fun `when primary provider circuit is OPEN then application fails over to another provider`() {
        val cmd = SendMailCommand(
            userId = 0L, // maps to index 0 => "sendgrid"
            from = "from@example.com",
            to = "to@example.com",
            content = "hello"
        )

        // Act: first call will be short-circuited by sendgrid CB; retry path should try others
        try {
            mailingApplication.sendMail(cmd)
        } catch (_: Exception) {
            // MailingApplication swallows exceptions in retry/fallback loops; shouldn't throw, but ignore if it does
        }

        val logs = mailSentLogRepository.findAll()
        // Expect exactly one successful send via a non-sendgrid provider
        assertThat(logs).hasSize(1)
        assertThat(logs[0].mailService).isIn(MailServiceType.MAILGUN, MailServiceType.DIRECT_SEND)
    }
}
