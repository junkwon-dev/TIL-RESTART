package com.jun.mail.domain.mail

import com.jun.mail.domain.entity.MailSentLog
import com.jun.mail.domain.entity.MailServiceType
import com.jun.mail.infrastructure.MailSentLogRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.stereotype.Service

@Service("directSend")
class DirectSendMailService(
    private val mailSentLogRepository: MailSentLogRepository
): MailService{
    @CircuitBreaker(name = "directSend")
    override fun sendMail(userId: Long, from: String, to: String, content: String) {
        mailSentLogRepository.save(
            MailSentLog(
                id = null,
                mailService = MailServiceType.DIRECT_SEND
            )
        )
        return
    }
}