package com.seoulcareconnect.service.user;

import com.seoulcareconnect.dto.user.PolicyDocumentProgressResponse;
import com.seoulcareconnect.entity.policy.PolicyDocument;
import com.seoulcareconnect.entity.user.PolicyDocumentProgress;
import com.seoulcareconnect.repository.policy.PolicyDocumentRepository;
import com.seoulcareconnect.repository.user.PolicyDocumentProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyDocumentProgressServiceTest {

    @Mock
    private PolicyDocumentRepository policyDocumentRepository;

    @Mock
    private PolicyDocumentProgressRepository progressRepository;

    private PolicyDocumentProgressService service;

    @BeforeEach
    void setUp() {
        service = new PolicyDocumentProgressService(
                policyDocumentRepository,
                progressRepository
        );
    }

    @Test
    void savesProgressUsingTheStableChecklistKey() {
        PolicyDocument document = new PolicyDocument();
        document.setChecklistKey("RESIDENT_REGISTER_COPY");
        when(policyDocumentRepository.findByPolicyDocumentIdAndPolicy_PolicyId(51L, 31L))
                .thenReturn(Optional.of(document));
        when(progressRepository.findByUserIdAndPolicyIdAndDocumentKey(
                7L,
                31L,
                "RESIDENT_REGISTER_COPY"
        )).thenReturn(Optional.empty());

        PolicyDocumentProgressResponse response =
                service.update(7L, 31L, 51L, true);

        ArgumentCaptor<PolicyDocumentProgress> captor =
                ArgumentCaptor.forClass(PolicyDocumentProgress.class);
        verify(progressRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getPolicyId()).isEqualTo(31L);
        assertThat(captor.getValue().getDocumentKey())
                .isEqualTo("RESIDENT_REGISTER_COPY");
        assertThat(captor.getValue().isCompleted()).isTrue();
        assertThat(response.completed()).isTrue();
    }
}
