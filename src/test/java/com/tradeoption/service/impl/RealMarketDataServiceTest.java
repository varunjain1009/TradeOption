package com.tradeoption.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeoption.domain.SystemConfig;
import com.tradeoption.service.McxApiService;
import com.tradeoption.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RealMarketDataServiceTest {

    @Mock
    private McxApiService mcxApiService;

    @Mock
    private SystemConfigService systemConfigService;

    private RealMarketDataService realMarketDataService;
    private SystemConfig systemConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        systemConfig = new SystemConfig();
        when(systemConfigService.getConfig()).thenReturn(systemConfig);
        realMarketDataService = new RealMarketDataService(mcxApiService, new ObjectMapper(), systemConfigService);
    }

    @Test
    void getLtp_ApiFails_FallbackDisabled_ReturnsEmpty() {
        // Arrange
        systemConfig.setEnableMockFallback(false);
        when(mcxApiService.getOptionChain(anyString(), anyString())).thenReturn(null);

        // Act
        java.util.Optional<Double> ltpStart = realMarketDataService.getLtp("CRUDEOIL");

        // Assert
        assertEquals(true, ltpStart.isEmpty());
    }

    @Test
    void getLtp_ApiFails_FallbackEnabled_ReturnsMockValue() {
        // Arrange
        systemConfig.setEnableMockFallback(true);
        when(mcxApiService.getOptionChain(anyString(), anyString())).thenReturn(null);

        // Act
        java.util.Optional<Double> ltp = realMarketDataService.getLtp("CRUDEOIL");

        // Assert
        assertEquals(true, ltp.isPresent());
        assertEquals(true, ltp.get() > 0);
    }

}
