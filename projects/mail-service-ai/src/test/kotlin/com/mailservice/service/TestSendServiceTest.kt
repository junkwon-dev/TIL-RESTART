package com.mailservice.service

import com.mailservice.domain.EmailServiceProvider
import com.mailservice.domain.TestSendRun
import com.mailservice.domain.TestSendResult
import com.mailservice.infrastructure.EmailServiceProviderRepository
import com.mailservice.infrastructure.TestSendRunRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TestSendServiceTest {

    private val providerService: ProviderService = mockk()
    private val testSendRunRepository: TestSendRunRepository = mockk()
    private val testSendService = TestSendService(providerService, testSendRunRepository)

    @Test
    fun `should send test emails and save results`() {
        val provider1 = EmailServiceProvider(id = 1L, name = "SendGrid", ratio = 70, isActive = true)
        val provider2 = EmailServiceProvider(id = 2L, name = "Mailgun", ratio = 30, isActive = true)
        val providers = listOf(provider1, provider2)

        every { providerService.findAll() } returns providers
        every { testSendRunRepository.save(any<TestSendRun>()) } answers { firstArg() }

        testSendService.sendTestEmails()

        verify { providerService.findAll() }
        verify { testSendRunRepository.save(any<TestSendRun>()) }
    }

    @Test
    fun `should get latest test result`() {
        val testRun1 = TestSendRun(id = 1L, timestamp = LocalDateTime.now().minusHours(1))
        val testRun2 = TestSendRun(id = 2L, timestamp = LocalDateTime.now())
        val testRuns = listOf(testRun1, testRun2)

        every { testSendRunRepository.findAll() } returns testRuns

        val latestRun = testSendService.getLatestTestResult()

        assertEquals(testRun2, latestRun)
        verify { testSendRunRepository.findAll() }
    }
}
