package com.jun.mail.infrastructure

import com.jun.mail.domain.entity.FeatureFlagConfig
import com.jun.mail.domain.entity.MailSentLog
import org.springframework.data.jpa.repository.JpaRepository

interface MailSentLogRepository: JpaRepository<MailSentLog, Long> {
    fun save(mailSentLog: MailSentLog): MailSentLog
}