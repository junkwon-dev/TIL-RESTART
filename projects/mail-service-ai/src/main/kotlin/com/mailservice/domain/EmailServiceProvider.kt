package com.mailservice.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
data class EmailServiceProvider(
    @Id @GeneratedValue
    var id: Long? = null,
    var name: String,
    var ratio: Int,
    var isActive: Boolean = true
)
