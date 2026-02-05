package com.tradeoption.service.impl;

import com.tradeoption.domain.Strategy;
import com.tradeoption.domain.SystemConfig;
import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.StrategyValidationService;
import com.tradeoption.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    }

    @Test
    public void testSuggestStraddle_RetriesOnInvalidSpread() {
        // Mock Spot Price
        when(marketDataService.getLtp("NIFTY")).thenReturn(22000.0);

        // Mock System Config
        SystemConfig config = new SystemConfig();
        when(systemConfigService.getConfig()).thenReturn(config);

        // Valid ATM (22000) is INVALID due to spread
        when(strategyValidationService.isSpreadValid(anyString(), eq(22000.0), anyString())).thenReturn(false);

        // Next strike (22050 or 21950) is VALID
        when(strategyValidationService.isSpreadValid(anyString(), eq(22050.0), anyString())).thenReturn(true);
        when(strategyValidationService.isSpreadValid(anyString(), eq(21950.0), anyString())).thenReturn(true);

        Strategy strategy = strategySuggestionService.suggestStraddle("NIFTY");

        assertNotNull(strategy);
        assertEquals(4, strategy.getLegs().size());

        // Should have skipped 22000 and picked 22050 (since loop tries +step first)
        // Loop logic:
        // i=0: current=start (22000) -> invalid
        // i=1: (i%2!=0) -> start - step (21950) -> valid?
        // Wait, loop:
        // i=0: current=start (22000)
        // i=1: %2!=0 -> current = start - (0/2 + 1)*step = start - 50 = 21950

        // So if 21950 is valid, it should pick 21950.
        // Let's assert it's NOT 22000.

        double strike = strategy.getLegs().get(0).getStrikePrice();
        System.out.println("Selected Strike: " + strike);
        assertEquals(22050.0, strike, 0.01);
    }
}
