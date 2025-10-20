package com.mailservice.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.* 

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should get providers page`() {
        mockMvc.perform(get("/admin/providers").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk)
            .andExpect(view().name("admin/providers"))
    }

    @Test
    fun `should add provider`() {
        mockMvc.perform(post("/admin/providers").with(user("admin").roles("ADMIN"))
            .param("name", "TestGrid")
            .param("ratio", "100")
            .with(csrf()))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin/providers"))
    }
}
