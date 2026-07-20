package com.seoulcareconnect.service.impl.policy;

import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.RawCollectedItem;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.entity.policy.enums.RawType;
import com.seoulcareconnect.integration.policy.ExternalDateParser;
import com.seoulcareconnect.integration.policy.ExternalPolicyClassifier;
import com.seoulcareconnect.integration.policy.ExternalPolicyItem;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.policy.RawCollectedItemRepository;
import com.seoulcareconnect.integration.policy.SeoulPolicyFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyUpsertService {

    private final PolicyRepository policyRepository;
    private final RawCollectedItemRepository rawRepository;
    private final ExternalDateParser dateParser;
    private final ExternalPolicyClassifier classifier;
    private final SeoulPolicyFilter seoulPolicyFilter;

    @Value("${app.policy.sync.zone-id:Asia/Seoul}")
    private String zoneId;

    @Value("${app.policy.query.closing-soon-days:7}")
    private int closingSoonDays;

    @Value("${app.policy.collect.exclude-expired:true}")
    private boolean excludeExpired;

    @Value("${app.policy.collect.include-no-end-date:true}")
    private boolean includeNoEndDate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean upsert(PolicySource source, ExternalPolicyItem item) {
        String externalId = normalizeSingleLine(item.getExternalId());
        if (externalId == null) externalId = generatedExternalId(item);
        externalId = limit(externalId, 100);

        Policy existing = policyRepository
                .findFirstBySource_SourceIdAndExternalId(source.getSourceId(), externalId)
                .orElse(null);

        boolean newPolicy = existing == null;

        RawCollectedItem rawItem = saveRawIfChanged(source, item, externalId);

        if (isExpired(item)) {
            if (existing != null) {
                existing.setApplyStatus(ApplyStatus.EXPIRED);
                existing.setStatus(PolicyStatus.EXPIRED);
                policyRepository.save(existing);
            }
            return false;
        }

        if (item.getStartDate() != null && item.getStartDate().isAfter(today())) {
            return false;
        }

        boolean always = dateParser.containsAlwaysText(
                item.getStatusText(),
                item.getApplyMethod(),
                item.getContentText()
        );
        if (!always && item.getEndDate() == null && !includeNoEndDate) {
            return false;
        }

        Policy policy = existing == null ? new Policy() : existing;
        policy.setSource(source);
        policy.setRawItem(rawItem);
        policy.setExternalId(externalId);
        policy.setTitle(limit(valueOr(item.getTitle(), "제목 확인 필요"), 300));
        policy.setCategory(classifier.category(
                item.getCategory() == null ? PolicyCategory.LIVING_SUPPORT : item.getCategory(),
                item.getTitle(),
                item.getSummary(),
                item.getTarget(),
                item.getBenefit(),
                item.getContentText()
        ));

        String searchableTarget = classifier.searchableTarget(
                item.getTarget(),
                item.getTitle(),
                item.getBenefit(),
                item.getSelectionCriteria(),
                item.getContentText()
        );
        policy.setTarget(limit(normalizeSingleLine(searchableTarget), 300));
        policy.setRegion(limit(seoulPolicyFilter.resolveRegion(source.getSourceName(), item), 50));
        policy.setDistrict(limit(normalizeSingleLine(item.getDistrict()), 50));
        policy.setStartDate(item.getStartDate());
        policy.setEndDate(item.getEndDate());
        policy.setApplyStatus(resolveApplyStatus(item));
        policy.setApplyMethod(limit(normalizeSingleLine(item.getApplyMethod()), 500));
        policy.setOfficialUrl(limit(safeUrl(item.getOfficialUrl()), 1000));
        policy.setContact(limit(buildContact(item), 200));
        policy.setStatus(resolvePolicyStatus(item, existing == null ? null : existing.getStatus()));

        if (existing == null) policy.setViewCount(0);

        PolicyDetail detail = policy.getDetail();
        if (detail == null) detail = new PolicyDetail();

        detail.setBenefit(normalizeText(firstNonBlank(
                item.getBenefit(),
                item.getSummary()
        )));
        detail.setSelectionCriteria(normalizeText(item.getSelectionCriteria()));
        detail.setRequiredDocumentsText(normalizeText(item.getRequiredDocumentsText()));
        detail.setContentText(normalizeText(item.getContentText()));
        detail.setContentHtml(null);
        policy.attachDetail(detail);

        policyRepository.save(policy);

        log.info(
                "{} 정책 처리 완료: source={}, externalId={}, title={}",
                newPolicy ? "신규 저장" : "기존 갱신",
                source.getSourceName(),
                externalId,
                policy.getTitle()
        );

        return newPolicy;
    }

    private boolean isExpired(ExternalPolicyItem item) {
        if (!excludeExpired) return false;
        if (item.getEndDate() != null && item.getEndDate().isBefore(today())) return true;

        String status = normalizeSingleLine(item.getStatusText());
        if (status == null) return false;

        String compact = status.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return compact.equals("마감")
                || compact.contains("모집마감")
                || compact.contains("접수종료")
                || compact.contains("신청종료")
                || compact.contains("모집완료")
                || compact.equals("종료")
                || compact.equals("n");
    }

    private ApplyStatus resolveApplyStatus(ExternalPolicyItem item) {
        if (!item.isApplicationInfoAvailable()) {
            return ApplyStatus.INFORMATION_ONLY;
        }

        if (dateParser.containsAlwaysText(
                item.getStatusText(),
                item.getApplyMethod(),
                item.getContentText()
        )) {
            return ApplyStatus.ALWAYS;
        }

        LocalDate endDate = item.getEndDate();
        if (endDate == null) return ApplyStatus.OPEN;

        long days = java.time.temporal.ChronoUnit.DAYS.between(today(), endDate);
        if (days < 0) return ApplyStatus.EXPIRED;
        if (days <= Math.max(closingSoonDays, 1)) return ApplyStatus.CLOSING_SOON;
        return ApplyStatus.OPEN;
    }

    private PolicyStatus resolvePolicyStatus(ExternalPolicyItem item, PolicyStatus existingStatus) {
        if (existingStatus == PolicyStatus.APPROVED
                || existingStatus == PolicyStatus.REJECTED) {
            return existingStatus;
        }

        boolean hasTitle = normalizeSingleLine(item.getTitle()) != null;
        boolean hasUsefulContent = safeUrl(item.getOfficialUrl()) != null
                || normalizeText(item.getContentText()) != null
                || normalizeText(item.getBenefit()) != null
                || normalizeText(item.getSummary()) != null;

        return hasTitle && hasUsefulContent
                ? PolicyStatus.AUTO_PUBLISHED
                : PolicyStatus.PENDING_REVIEW;
    }

    private RawCollectedItem saveRawIfChanged(
            PolicySource source,
            ExternalPolicyItem item,
            String externalId
    ) {
        String rawPayload = firstNonBlank(
                item.getRawJson(),
                item.getRawXml(),
                item.getContentText()
        );

        String contentHash = sha256(
                source.getSourceName()
                        + "|"
                        + externalId
                        + "|"
                        + valueOr(rawPayload, "")
        );

        return rawRepository
                .findFirstBySource_SourceIdAndContentHashOrderByCollectedAtDesc(
                        source.getSourceId(),
                        contentHash
                )
                .orElseGet(() -> {
                    RawCollectedItem raw = new RawCollectedItem();
                    raw.setSource(source);
                    raw.setRawType(RawType.API);
                    raw.setExternalId(externalId);
                    raw.setSourceUrl(limit(safeUrl(item.getOfficialUrl()), 1000));
                    raw.setRawJson(item.getRawJson());
                    raw.setRawXml(item.getRawXml());
                    raw.setRawText(normalizeText(item.getContentText()));
                    raw.setHttpStatus(item.getHttpStatus());
                    raw.setContentHash(contentHash);
                    return rawRepository.save(raw);
                });
    }

    private String buildContact(ExternalPolicyItem item) {
        String agency = normalizeSingleLine(item.getAgencyName());
        String contact = normalizeSingleLine(item.getContact());
        if (agency != null && contact != null && !contact.contains(agency)) {
            return agency + " | " + contact;
        }
        return firstNonBlank(agency, contact);
    }

    private String generatedExternalId(ExternalPolicyItem item) {
        String officialUrl = safeUrl(item.getOfficialUrl());
        String stableValue;

        if (officialUrl != null) {
            stableValue = officialUrl;
        } else {
            stableValue = String.join("|",
                    valueOr(normalizeSingleLine(item.getAgencyName()), ""),
                    valueOr(normalizeSingleLine(item.getTitle()), "")
            );
        }

        return sha256(
                valueOr(item.getSourceName(), "")
                        + "|"
                        + stableValue
        ).substring(0, 40);
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of(zoneId));
    }

    private String safeUrl(String value) {
        String normalized = normalizeSingleLine(value);
        if (normalized == null) return null;
        return normalized.startsWith("http://") || normalized.startsWith("https://")
                ? normalized
                : null;
    }

    private String normalizeSingleLine(String value) {
        if (value == null) return null;
        String result = value.replaceAll("\\s+", " ").trim();
        return result.isBlank() ? null : result;
    }

    private String normalizeText(String value) {
        if (value == null) return null;
        String result = value
                .replaceAll("[ \\t\\r\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return result.isBlank() ? null : result;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalizeText(value);
            if (normalized != null) return normalized;
        }
        return null;
    }

    private String valueOr(String value, String fallback) {
        String normalized = normalizeText(value);
        return normalized == null ? fallback : normalized;
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 해시 생성 실패", e);
        }
    }
}
