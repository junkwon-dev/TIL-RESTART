package com.jun.mail.application

import com.jun.mail.domain.command.SendMailCommand
import com.jun.mail.domain.entity.MailSentLog
import com.jun.mail.domain.mail.MailService
import com.jun.mail.infrastructure.FeatureFlagConfigRepository
import com.jun.mail.infrastructure.MailSentLogRepository
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.retry.support.RetrySynchronizationManager
import org.springframework.stereotype.Component

@Component
class MailingApplication(
    private val featureFlagConfigRepository: FeatureFlagConfigRepository,
    private val mailServices: Map<String, MailService>,
) {
    private val log = LoggerFactory.getLogger(MailingApplication::class.java)
    // 알아낸 사실.. Spring 7에서 기본 제공하는 Resilience 에는 Context를 얻을 있는 기능이 없다.
    @Retryable(maxAttempts = 5, backoff = Backoff(delay = 0))
    fun sendMail(sendMailCommand: SendMailCommand){
        // feature flag 기능
        val featureFlagConfig = featureFlagConfigRepository.findFeatureFlagConfigByFeatureAndIsActive(
            feature = "MAIL_SERVICE",
            isActive = true
        )

        val retryContext = RetrySynchronizationManager.getContext()
        val retryCount = retryContext?.retryCount ?: 0
        if(retryCount>0){
            mailServices.values.forEach {
                try {
                    it.sendMail(
                        userId = sendMailCommand.userId,
                        from = sendMailCommand.from,
                        to = sendMailCommand.to,
                        content = sendMailCommand.content,
                    )
                    return
                } catch (e: Exception){
                    log.error("sendMail retry path failed", e)
                }
            }
        }

        if(featureFlagConfig != null){
            // userId를 기반으로 나머지 연산
            val slot = ((sendMailCommand.userId) % featureFlagConfig.options.size).toInt()

            // ["sendgrid", "sendgrid", "mailgun", "directsend"]
            val key = featureFlagConfig.options[slot]

            mailServices[key]?.sendMail(
                userId = sendMailCommand.userId,
                from = sendMailCommand.from,
                to = sendMailCommand.to,
                content = sendMailCommand.content,
            )
        }else{
            mailServices.values.forEach {
                try {
                    it.sendMail(
                        userId = sendMailCommand.userId,
                        from = sendMailCommand.from,
                        to = sendMailCommand.to,
                        content = sendMailCommand.content,
                    )
                    return
                } catch (e: Exception) {
                    log.error("sendMail fallback path failed", e)
                }
            }
        }
    }

    @Retryable(maxAttempts = 5)
    fun receiveMail(sendMailCommand: SendMailCommand){
        // feature flag 기능
        val featureFlagConfig = featureFlagConfigRepository.findFeatureFlagConfigByFeatureAndIsActive(
            feature = "RECEIVE_MAIL_SERVICE",
            isActive = true
        )

        val retryContext = RetrySynchronizationManager.getContext()
        val retryCount = retryContext?.retryCount ?: 0

        if(retryCount>0){
            mailServices.values.forEach {
                try {
                    it.sendMail(
                        userId = sendMailCommand.userId,
                        from = sendMailCommand.from,
                        to = sendMailCommand.to,
                        content = sendMailCommand.content,
                    )
                    return
                } catch (e: Exception) {
                    log.error("receiveMail retry path failed", e)
                }
            }
        }

        if(featureFlagConfig != null){
            // userId를 기반으로 나머지 연산
            val slot = ((sendMailCommand.userId) % featureFlagConfig.options.size).toInt()

            // ["sendgrid", "sendgrid", "mailgun", "directsend"]
            val key = featureFlagConfig.options[slot]

            mailServices[key]?.sendMail(
                userId = sendMailCommand.userId,
                from = sendMailCommand.from,
                to = sendMailCommand.to,
                content = sendMailCommand.content,
            )
        }else{
            mailServices.values.forEach {
                try {
                    it.sendMail(
                        userId = sendMailCommand.userId,
                        from = sendMailCommand.from,
                        to = sendMailCommand.to,
                        content = sendMailCommand.content,
                    )
                    return
                } catch (e: Exception) {
                    log.error("receiveMail fallback path failed", e)
                }
            }
        }
    }
}