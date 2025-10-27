package com.jun.mail.domain.command

data class UpdateFeatureFlagConfigCommand(
    val options: List<String>,
    val isActive: Boolean
)