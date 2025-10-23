package com.jun.mail.domain.mail

interface MailService{
    fun sendMail(userId: Long, from: String, to: String, content: String)
}