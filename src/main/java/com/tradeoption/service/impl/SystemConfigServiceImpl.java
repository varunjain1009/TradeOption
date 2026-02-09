package com.tradeoption.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeoption.domain.SystemConfig;
import com.tradeoption.service.SystemConfigService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigServiceImpl.class);
    private static final String CONFIG_FILE = "config.json";

    private final ObjectMapper objectMapper;
    private SystemConfig currentConfig;

    public SystemConfigServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // Start with default
        this.currentConfig = new SystemConfig();
    }

    @PostConstruct
    public void init() {
        reloadConfig();
    }

    @Override
    public SystemConfig getConfig() {
        return currentConfig;
    }

    @Scheduled(fixedRate = 5000) // Poll every 5 seconds for changes
    @Override
    public void reloadConfig() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try {
                SystemConfig newConfig = objectMapper.readValue(file, SystemConfig.class);
                // Simple equality check or always notify?
                // For now, let's always notify if we successfully read, or maybe implement
                // equals
                // Ideally, check hash or something.
                // Let's just notify. The consumers can debounce if needed.
                this.currentConfig = newConfig;
                notifyListeners();
            } catch (IOException e) {
                logger.error("Failed to load configuration from " + CONFIG_FILE, e);
            }
        } else {
            logger.warn("Configuration file {} not found, using defaults. Creating default file...", CONFIG_FILE);
            saveConfig(currentConfig); // Create default file
        }
    }

    @Override
    public void saveConfig(SystemConfig config) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(CONFIG_FILE), config);
            this.currentConfig = config;
            logger.info("Configuration saved to {}", CONFIG_FILE);
            notifyListeners();
        } catch (IOException e) {
            logger.error("Failed to save configuration to " + CONFIG_FILE, e);
        }
    }

    private final java.util.List<java.util.function.Consumer<SystemConfig>> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Override
    public void addListener(java.util.function.Consumer<SystemConfig> listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (java.util.function.Consumer<SystemConfig> listener : listeners) {
            listener.accept(currentConfig);
        }
    }
}
