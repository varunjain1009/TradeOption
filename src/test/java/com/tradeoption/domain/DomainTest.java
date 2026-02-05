package com.tradeoption.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class DomainTest {

    @Test
    public void testMarketData() {
        LocalDateTime now = LocalDateTime.now();
        MarketData md = new MarketData("NIFTY", 22000.0, now);

        assertEquals("NIFTY", md.getSymbol());
        assertEquals(22000.0, md.getLtp());
        assertEquals(now, md.getTimestamp());

        md.setSymbol("BANKNIFTY");
        md.setLtp(48000.0);
        LocalDateTime later = now.plusMinutes(1);
        md.setTimestamp(later);

        assertEquals("BANKNIFTY", md.getSymbol());
        assertEquals(48000.0, md.getLtp());
        assertEquals(later, md.getTimestamp());
    }

    @Test
    public void testEntryUpdateRequest() {
        EntryUpdateRequest req = new EntryUpdateRequest();
        req.setPrice(100.0);
        req.setQuantity(50);

        assertEquals(100.0, req.getPrice());
        assertEquals(50, req.getQuantity());
    }

    @Test
    public void testPnlHistoryPoint() {
        PnlHistoryPoint point = new PnlHistoryPoint(1000L, 500.0);
        assertEquals(1000L, point.getTimestamp());
        assertEquals(500.0, point.getRealizedPnl());

        point.setTimestamp(2000L);
        point.setRealizedPnl(600.0);
        assertEquals(2000L, point.getTimestamp());
        assertEquals(600.0, point.getRealizedPnl());
    }

    @Test
    public void testDashboardMetrics() {
        Greeks greeks = new Greeks();
        long now = System.currentTimeMillis();
        DashboardMetrics metrics = new DashboardMetrics(greeks, 100.0, 500.0, -200.0, 0.65, 2.5, now);

        assertEquals(greeks, metrics.getGreeks());
        assertEquals(100.0, metrics.getCurrentPnl());
        assertEquals(500.0, metrics.getMaxProfit());
        assertEquals(-200.0, metrics.getMaxLoss());
        assertEquals(0.65, metrics.getProbabilityOfProfit());
        assertEquals(2.5, metrics.getRiskRewardRatio());
        assertEquals(now, metrics.getTimestamp());

        metrics.setCurrentPnl(200.0);
        assertEquals(200.0, metrics.getCurrentPnl());
    }

    @Test
    public void testSystemConfig() {
        SystemConfig config = new SystemConfig();
        config.setRiskFreeRate(0.06);
        config.setRefreshIntervalMs(2000L);
        config.setMaxBidAskDiffPercent(2.0);

        assertEquals(0.06, config.getRiskFreeRate());
        assertEquals(2000L, config.getRefreshIntervalMs());
        assertEquals(2.0, config.getMaxBidAskDiffPercent());
    }
}
