package com.tradeoption.controller;

import com.tradeoption.domain.Strategy;
import com.tradeoption.scheduler.DashboardBroadcaster;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/strategy")
public class StrategyController {

    private final DashboardBroadcaster dashboardBroadcaster;

    public StrategyController(DashboardBroadcaster dashboardBroadcaster) {
        this.dashboardBroadcaster = dashboardBroadcaster;
    }

    @PostMapping
    public ResponseEntity<String> updateStrategy(@RequestBody Strategy strategy) {
        dashboardBroadcaster.setStrategy(strategy);
        return ResponseEntity.ok("Strategy updated successfully");
    }
}
