package com.jun.mail.domain.mail

import com.jun.mail.domain.entity.MailSentLog
import com.jun.mail.domain.entity.MailServiceType
import com.jun.mail.infrastructure.MailSentLogRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.stereotype.Service

@Service("sendgrid")
class SendgridMailService(
    private val mailSentLogRepository: MailSentLogRepository
): MailService{
    @CircuitBreaker(name = "sendgrid")
    override fun sendMail(userId: Long, from: String, to: String, content: String) {
        throw RuntimeException("Disastered")
        mailSentLogRepository.save(
            MailSentLog(
                id = null,
                mailService = MailServiceType.SENDGRID
            )
        )
        return
    }
}