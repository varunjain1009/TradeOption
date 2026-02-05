package com.tradeoption.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeoption.domain.AnalyticsSnapshot;
import com.tradeoption.domain.Greeks;
import com.tradeoption.domain.Strategy;
import com.tradeoption.repository.RocksDBRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HistoricalAnalyticsIntegrationTest {

    private RocksDBRepository repository;
    private HistoricalAnalyticsServiceImpl service;
    private static final String TEST_DB_FILE = "trade-option-db";

    @BeforeEach
    public void setUp() {
        FileSystemUtils.deleteRecursively(new File(TEST_DB_FILE));
        repository = new RocksDBRepository(new ObjectMapper());
        repository.init();
        service = new HistoricalAnalyticsServiceImpl(repository);
    }

    @AfterEach
    public void tearDown() {
        repository.close();
        FileSystemUtils.deleteRecursively(new File(TEST_DB_FILE));
    }

    @Test
    public void testCaptureAndRetrieveHistory() throws InterruptedException {
        String strategyId = "strat-" + UUID.randomUUID().toString();
        Strategy strategy = new Strategy();
        strategy.setId(strategyId);

        // Capture data at t1
        Greeks g1 = new Greeks();
        g1.setDelta(0.5);
        service.captureSnapshot(strategy, g1, 100.0, 15000.0);

        Thread.sleep(10); // Ensure timestamp diff (though uniqueness relies on timestamp, so collisions
                          // possible if super fast)

        // Capture data at t2
        Greeks g2 = new Greeks();
        g2.setDelta(0.6);
        service.captureSnapshot(strategy, g2, 200.0, 15010.0);

        // Retrieve
        List<AnalyticsSnapshot> history = service.getHistory(strategyId);

        assertEquals(2, history.size());

        // Verify Order (Oldest first because of RocksDB lexicographical key sort order
        // of "STRAT:ID:HIST:TIMESTAMP")
        // Wait, "STRAT:ID:HIST:000...Small" comes before "STRAT:ID:HIST:000...Large"
        // So iterator is correct.

        AnalyticsSnapshot s1 = history.get(0);
        AnalyticsSnapshot s2 = history.get(1);

        assertTrue(s1.getTimestamp() < s2.getTimestamp());
        assertEquals(100.0, s1.getPnl());
        assertEquals(200.0, s2.getPnl());
        assertEquals(0.5, s1.getGreeks().getDelta());
    }
}
