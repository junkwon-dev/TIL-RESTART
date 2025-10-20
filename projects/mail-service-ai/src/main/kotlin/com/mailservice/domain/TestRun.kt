package com.mailservice.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.JoinColumn
import java.time.LocalDateTime

@Entity
data class TestSendRun(
    @Id @GeneratedValue
    var id: Long? = null,
    var timestamp: LocalDateTime = LocalDateTime.now(),
    var totalRequests: Int = 100,
    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "testSendRun")
    var distributionResults: MutableList<TestSendResult> = mutableListOf()
)

@Entity
data class TestSendResult(
    @Id @GeneratedValue
    var id: Long? = null,
    var serviceProviderName: String,
    var sentCount: Int,
    @ManyToOne
    @JoinColumn(name = "test_send_run_id")
    var testSendRun: TestSendRun? = null
)