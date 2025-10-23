package com.jun.mail.domain.featureFlagConfig

import com.jun.mail.domain.command.CreateFeatureFlagConfigCommand
import com.jun.mail.domain.entity.FeatureFlagConfig
import com.jun.mail.infrastructure.FeatureFlagConfigRepository
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class FeatureFlagConfigService(
    val featureFlagConfigRepository: FeatureFlagConfigRepository
) {
    fun createFeatureFlagConfig(command: CreateFeatureFlagConfigCommand): FeatureFlagConfig {
        return featureFlagConfigRepository.save(
            FeatureFlagConfig(
                id = null,
                feature = command.feature,
                options = command.options,
                isActive = command.isActive
            )
        )
    }

    fun toggleFeatureFlagConfig(id: Long): FeatureFlagConfig {
        val featureFlagConfig = featureFlagConfigRepository.findById(id).orElseThrow()
        featureFlagConfig.toggle()
        return featureFlagConfig
    }

    fun getFeatureFlagConfigs(): Set<FeatureFlagConfig> {
        return featureFlagConfigRepository.findAll().toSet()
    }

    fun deleteFeatureFlagConfig(id: Long) {
        featureFlagConfigRepository.deleteById(id)
    }
}