package com.seoulcareconnect.service.ai;

import com.seoulcareconnect.entity.ai.AiPolicyExplanation.ReviewStatus;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiSummaryAutomationServiceTest {

    @Mock
    private AiSummarySettingService settingService;

    @Mock
    private AiPolicyExplanationService explanationService;

    @Mock
    private PolicyRepository policyRepository;

    private AiSummaryAutomationService service;

    @BeforeEach
    void setUp() {
        TaskExecutor directExecutor = Runnable::run;
        service = new AiSummaryAutomationService(
                settingService,
                explanationService,
                policyRepository,
                directExecutor
        );
    }

    @Test
    void generatesEveryMissingSummaryInManualMode() {
        when(settingService.isAutomaticSummaryEnabled()).thenReturn(false);
        when(explanationService.isOpenAiConfigured()).thenReturn(true);
        when(policyRepository.findIdsWithoutAiExplanation(
                anyList(),
                eq(ApplyStatus.EXPIRED),
                eq(ReviewStatus.APPROVED)
        )).thenReturn(List.of(11L, 12L));

        AiSummaryAutomationService.StartResult result = service.startManualGeneration();
        AiSummaryAutomationService.BatchStatus status = service.manualGenerationStatus();

        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.started()).isTrue();
        assertThat(status.running()).isFalse();
        assertThat(status.processedCount()).isEqualTo(2);
        assertThat(status.succeededCount()).isEqualTo(2);
        assertThat(status.failedCount()).isZero();
        verify(explanationService).generateIfMissing(11L);
        verify(explanationService).generateIfMissing(12L);
    }

    @Test
    void skipsCreatedPolicyWhenAutomaticModeIsDisabled() {
        when(settingService.isAutomaticSummaryEnabled()).thenReturn(false);

        service.handlePolicyCreated(new AiSummaryAutomationService.PolicyCreated(21L));

        verifyNoInteractions(policyRepository, explanationService);
    }

    @Test
    void generatesCreatedPolicyWhenAutomaticModeIsEnabled() {
        Policy policy = new Policy();
        policy.setStatus(PolicyStatus.AUTO_PUBLISHED);
        policy.setApplyStatus(ApplyStatus.OPEN);
        when(settingService.isAutomaticSummaryEnabled()).thenReturn(true);
        when(policyRepository.findById(21L)).thenReturn(Optional.of(policy));

        service.handlePolicyCreated(new AiSummaryAutomationService.PolicyCreated(21L));

        verify(explanationService).generateIfMissing(21L);
    }
}
