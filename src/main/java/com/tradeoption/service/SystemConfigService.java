package com.tradeoption.service;

import com.tradeoption.domain.SystemConfig;

public interface SystemConfigService {
    SystemConfig getConfig();

    void reloadConfig();

    void saveConfig(SystemConfig config);
}
