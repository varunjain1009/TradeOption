package com.tradeoption.service.impl;

import com.tradeoption.domain.AnalyticsSnapshot;
import com.tradeoption.domain.Greeks;
import com.tradeoption.domain.Strategy;
import com.tradeoption.repository.RocksDBRepository;
import com.tradeoption.service.HistoricalAnalyticsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class HistoricalAnalyticsServiceImpl implements HistoricalAnalyticsService {

    private final RocksDBRepository repository;

    public HistoricalAnalyticsServiceImpl(RocksDBRepository repository) {
        this.repository = repository;
    }

    @Override
    public void captureSnapshot(Strategy strategy, Greeks greeks, double pnl, double spotPrice) {
        // Ensure strategy has ID
        if (strategy.getId() == null) {
            // For now, if no ID, ignore or generate one?
            // Better to assume Strategy has one. If not, we can't key it properly.
            // Let's assume the caller ensures this.
            return;
        }

        long timestamp = System.currentTimeMillis();
        AnalyticsSnapshot snapshot = new AnalyticsSnapshot(timestamp, strategy.getId(), pnl, greeks, spotPrice);

        // Key Format: STRAT:{ID}:HIST:{Timestamp}
        // Use zero-padding for timestamp to ensure lexicographical order matches
        // chronological order
        // 20 digits covers Long.MAX_VALUE
        String key = String.format("STRAT:%s:HIST:%020d", strategy.getId(), timestamp);

        repository.save(key, snapshot);
    }

    @Override
    public List<AnalyticsSnapshot> getHistory(String strategyId) {
        String prefix = String.format("STRAT:%s:HIST:", strategyId);
        return repository.findByPrefix(prefix, AnalyticsSnapshot.class);
    }
}
