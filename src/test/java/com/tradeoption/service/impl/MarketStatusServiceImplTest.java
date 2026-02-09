package com.tradeoption.service.impl;

import com.tradeoption.domain.SystemConfig;
import com.tradeoption.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketStatusServiceImplTest {

    @Mock
    private SystemConfigService systemConfigService;

    private MarketStatusServiceImpl marketStatusService;
    private SystemConfig config;

    @BeforeEach
    void setUp() {
        marketStatusService = new MarketStatusServiceImpl(systemConfigService);
        config = new SystemConfig();
        // Set defaults matching the code
        config.setMarketOpenTime("09:00");
        config.setMarketCloseTime("23:45");
        config.setMarketClosedDays(Arrays.asList("SUNDAY", "MONDAY"));
        when(systemConfigService.getConfig()).thenReturn(config);
    }

    @Test
    void testIsMarketOpen_StandardHours_Open() {
        // Mock time: Tuesday at 10:00
        LocalDateTime mockNow = LocalDateTime.of(2024, 3, 12, 10, 0); // March 12, 2024 is a Tuesday

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(mockNow);
            assertTrue(marketStatusService.isMarketOpen());
            assertEquals("Market is Open", marketStatusService.getMarketStatusReason());
        }
    }

    @Test
    void testIsMarketOpen_Weekend_Closed() {
        // Mock time: Sunday
        LocalDateTime mockNow = LocalDateTime.of(2024, 3, 10, 10, 0); // March 10, 2024 is a Sunday

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(mockNow);
            assertFalse(marketStatusService.isMarketOpen());
            assertEquals("Market Closed (Day off)", marketStatusService.getMarketStatusReason());
        }
    }

    @Test
    void testIsMarketOpen_BeforeOpen_Closed() {
        // Mock time: Tuesday at 08:00
        LocalDateTime mockNow = LocalDateTime.of(2024, 3, 12, 8, 0);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(mockNow);
            assertFalse(marketStatusService.isMarketOpen());
            assertEquals("Outside Trading Hours", marketStatusService.getMarketStatusReason());
        }
    }

    @Test
    void testIsMarketOpen_AfterClose_Closed() {
        // Mock time: Tuesday at 23:50
        LocalDateTime mockNow = LocalDateTime.of(2024, 3, 12, 23, 50);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(mockNow);
            assertFalse(marketStatusService.isMarketOpen());
            assertEquals("Outside Trading Hours", marketStatusService.getMarketStatusReason());
        }
    }

    @Test
    void testIsMarketOpen_Holiday_Closed() {
        // Mock time: Tuesday at 10:00 (Standard Open) but set as Holiday
        LocalDateTime mockNow = LocalDateTime.of(2024, 3, 12, 10, 0);
        config.setHolidays(Collections.singletonList("2024-03-12"));

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(mockNow);
            assertFalse(marketStatusService.isMarketOpen());
            assertEquals("Holiday", marketStatusService.getMarketStatusReason());
        }
    }

    @Test
    void testConfigurableHours() {
        // Change config to open 10:00 - 15:00
        config.setMarketOpenTime("10:00");
        config.setMarketCloseTime("15:00");

        // Check 09:30 (now closed)
        LocalDateTime mockNow = LocalDateTime.of(2024, 3, 12, 9, 30);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(mockNow);
            assertFalse(marketStatusService.isMarketOpen());
        }

        // Check 14:00 (now open)
        mockNow = LocalDateTime.of(2024, 3, 12, 14, 0);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(mockNow);
            assertTrue(marketStatusService.isMarketOpen());
        }
    }
}
