package com.seoulcareconnect.service.impl.policy;

import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.RawCollectedItem;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.integration.policy.ExternalDateParser;
import com.seoulcareconnect.integration.policy.ExternalPolicyClassifier;
import com.seoulcareconnect.integration.policy.ExternalPolicyItem;
import com.seoulcareconnect.integration.policy.SeoulPolicyFilter;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.policy.RawCollectedItemRepository;
import com.seoulcareconnect.service.ai.AiSummaryAutomationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyUpsertServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private RawCollectedItemRepository rawCollectedItemRepository;

    @Mock
    private SeoulPolicyFilter seoulPolicyFilter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PolicyUpsertService service;

    @BeforeEach
    void setUp() {
        service = new PolicyUpsertService(
                policyRepository,
                rawCollectedItemRepository,
                new ExternalDateParser(),
                new ExternalPolicyClassifier(),
                seoulPolicyFilter,
                eventPublisher
        );
        ReflectionTestUtils.setField(service, "zoneId", "Asia/Seoul");
        ReflectionTestUtils.setField(service, "closingSoonDays", 7);
        ReflectionTestUtils.setField(service, "excludeExpired", true);
        ReflectionTestUtils.setField(service, "includeNoEndDate", true);
    }

    @Test
    void publishesSummaryUpdateWhenRequiredDocumentsChange() {
        PolicySource source = new PolicySource();
        ReflectionTestUtils.setField(source, "sourceId", 3L);
        source.setSourceName("서울정책API");

        Policy existing = new Policy();
        ReflectionTestUtils.setField(existing, "policyId", 17L);
        existing.setSource(source);
        existing.setExternalId("policy-17");
        existing.setTitle("중장년 일자리 지원");
        existing.setCategory(PolicyCategory.JOB);
        existing.setTarget("서울시 중장년");
        existing.setRegion("서울특별시");
        existing.setApplyStatus(ApplyStatus.OPEN);
        existing.setApplyMethod("온라인 신청");
        existing.setOfficialUrl("https://example.go.kr/policy/17");
        existing.setContact("서울시");
        existing.setStatus(PolicyStatus.AUTO_PUBLISHED);

        PolicyDetail existingDetail = new PolicyDetail();
        existingDetail.setBenefit("취업 지원");
        existingDetail.setSelectionCriteria("서울시 중장년");
        existingDetail.setRequiredDocumentsText("참여신청서");
        existingDetail.setContentText("정책 상세");
        existing.attachDetail(existingDetail);

        ExternalPolicyItem incoming = ExternalPolicyItem.builder()
                .externalId("policy-17")
                .title("중장년 일자리 지원")
                .agencyName("서울시")
                .category(PolicyCategory.JOB)
                .target("서울시 중장년")
                .region("서울특별시")
                .applyMethod("온라인 신청")
                .officialUrl("https://example.go.kr/policy/17")
                .benefit("취업 지원")
                .selectionCriteria("서울시 중장년")
                .requiredDocumentsText("참여신청서\n주민등록등본")
                .contentText("정책 상세")
                .rawJson("{\"id\":\"policy-17\"}")
                .build();

        when(policyRepository.findFirstBySource_SourceIdAndExternalId(
                3L,
                "policy-17"
        )).thenReturn(Optional.of(existing));
        when(rawCollectedItemRepository
                .findFirstBySource_SourceIdAndContentHashOrderByCollectedAtDesc(
                        eq(3L),
                        anyString()
                ))
                .thenReturn(Optional.of(new RawCollectedItem()));
        when(seoulPolicyFilter.resolveRegion("서울정책API", incoming))
                .thenReturn("서울특별시");
        when(policyRepository.save(any(Policy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.upsert(source, incoming);

        verify(eventPublisher).publishEvent(any(AiSummaryAutomationService.PolicyUpdated.class));
    }
}
