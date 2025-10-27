package com.jun.mail.infrastructure

import com.jun.mail.domain.entity.FeatureFlagConfig
import org.springframework.data.jpa.repository.JpaRepository

interface FeatureFlagConfigRepository: JpaRepository<FeatureFlagConfig, Long> {
    fun findFeatureFlagConfigByFeatureAndIsActive(feature: String, isActive: Boolean): FeatureFlagConfig?
    fun save(featureFlagConfig: FeatureFlagConfig): FeatureFlagConfig
}