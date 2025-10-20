package com.mailservice.controller

import com.mailservice.domain.EmailServiceProvider
import com.mailservice.service.ProviderService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

import com.mailservice.service.TestSendService

@Controller
@RequestMapping("/admin")
class AdminController(private val providerService: ProviderService, private val testSendService: TestSendService) {

    @GetMapping("/providers")
    fun getProviders(model: Model): String {
        model.addAttribute("providers", providerService.findAll())
        model.addAttribute("newProvider", EmailServiceProvider(name = "", ratio = 0))
        return "admin/providers"
    }

    @PostMapping("/providers")
    fun addProvider(provider: EmailServiceProvider): String {
        providerService.save(provider)
        return "redirect:/admin/providers"
    }

    @PostMapping("/test-send")
    fun testSend(): String {
        return "redirect:/admin/test-results"
    }

    @GetMapping("/test-results")
    fun getTestResults(model: Model): String {
        val latestRun = testSendService.getLatestTestResult()
        model.addAttribute("latestRun", latestRun)
        return "admin/results"
    }
}