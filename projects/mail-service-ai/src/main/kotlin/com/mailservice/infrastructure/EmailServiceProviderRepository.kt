package com.mailservice.infrastructure

import com.mailservice.domain.EmailServiceProvider
import org.springframework.data.jpa.repository.JpaRepository

interface EmailServiceProviderRepository : JpaRepository<EmailServiceProvider, Long>
