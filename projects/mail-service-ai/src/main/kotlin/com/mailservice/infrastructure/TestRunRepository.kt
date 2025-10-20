package com.mailservice.infrastructure

import com.mailservice.domain.TestSendRun
import org.springframework.data.jpa.repository.JpaRepository

interface TestSendRunRepository : JpaRepository<TestSendRun, Long>
