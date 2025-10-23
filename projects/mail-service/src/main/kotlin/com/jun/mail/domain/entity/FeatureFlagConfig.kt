package com.jun.mail.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "feature_flag_config")
class FeatureFlagConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "feature", columnDefinition = "varchar(300)", unique = true)
    var feature: String,

    @Column(name = "options", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter::class)
    var options: List<String>,

    @Column(name = "is_active", columnDefinition = "BOOLEAN")
    var isActive: Boolean = false,
){
    fun toggle(){
        isActive = !isActive
    }
}