package com.tradeoption.service.impl;

import com.tradeoption.domain.SystemConfig;
import com.tradeoption.service.MarketStatusService;
import com.tradeoption.service.SystemConfigService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MarketStatusServiceImpl implements MarketStatusService {

    private final SystemConfigService systemConfigService;

    public MarketStatusServiceImpl(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public boolean isMarketOpen() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        DayOfWeek day = today.getDayOfWeek();
        SystemConfig config = systemConfigService.getConfig();

        // 1. Check Special Sessions (Overrides everything)
        if (config.getSpecialSessions() != null) {
            for (SystemConfig.SpecialSession session : config.getSpecialSessions()) {
                if (isSpecialSessionActive(session, now)) {
                    return true; // Force Open
                }
            }
        }

        // 2. Check Weekend/Closed Days (Configurable)
        if (config.getMarketClosedDays() != null && config.getMarketClosedDays().contains(day.toString())) {
            // Unless special session active (checked above)
            return false;
        }

        // 3. Check Holidays
        if (config.getHolidays() != null && config.getHolidays().contains(today.toString())) {
            return false;
        }

        // 4. Check Standard Time (Configurable)
        LocalTime openTime = LocalTime.parse(config.getMarketOpenTime());
        LocalTime closeTime = LocalTime.parse(config.getMarketCloseTime());

        if (time.isBefore(openTime) || time.isAfter(closeTime)) {
            return false;
        }

        return true;
    }

    @Override
    public String getMarketStatusReason() {
        if (isMarketOpen())
            return "Market is Open";

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        SystemConfig config = systemConfigService.getConfig();
        DayOfWeek day = today.getDayOfWeek();

        if (config.getMarketClosedDays() != null && config.getMarketClosedDays().contains(day.toString()))
            return "Market Closed (Day off)";
        if (config.getHolidays() != null && config.getHolidays().contains(today.toString()))
            return "Holiday";

        LocalTime openTime = LocalTime.parse(config.getMarketOpenTime());
        LocalTime closeTime = LocalTime.parse(config.getMarketCloseTime());

        if (time.isBefore(openTime) || time.isAfter(closeTime))
            return "Outside Trading Hours";

        return "Market Closed";
    }

    private boolean isSpecialSessionActive(SystemConfig.SpecialSession session, LocalDateTime now) {
        try {
            LocalDate sessionDate = LocalDate.parse(session.getDate());
            if (!sessionDate.isEqual(now.toLocalDate()))
                return false;

            LocalTime start = LocalTime.parse(session.getStartTime());
            LocalTime end = LocalTime.parse(session.getEndTime());
            LocalTime currentTime = now.toLocalTime();

            return !currentTime.isBefore(start) && !currentTime.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }
}
