package com.tradeoption.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tradeoption.service.RateLimitingService;
import com.tradeoption.config.RateLimitFilter;
import org.springframework.context.annotation.Import;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@Import({ RateLimitingService.class, RateLimitFilter.class })
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.tradeoption.repository.RocksDBRepository rocksDBRepository;

    @Test
    void shouldReturnUpStatus() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
