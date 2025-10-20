package com.mailservice.service

import com.mailservice.domain.TestSendRun
import com.mailservice.domain.TestSendResult
import com.mailservice.infrastructure.TestSendRunRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TestSendService(private val providerService: ProviderService, private val testSendRunRepository: TestSendRunRepository) {

    // Removed @Async temporarily for debugging
    @Transactional
    fun sendTestEmails() {
        val providers = providerService.findAll().filter { it.isActive }
        val totalRatio = providers.sumOf { it.ratio }
        if (totalRatio != 100) {
            throw IllegalStateException("Ratios must sum to 100")
        }

        val testRun = TestSendRun()
        val results = mutableListOf<TestSendResult>()

        for (provider in providers) {
            val count = (100 * provider.ratio) / 100
            // Here you would integrate with the actual email sending logic
            // For now, we just simulate it
            results.add(TestSendResult(serviceProviderName = provider.name, sentCount = count, testSendRun = testRun))
        }
        testRun.distributionResults = results
        testSendRunRepository.save(testRun)
    }

    fun getLatestTestResult(): TestSendRun? {
        return testSendRunRepository.findAll().maxByOrNull { it.timestamp }
    }
}
