package com.jun.mail.domain.command

data class SendMailCommand(
    val from: String,
    val to: String,
    val userId: Long,
    val content : String,
)