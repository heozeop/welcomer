package com.welcomer.welcome.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return health status with details`() {
        mockMvc.perform(get("/api/health")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.details.service").value("welcome-service"))
            .andExpect(jsonPath("$.details.version").value("0.0.1-SNAPSHOT"))
    }

    @Test
    fun `should return liveness status`() {
        mockMvc.perform(get("/api/health/live")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }

    @Test
    fun `should return readiness status with checks`() {
        mockMvc.perform(get("/api/health/ready")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.checks").exists())
            .andExpect(jsonPath("$.checks.service").value("welcome-service"))
            .andExpect(jsonPath("$.checks.version").value("0.0.1-SNAPSHOT"))
    }

    @Test
    fun `should return actuator health endpoint`() {
        mockMvc.perform(get("/actuator/health")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").exists())
    }

    @Test
    fun `should include database health in actuator endpoint`() {
        mockMvc.perform(get("/actuator/health")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.components.db").exists())
            .andExpect(jsonPath("$.components.db.status").value("UP"))
    }
}