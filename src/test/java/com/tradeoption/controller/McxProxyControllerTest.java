package com.tradeoption.controller;

import com.tradeoption.service.McxApiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class McxProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private McxApiService mcxApiService;

    @Test
    public void testGetOptionChainEndpoint() throws Exception {
        String mockResponse = "{\"status\": \"ok\"}";
        Mockito.when(mcxApiService.getOptionChain(anyString(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/mcx/option-chain")
                .param("commodity", "SILVER")
                .param("expiry", "28NOV2024"))
                .andExpect(status().isOk())
                .andExpect(content().string(mockResponse));
    }
}
