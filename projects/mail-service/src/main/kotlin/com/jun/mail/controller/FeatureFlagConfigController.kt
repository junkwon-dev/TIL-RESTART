package com.jun.mail.controller

import com.jun.mail.domain.command.CreateFeatureFlagConfigCommand
import com.jun.mail.domain.command.UpdateFeatureFlagConfigCommand
import com.jun.mail.domain.entity.FeatureFlagConfig
import com.jun.mail.domain.featureFlagConfig.FeatureFlagConfigService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("feature-flag-config")
class FeatureFlagConfigController(
    private val featureFlagConfigService: FeatureFlagConfigService
) {
    @PostMapping
    fun createConfig(@RequestBody config: CreateFeatureFlagConfigCommand): FeatureFlagConfig {
        return featureFlagConfigService.createFeatureFlagConfig(command = config)
    }

    @PutMapping("/{featureFlagConfigId}")
    fun updateConfig(
        @PathVariable("featureFlagConfigId") featureFlagConfigId: Long,
        @RequestBody config: UpdateFeatureFlagConfigCommand): FeatureFlagConfig {
        return featureFlagConfigService.updateFeatureFlagConfig(id= featureFlagConfigId, command = config)
    }

    @PutMapping("/{featureFlagConfigId}/toggle")
    fun toggleConfig(
        @PathVariable("featureFlagConfigId") featureFlagConfigId: Long,
    ): FeatureFlagConfig{
        return featureFlagConfigService.toggleFeatureFlagConfig(featureFlagConfigId)
    }

    @GetMapping
    fun getFeatureFlagConfigs(): Set<FeatureFlagConfig> {
        return featureFlagConfigService.getFeatureFlagConfigs()
    }

    @DeleteMapping("/{featureFlagConfigId}")
    fun deleteConfig(
        @PathVariable("featureFlagConfigId") featureFlagConfigId: Long,
    ) {
        return featureFlagConfigService.deleteFeatureFlagConfig(featureFlagConfigId)
    }
}