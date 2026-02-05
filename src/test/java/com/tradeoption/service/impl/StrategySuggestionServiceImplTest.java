package com.tradeoption.service.impl;

import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.SystemConfig;
import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.StrategyValidationService;
import com.tradeoption.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class StrategySuggestionServiceImplTest {

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private StrategyValidationService strategyValidationService;

    private StrategySuggestionServiceImpl strategySuggestionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        strategySuggestionService = new StrategySuggestionServiceImpl(marketDataService, systemConfigService,
                strategyValidationService);

        // Default valid spread
        when(strategyValidationService.isSpreadValid(anyString(), anyDouble(), anyString())).thenReturn(true);
        when(systemConfigService.getConfig()).thenReturn(new SystemConfig());
    }

    @Test
    public void testSuggestStraddle_Normal() {
        when(marketDataService.getLtp("NIFTY")).thenReturn(22020.0); // Rounds to 22000

        Strategy strategy = strategySuggestionService.suggestStraddle("NIFTY");

        assertNotNull(strategy);
        assertEquals("NIFTY", strategy.getSymbol());
        assertEquals(4, strategy.getLegs().size());

        // ATM strikes
        assertEquals(22000.0, strategy.getLegs().get(0).getStrikePrice());
        assertEquals(22000.0, strategy.getLegs().get(1).getStrikePrice());

        // Wings (+/- 500)
        assertEquals(22500.0, strategy.getLegs().get(2).getStrikePrice());
        assertEquals(21500.0, strategy.getLegs().get(3).getStrikePrice());
    }

    @Test
    public void testSuggestStraddle_BankNifty() {
        when(marketDataService.getLtp("BANKNIFTY")).thenReturn(48040.0); // Rounds to 48000

        Strategy strategy = strategySuggestionService.suggestStraddle("BANKNIFTY");

        assertNotNull(strategy);
        // ATM: 48000
        assertEquals(48000.0, strategy.getLegs().get(0).getStrikePrice());

        // Wings (+/- 1000 for BN)
        assertEquals(49000.0, strategy.getLegs().get(2).getStrikePrice());
        assertEquals(47000.0, strategy.getLegs().get(3).getStrikePrice());
    }

    @Test
    public void testSuggestStraddle_RetriesOnInvalidSpread() {
        when(marketDataService.getLtp("NIFTY")).thenReturn(22000.0);

        // 22000 Invalid
        when(strategyValidationService.isSpreadValid(anyString(), eq(22000.0), anyString())).thenReturn(false);
        // 22050 Valid (Next step up)
        when(strategyValidationService.isSpreadValid(anyString(), eq(22050.0), anyString())).thenReturn(true);

        Strategy strategy = strategySuggestionService.suggestStraddle("NIFTY");
        assertEquals(22050.0, strategy.getLegs().get(0).getStrikePrice());
    }

    @Test
    public void testSuggestStrangle() {
        when(marketDataService.getLtp("NIFTY")).thenReturn(22000.0);

        Strategy strategy = strategySuggestionService.suggestStrangle("NIFTY");

        assertNotNull(strategy);
        assertEquals(4, strategy.getLegs().size());

        // NIFTY Strangle Dist: 500
        // Sell Call: 22000 + 500 = 22500
        // Sell Put: 22000 - 500 = 21500
        assertEquals(22500.0, strategy.getLegs().get(0).getStrikePrice());
        assertEquals(21500.0, strategy.getLegs().get(1).getStrikePrice());

        // Wings: +200
        // Buy Call: 22500 + 200 = 22700
        // Buy Put: 21500 - 200 = 21300
        assertEquals(22700.0, strategy.getLegs().get(2).getStrikePrice());
        assertEquals(21300.0, strategy.getLegs().get(3).getStrikePrice());
    }

    @Test
    public void testSuggestStrangle_BankNifty() {
        when(marketDataService.getLtp("BANKNIFTY")).thenReturn(48000.0);

        Strategy strategy = strategySuggestionService.suggestStrangle("BANKNIFTY");

        // BN Strangle Dist: 1000
        // Sell Call: 49000
        // Sell Put: 47000
        assertEquals(49000.0, strategy.getLegs().get(0).getStrikePrice());
        assertEquals(47000.0, strategy.getLegs().get(1).getStrikePrice());
    }

    @Test
    public void testExpirySelection() {
        when(marketDataService.getLtp("NIFTY")).thenReturn(22000.0);

        SystemConfig config = new SystemConfig();
        Map<String, List<String>> expiries = new HashMap<>();
        expiries.put("NIFTY", Collections.singletonList("2024-04-25"));
        config.setSymbolExpiries(expiries);
        when(systemConfigService.getConfig()).thenReturn(config);

        Strategy strategy = strategySuggestionService.suggestStraddle("NIFTY");
        assertEquals("2024-04-25", strategy.getLegs().get(0).getExpiryDate());
    }

    @Test
    public void testFallbackWhenSpotIsZero() {
        when(marketDataService.getLtp("NIFTY")).thenReturn(0.0);

        Strategy strategy = strategySuggestionService.suggestStraddle("NIFTY");

        // Fallback spot 22000
        assertEquals(22000.0, strategy.getLegs().get(0).getStrikePrice());
    }
}
