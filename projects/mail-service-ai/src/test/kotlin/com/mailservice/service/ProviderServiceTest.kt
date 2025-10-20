package com.mailservice.service

import com.mailservice.domain.EmailServiceProvider
import com.mailservice.infrastructure.EmailServiceProviderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProviderServiceTest {

    private val repository: EmailServiceProviderRepository = mockk()
    private val service = ProviderService(repository)

    @Test
    fun `should save provider`() {
        val provider = EmailServiceProvider(name = "SendGrid", ratio = 100)
        every { repository.save(provider) } returns provider

        val result = service.save(provider)

        assertEquals(provider, result)
        verify { repository.save(provider) }
    }
}
