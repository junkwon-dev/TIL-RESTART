package com.mailservice.service

import com.mailservice.domain.EmailServiceProvider
import com.mailservice.infrastructure.EmailServiceProviderRepository
import org.springframework.stereotype.Service

@Service
class ProviderService(private val repository: EmailServiceProviderRepository) {

    fun findAll(): List<EmailServiceProvider> = repository.findAll()

    fun save(provider: EmailServiceProvider): EmailServiceProvider {
        // Basic validation
        if (provider.ratio < 0 || provider.ratio > 100) {
            throw IllegalArgumentException("Ratio must be between 0 and 100.")
        }
        return repository.save(provider)
    }

    fun deleteById(id: Long) = repository.deleteById(id)

    fun validateRatios(): Boolean {
        val activeProviders = repository.findAll().filter { it.isActive }
        return activeProviders.sumOf { it.ratio } == 100
    }
}
