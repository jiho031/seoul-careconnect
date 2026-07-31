package com.seoulcareconnect.service.ai;

import com.seoulcareconnect.dto.ai.AiPolicyExplanationDto;
import com.seoulcareconnect.service.policy.PolicyDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPolicySummaryGenerationServiceTest {

    @Mock
    private AiPolicyExplanationService explanationService;

    @Mock
    private PolicyDetailService policyDetailService;

    private Queue<Runnable> tasks;
    private AiPolicySummaryGenerationService service;

    @BeforeEach
    void setUp() {
        tasks = new ArrayDeque<>();
        TaskExecutor executor = tasks::add;
        service = new AiPolicySummaryGenerationService(
                explanationService,
                policyDetailService,
                executor
        );
    }

    @Test
    void reportsActualServerStagesAndCompletesTheSummaryJob() {
        when(explanationService.findGenerated(54L)).thenReturn(
                Optional.empty(),
                Optional.of(mock(AiPolicyExplanationDto.class))
        );
        when(explanationService.isOpenAiConfigured()).thenReturn(true);
        AtomicReference<String> savingState = new AtomicReference<>();
        doAnswer(invocation -> {
            Runnable beforeSave = invocation.getArgument(1, Runnable.class);
            beforeSave.run();
            savingState.set(service.status(54L).state());
            return mock(AiPolicyExplanationDto.class);
        }).when(explanationService).generateIfMissing(eq(54L), any(Runnable.class));

        var queued = service.start(54L);

        assertThat(queued.state()).isEqualTo("QUEUED");
        assertThat(queued.stage()).isEqualTo(1);
        assertThat(tasks).hasSize(1);

        tasks.remove().run();

        assertThat(savingState.get()).isEqualTo("SAVING");
        assertThat(service.status(54L).state()).isEqualTo("COMPLETED");
        assertThat(service.status(54L).stage()).isEqualTo(4);
        verify(policyDetailService).get(54L);
        verify(explanationService).generateIfMissing(eq(54L), any(Runnable.class));
    }

    @Test
    void returnsCompletedWithoutStartingAnotherJobWhenSummaryAlreadyExists() {
        when(explanationService.findGenerated(54L))
                .thenReturn(Optional.of(mock(AiPolicyExplanationDto.class)));

        var response = service.start(54L);

        assertThat(response.state()).isEqualTo("COMPLETED");
        assertThat(response.stage()).isEqualTo(4);
        assertThat(tasks).isEmpty();
        verify(policyDetailService).get(54L);
        verify(explanationService).findGenerated(54L);
        verifyNoMoreInteractions(explanationService);
    }
}
