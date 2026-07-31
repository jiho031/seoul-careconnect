package com.seoulcareconnect.service.ai;

import com.seoulcareconnect.entity.ai.AiSummarySetting;
import com.seoulcareconnect.repository.ai.AiSummarySettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiSummarySettingService {

    private final AiSummarySettingRepository settingRepository;

    public boolean isAutomaticSummaryEnabled() {
        return settingRepository.findById(AiSummarySetting.DEFAULT_ID)
                .map(AiSummarySetting::isAutomaticSummaryEnabled)
                .orElse(false);
    }

    @Transactional
    public boolean updateAutomaticSummaryEnabled(boolean enabled) {
        AiSummarySetting setting = settingRepository.findById(AiSummarySetting.DEFAULT_ID)
                .orElseGet(AiSummarySetting::defaultSetting);
        setting.updateAutomaticSummaryEnabled(enabled);
        return settingRepository.save(setting).isAutomaticSummaryEnabled();
    }
}
