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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
                eq(ReviewStatus.APPROVED),
                any(LocalDate.class)
        )).thenReturn(List.of(11L, 12L));
        when(policyRepository.findById(11L)).thenReturn(Optional.of(eligiblePolicy(null)));
        when(policyRepository.findById(12L)).thenReturn(Optional.of(eligiblePolicy(seoulToday())));

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
    void excludesEndedPolicyBeforeManualSummaryRuns() {
        when(settingService.isAutomaticSummaryEnabled()).thenReturn(false);
        when(explanationService.isOpenAiConfigured()).thenReturn(true);
        when(policyRepository.findIdsWithoutAiExplanation(
                anyList(),
                eq(ApplyStatus.EXPIRED),
                eq(ReviewStatus.APPROVED),
                any(LocalDate.class)
        )).thenReturn(List.of(31L));
        when(policyRepository.findById(31L))
                .thenReturn(Optional.of(eligiblePolicy(seoulToday().minusDays(1))));

        service.startManualGeneration();

        AiSummaryAutomationService.BatchStatus status = service.manualGenerationStatus();
        assertThat(status.requestedCount()).isZero();
        assertThat(status.processedCount()).isZero();
        verify(explanationService, never()).generateIfMissing(anyLong());
    }

    @Test
    void stopsAfterTheCurrentlyProcessingPolicyFinishes() {
        when(settingService.isAutomaticSummaryEnabled()).thenReturn(false);
        when(explanationService.isOpenAiConfigured()).thenReturn(true);
        when(policyRepository.findIdsWithoutAiExplanation(
                anyList(),
                eq(ApplyStatus.EXPIRED),
                eq(ReviewStatus.APPROVED),
                any(LocalDate.class)
        )).thenReturn(List.of(11L, 12L));
        when(policyRepository.findById(11L)).thenReturn(Optional.of(eligiblePolicy(null)));
        doAnswer(invocation -> {
            assertThat(service.requestManualGenerationStop()).isTrue();
            return null;
        }).when(explanationService).generateIfMissing(11L);

        service.startManualGeneration();

        AiSummaryAutomationService.BatchStatus status = service.manualGenerationStatus();
        assertThat(status.running()).isFalse();
        assertThat(status.stopped()).isTrue();
        assertThat(status.stopRequested()).isFalse();
        assertThat(status.processedCount()).isEqualTo(1);
        assertThat(status.succeededCount()).isEqualTo(1);
        verify(explanationService).generateIfMissing(11L);
        verify(explanationService, never()).generateIfMissing(12L);
    }

    @Test
    void countsOnlyPoliciesWhoseEndDateHasNotPassed() {
        when(policyRepository.countWithoutAiExplanation(
                anyList(),
                eq(ApplyStatus.EXPIRED),
                eq(ReviewStatus.APPROVED),
                any(LocalDate.class)
        )).thenReturn(4L);

        assertThat(service.countMissingSummaries()).isEqualTo(4L);
        verify(policyRepository).countWithoutAiExplanation(
                anyList(),
                eq(ApplyStatus.EXPIRED),
                eq(ReviewStatus.APPROVED),
                any(LocalDate.class)
        );
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

    @Test
    void skipsUpdatedPolicyWhenAutomaticModeIsDisabled() {
        when(settingService.isAutomaticSummaryEnabled()).thenReturn(false);

        service.handlePolicyUpdated(new AiSummaryAutomationService.PolicyUpdated(22L));

        verifyNoInteractions(policyRepository, explanationService);
    }

    @Test
    void regeneratesUpdatedPolicyWhenAutomaticModeIsEnabled() {
        Policy policy = new Policy();
        policy.setStatus(PolicyStatus.AUTO_PUBLISHED);
        policy.setApplyStatus(ApplyStatus.OPEN);
        when(settingService.isAutomaticSummaryEnabled()).thenReturn(true);
        when(policyRepository.findById(22L)).thenReturn(Optional.of(policy));

        service.handlePolicyUpdated(new AiSummaryAutomationService.PolicyUpdated(22L));

        verify(explanationService).regenerate(22L);
    }

    private Policy eligiblePolicy(LocalDate endDate) {
        Policy policy = new Policy();
        policy.setStatus(PolicyStatus.APPROVED);
        policy.setApplyStatus(ApplyStatus.OPEN);
        policy.setEndDate(endDate);
        return policy;
    }

    private LocalDate seoulToday() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
}
