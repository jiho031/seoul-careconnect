package com.seoulcareconnect.service.policy;

import com.seoulcareconnect.entity.policy.DocumentGuide;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDocument;
import com.seoulcareconnect.entity.policy.enums.DocumentCategory;
import com.seoulcareconnect.entity.policy.enums.DocumentRequirementType;
import com.seoulcareconnect.integration.policy.ExternalPolicyDocument;
import com.seoulcareconnect.repository.policy.PolicyDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyDocumentServiceTest {

    @Mock
    private PolicyDocumentRepository policyDocumentRepository;

    @Mock
    private DocumentGuideService documentGuideService;

    private PolicyDocumentService service;

    @BeforeEach
    void setUp() {
        service = new PolicyDocumentService(
                policyDocumentRepository,
                documentGuideService
        );
    }

    @Test
    void structuresDocumentsAndRemovesSharedSiteNoise() {
        List<DocumentGuide> guides = List.of(
                guide(
                        "APPLICATION_FORM",
                        "참가·지원 신청서",
                        DocumentCategory.ANNOUNCEMENT_FORM,
                        "참가신청서\n신청서"
                ),
                guide(
                        "BUSINESS_PLAN",
                        "사업계획서",
                        DocumentCategory.SELF_WRITTEN,
                        "사업계획서"
                ),
                guide(
                        "SPORTS_INSTRUCTOR_LICENSE",
                        "체육지도자 자격증",
                        DocumentCategory.EDUCATION_QUALIFICATION,
                        "체육지도자 자격증"
                ),
                guide(
                        "CORPORATE_REGISTRY",
                        "법인등기사항증명서",
                        DocumentCategory.BUSINESS_CORPORATE,
                        "법인등기부등본"
                ),
                guide(
                        "BUSINESS_REGISTRATION_COPY",
                        "사업자등록증 사본",
                        DocumentCategory.BUSINESS_CORPORATE,
                        "사업자등록증"
                )
        );
        when(documentGuideService.activeGuides()).thenReturn(guides);

        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "policyId", 31L);
        String documents = """
                참가신청서 및 사업계획서 (별첨1)
                체육지도자 자격증(지도자인 경우)
                법인등기부등본 또는 사업자등록증(공고일 이후 발급분)
                새롭게 등장한 활동내역
                중소벤처24 증명서 및 사업신청 통화서비스 제공
                중소기업현황정보시스템 중소기업확인서 신청 및 발급 제공
                개인정보처리방침
                """;
        ExternalPolicyDocument attachment = ExternalPolicyDocument.builder()
                .text("참가신청서 및 사업계획서 (별첨1)")
                .sourceType("OFFICIAL_ATTACHMENT")
                .attachmentName("별첨1 참가신청서 및 사업계획서.hwp")
                .downloadUrl("https://official.example/form.hwp")
                .confidence(100)
                .build();

        service.sync(
                policy,
                documents,
                List.of(attachment),
                "https://official.example/notice"
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PolicyDocument>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(policyDocumentRepository).saveAll(captor.capture());
        List<PolicyDocument> saved = captor.getValue();

        assertThat(saved).hasSize(6);
        assertThat(saved)
                .extracting(PolicyDocument::getDisplayName)
                .containsExactly(
                        "참가·지원 신청서",
                        "사업계획서",
                        "체육지도자 자격증",
                        "법인등기사항증명서",
                        "사업자등록증 사본",
                        "새롭게 등장한 활동내역"
                );
        assertThat(saved)
                .extracting(PolicyDocument::getOriginalText)
                .noneMatch(value -> value.contains("개인정보처리방침")
                        || value.contains("중소벤처24")
                        || value.contains("중소기업현황정보시스템"));

        PolicyDocument application = saved.get(0);
        assertThat(application.getDownloadUrl())
                .isEqualTo("https://official.example/form.hwp");
        assertThat(application.getAttachmentName())
                .isEqualTo("별첨1 참가신청서 및 사업계획서.hwp");

        PolicyDocument conditional = saved.get(2);
        assertThat(conditional.getRequirementType())
                .isEqualTo(DocumentRequirementType.CONDITIONAL);
        assertThat(conditional.getConditionText()).isEqualTo("지도자인 경우");

        PolicyDocument firstAlternative = saved.get(3);
        PolicyDocument secondAlternative = saved.get(4);
        assertThat(firstAlternative.getRequirementType())
                .isEqualTo(DocumentRequirementType.ALTERNATIVE);
        assertThat(secondAlternative.getRequirementType())
                .isEqualTo(DocumentRequirementType.ALTERNATIVE);
        assertThat(firstAlternative.getAlternativeGroup())
                .isEqualTo(secondAlternative.getAlternativeGroup())
                .isNotBlank();

        PolicyDocument unknown = saved.get(5);
        assertThat(unknown.getGuide()).isNull();
        assertThat(unknown.getCategory()).isEqualTo(DocumentCategory.OTHER);
        assertThat(unknown.getOfficialPageUrl())
                .isEqualTo("https://official.example/notice");
    }

    @Test
    void keepsDistinctDocumentsWhenOneAliasContainsTheOther() {
        when(documentGuideService.activeGuides()).thenReturn(List.of(
                guide(
                        "BUSINESS_REGISTRATION_CERTIFICATE",
                        "사업자등록증명",
                        DocumentCategory.BUSINESS_CORPORATE,
                        "사업자등록증명원"
                ),
                guide(
                        "BUSINESS_REGISTRATION_COPY",
                        "사업자등록증 사본",
                        DocumentCategory.BUSINESS_CORPORATE,
                        "사업자등록증 사본\n사업자등록증"
                )
        ));

        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "policyId", 32L);

        service.sync(
                policy,
                "사업자등록증명원 또는 사업자등록증 사본",
                List.of(),
                "https://official.example/notice"
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PolicyDocument>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(policyDocumentRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(PolicyDocument::getDisplayName)
                .containsExactly("사업자등록증명", "사업자등록증 사본");
    }

    @Test
    void removesNoiseOnlyDocumentTextWithoutCreatingChecklistItems() {
        when(documentGuideService.activeGuides()).thenReturn(List.of());
        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "policyId", 33L);

        service.sync(
                policy,
                """
                        중소벤처24 증명서 및 사업신청 통화서비스 제공
                        중소기업현황정보시스템 중소기업확인서 신청 및 발급 제공
                        개인정보처리방침
                        """,
                List.of(),
                "https://official.example/notice"
        );

        verify(policyDocumentRepository).deleteAllByPolicyId(33L);
        verify(policyDocumentRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    private DocumentGuide guide(
            String key,
            String displayName,
            DocumentCategory category,
            String aliases
    ) {
        DocumentGuide guide = new DocumentGuide();
        guide.setGuideKey(key);
        guide.setDisplayName(displayName);
        guide.setCategory(category);
        guide.setAliasesText(aliases);
        guide.setActive(true);
        return guide;
    }
}
