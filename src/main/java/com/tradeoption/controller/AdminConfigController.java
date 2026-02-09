package com.tradeoption.controller;

import com.tradeoption.domain.SystemConfig;
import com.tradeoption.service.SystemConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminConfigController {

    private final SystemConfigService systemConfigService;

    public AdminConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/config")
    public ResponseEntity<SystemConfig> getConfig() {
        return ResponseEntity.ok(systemConfigService.getConfig());
    }

    @PostMapping("/config")
    public ResponseEntity<Void> updateConfig(@RequestBody SystemConfig config) {
        systemConfigService.saveConfig(config);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/special-session")
    public ResponseEntity<Void> addSpecialSession(@RequestBody SystemConfig.SpecialSession session) {
        SystemConfig config = systemConfigService.getConfig();
        if (config.getSpecialSessions() == null) {
            config.setSpecialSessions(new java.util.ArrayList<>());
        }
        config.getSpecialSessions().add(session);
        systemConfigService.saveConfig(config);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/special-session")
    public ResponseEntity<Void> removeSpecialSession(@RequestParam String date) {
        SystemConfig config = systemConfigService.getConfig();
        if (config.getSpecialSessions() != null) {
            config.getSpecialSessions().removeIf(s -> s.getDate().equals(date));
            systemConfigService.saveConfig(config);
        }
        return ResponseEntity.ok().build();
    }
}
