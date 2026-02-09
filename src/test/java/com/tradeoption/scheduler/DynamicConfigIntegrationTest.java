package com.tradeoption.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeoption.domain.SystemConfig;
import com.tradeoption.service.SystemConfigService;
import com.tradeoption.service.MarketStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class DynamicConfigIntegrationTest {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private MarketStatusService marketStatusService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    public void testDynamicIntervalUpdate() throws InterruptedException {
        // 1. Set initial interval
        SystemConfig config = systemConfigService.getConfig();
        config.setRefreshIntervalMs(10000); // 10 seconds
        systemConfigService.saveConfig(config);

        // 2. Add a listener to verify notification
        CountDownLatch latch = new CountDownLatch(1);
        systemConfigService.addListener(c -> {
            if (c.getRefreshIntervalMs() == 100) {
                latch.countDown();
            }
        });

        // 3. Update to fast interval
        config.setRefreshIntervalMs(100);
        systemConfigService.saveConfig(config);

        // 4. Verify listener triggered
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testMarketStatusOverride() {
        SystemConfig config = systemConfigService.getConfig();

        // Add special session for TODAY to force open
        LocalDate today = LocalDate.now();
        SystemConfig.SpecialSession session = new SystemConfig.SpecialSession();
        session.setDate(today.toString());
        session.setStartTime("00:00");
        session.setEndTime("23:59");
        session.setReason("Test Session");

        config.setSpecialSessions(Collections.singletonList(session));
        systemConfigService.saveConfig(config);

        assertThat(marketStatusService.isMarketOpen()).isTrue();
    }
}
