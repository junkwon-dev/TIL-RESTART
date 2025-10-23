package com.jun.mail.application

import com.jun.mail.domain.command.SendMailCommand
import com.jun.mail.domain.entity.MailSentLog
import com.jun.mail.domain.mail.MailService
import com.jun.mail.infrastructure.FeatureFlagConfigRepository
import com.jun.mail.infrastructure.MailSentLogRepository
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component

@Component
class MailingApplication(
    private val featureFlagConfigRepository: FeatureFlagConfigRepository,
    private val mailServices: Map<String, MailService>,
) {
    @Retryable
    fun sendMail(sendMailCommand: SendMailCommand){
        // feature flag 기능
        val featureFlagConfig = featureFlagConfigRepository.findFeatureFlagConfigByFeatureAndIsActive(
            feature = "MAIL_SERVICE",
            isActive = true
        )

        // userId를 기반으로 나머지 연산
        val slot = (sendMailCommand.userId % featureFlagConfig.options.size).toInt()

        // ["sendgrid", "sendgrid", "mailgun", "directsend"]
        val key = featureFlagConfig.options[slot]

        mailServices[key]?.sendMail(
            userId = sendMailCommand.userId,
            from = sendMailCommand.from,
            to = sendMailCommand.to,
            content = sendMailCommand.content,
        )
    }
}