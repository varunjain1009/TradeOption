package com.tradeoption.controller;

import com.tradeoption.domain.*;
import com.tradeoption.service.*;
import com.tradeoption.repository.PositionRepository;
import com.tradeoption.scheduler.DashboardBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tradeoption.service.RateLimitingService;
import com.tradeoption.config.RateLimitFilter;
import org.springframework.context.annotation.Import;

@WebMvcTest(StrategyController.class)
@WithMockUser(username = "testuser")
@Import({ RateLimitingService.class, RateLimitFilter.class })
public class StrategyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PositionRepository positionRepository;

    @MockBean
    private MarketDataService marketDataService;

    @MockBean
    private StrategySuggestionService strategySuggestionService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private DashboardBroadcaster dashboardBroadcaster;

    @MockBean
    private SystemConfigService systemConfigService;

    @MockBean
    private StrategyService strategyService;

    // Mock user details service required for SecurityConfig even if mocked user is
    // used
    @MockBean
    private com.tradeoption.service.CustomUserDetailsService customUserDetailsService;

    @Test
    public void testGetPositions() throws Exception {
        Position p = new Position("NIFTY", "28MAR2024", 22000.0, LegType.CE);
        when(positionRepository.findAll()).thenReturn(Collections.singletonList(p));

        mockMvc.perform(get("/api/strategy/positions")
                .with(csrf())) // CSRF might be required for POST, handled by security config but safe to add
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("NIFTY"));
    }

    @Test
    public void testGetPositionHistory() throws Exception {
        Position p = new Position("NIFTY", "28MAR2024", 22000.0, LegType.CE);
        when(positionRepository.findById("pos-1")).thenReturn(p);

        mockMvc.perform(get("/api/strategy/position/pos-1/history"))
                .andExpect(status().isOk());
    }

    @Test
    public void testPlaceTrade() throws Exception {
        Position p = new Position("NIFTY", "28MAR2024", 22000.0, LegType.CE);
        when(positionRepository.findById(anyString())).thenReturn(p);

        String json = "{\"symbol\":\"NIFTY\", \"expiryDate\":\"28MAR2024\", \"strikePrice\":22000.0, \"optionType\":\"CE\", \"tradeAction\":\"BUY\", \"price\":150.0, \"quantity\":50}";

        mockMvc.perform(post("/api/strategy/trade")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("Trade processed successfully"));
    }

    @Test
    public void testSuggestStraddle() throws Exception {
        Strategy strat = new Strategy();
        strat.setId("suggested-straddle");
        when(strategySuggestionService.suggestStraddle("NIFTY")).thenReturn(strat);

        mockMvc.perform(get("/api/strategy/suggest/straddle?symbol=NIFTY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("suggested-straddle"));
    }

    @Test
    public void testSuggestStrangle() throws Exception {
        Strategy strat = new Strategy();
        strat.setId("suggested-strangle");
        when(strategySuggestionService.suggestStrangle("NIFTY")).thenReturn(strat);

        mockMvc.perform(get("/api/strategy/suggest/strangle?symbol=NIFTY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("suggested-strangle"));
    }

    @Test
    public void testUpdateExit() throws Exception {
        Position p = new Position("NIFTY", "28MAR2024", 22000.0, LegType.CE);
        when(positionRepository.findById("pos-1")).thenReturn(p);

        mockMvc.perform(post("/api/strategy/position/pos-1/exit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\": 100.0, \"quantity\": 50}"))
                .andExpect(status().isOk());
    }
}
