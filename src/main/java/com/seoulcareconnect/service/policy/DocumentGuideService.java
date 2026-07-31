package com.seoulcareconnect.service.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.entity.policy.DocumentGuide;
import com.seoulcareconnect.entity.policy.enums.DocumentCategory;
import com.seoulcareconnect.repository.policy.DocumentGuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentGuideService {

    private final DocumentGuideRepository documentGuideRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void seedDefaults() {
        for (GuideSeed seed : readSeeds()) {
            DocumentGuide existingGuide = documentGuideRepository
                    .findByGuideKey(seed.guideKey())
                    .orElse(null);

            if (existingGuide != null) {
                boolean changed = false;

                if (!StringUtils.hasText(existingGuide.getHelpName())
                        && StringUtils.hasText(seed.helpName())) {
                    existingGuide.setHelpName(seed.helpName());
                    changed = true;
                }

                if (!StringUtils.hasText(existingGuide.getHelpUrl())
                        && StringUtils.hasText(seed.helpUrl())) {
                    existingGuide.setHelpUrl(seed.helpUrl());
                    changed = true;
                }

                if (changed) {
                    documentGuideRepository.save(existingGuide);
                }
                continue;
            }

            DocumentGuide guide = new DocumentGuide();
            guide.setGuideKey(seed.guideKey());
            guide.setDisplayName(seed.displayName());
            guide.setCategory(seed.category());
            guide.setIssuer(seed.issuer());
            guide.setOfficialUrl(seed.officialUrl());
            guide.setHelpName(seed.helpName());
            guide.setHelpUrl(seed.helpUrl());
            guide.setSummary(seed.summary());
            guide.setStepsText(joinLines(seed.steps()));
            guide.setPreparationText(joinLines(seed.preparationItems()));
            guide.setCaution(seed.caution());
            guide.setAliasesText(joinLines(seed.aliases()));
            guide.setVerifiedOn(seed.verifiedOn());
            guide.setActive(true);
            documentGuideRepository.save(guide);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentGuide> activeGuides() {
        return documentGuideRepository.findByActiveTrueOrderByDisplayNameAsc();
    }

    private List<GuideSeed> readSeeds() {
        try {
            return objectMapper.readValue(
                    new ClassPathResource("document-guides.json").getInputStream(),
                    new TypeReference<>() {
                    }
            );
        } catch (IOException exception) {
            throw new IllegalStateException("서류 발급 가이드 초기 데이터를 읽지 못했습니다.", exception);
        }
    }

    private String joinLines(List<String> values) {
        return values == null
                ? null
                : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + "\n" + right)
                .orElse(null);
    }

    private record GuideSeed(
            String guideKey,
            String displayName,
            DocumentCategory category,
            String issuer,
            String officialUrl,
            String helpName,
            String helpUrl,
            String summary,
            List<String> steps,
            List<String> preparationItems,
            String caution,
            List<String> aliases,
            LocalDate verifiedOn
    ) {
    }
}
