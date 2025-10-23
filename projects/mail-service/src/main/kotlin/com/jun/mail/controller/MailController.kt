package com.jun.mail.controller

import com.jun.mail.application.MailingApplication
import com.jun.mail.domain.command.SendMailCommand
import com.jun.mail.domain.mail.MailLogService
import com.jun.mail.domain.mail.MailService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("mail")
class MailController(
    private val mailingApplication: MailingApplication,
    private val mailLogService: MailLogService
) {
    @PostMapping
    fun sendMail(
        @RequestBody sendMailCommand: SendMailCommand
    ){
        return mailingApplication.sendMail(sendMailCommand)
    }

    @PostMapping("/bulk")
    fun sendBulkMail(
    ){
        repeat((1..1000).count()) {
            mailingApplication.sendMail(SendMailCommand(
                from = "test$it@example.com",
                to = "testReceiver$it@example.com",
                userId = it.toLong(),
                content = "TEST"
            ))
        }
        return
    }

    @GetMapping
    fun getSentMailLog(): String{
        return mailLogService.getStatistics()
    }
}