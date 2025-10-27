package com.jun.mail.domain.mail

import com.jun.mail.domain.command.SendMailCommand
import com.jun.mail.domain.entity.MailServiceType
import com.jun.mail.infrastructure.MailSentLogRepository
import org.springframework.stereotype.Service

@Service
class MailLogService(
    private val mailSentLogRepository: MailSentLogRepository
){
    fun getStatistics(): String{
        val logs = mailSentLogRepository.findAll()
        val statistics: MutableMap<MailServiceType, Int> = mutableMapOf()
        logs.forEach {
            if(statistics[it.mailService] == null){
                statistics[it.mailService] = 0
            }
            statistics[it.mailService] = statistics[it.mailService]!! + 1
        }
        return statistics.toString()
    }
}