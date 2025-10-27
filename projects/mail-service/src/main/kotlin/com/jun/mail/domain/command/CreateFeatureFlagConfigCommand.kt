package com.jun.mail.domain.command

data class CreateFeatureFlagConfigCommand(
    val feature: String,
    val options: List<String>,
    val isActive: Boolean
)