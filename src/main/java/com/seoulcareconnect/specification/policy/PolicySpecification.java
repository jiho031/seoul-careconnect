package com.seoulcareconnect.specification.policy;

import com.seoulcareconnect.dto.policy.PolicySearchDTO;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.enums.AgeGroup;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class PolicySpecification {

    private PolicySpecification() {
    }

    public static Specification<Policy> build(PolicySearchDTO search, LocalDate today, int newDays) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(root.get("status").in(
                    PolicyStatus.AUTO_PUBLISHED,
                    PolicyStatus.APPROVED
            ));
            predicates.add(cb.notEqual(root.get("applyStatus"), ApplyStatus.EXPIRED));
            predicates.add(cb.or(
                    cb.isNull(root.get("startDate")),
                    cb.lessThanOrEqualTo(root.get("startDate"), today)
            ));
            predicates.add(cb.or(
                    cb.isNull(root.get("endDate")),
                    cb.greaterThanOrEqualTo(root.get("endDate"), today)
            ));

            String keyword = normalize(search.getKeyword());
            if (keyword != null) {
                String pattern = likePattern(keyword);
                var source = root.join("source", JoinType.LEFT);
                var detail = root.join("detail", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("target")), pattern),
                        cb.like(cb.lower(root.get("contact")), pattern),
                        cb.like(cb.lower(root.get("applyMethod")), pattern),
                        cb.like(cb.lower(source.get("sourceName")), pattern),
                        cb.like(cb.lower(detail.get("benefit")), pattern),
                        cb.like(cb.lower(detail.get("selectionCriteria")), pattern),
                        cb.like(cb.lower(detail.get("contentText")), pattern)
                ));
            }

            String district = normalize(search.getDistrict());
            if (district != null) {
                if ("서울시 전체".equals(district)) {
                    predicates.add(cb.or(
                            cb.isNull(root.get("district")),
                            cb.equal(root.get("district"), "")
                    ));
                } else {
                    predicates.add(cb.or(
                            cb.equal(root.get("district"), district),
                            cb.isNull(root.get("district")),
                            cb.equal(root.get("district"), "")
                    ));
                }
            }

            parseEnum(PolicyCategory.class, search.getCategory())
                    .ifPresent(value -> predicates.add(cb.equal(root.get("category"), value)));

            parseEnum(AgeGroup.class, search.getAgeGroup())
                    .filter(value -> value != AgeGroup.ALL)
                    .ifPresent(value -> {
                        String labelPattern = likePattern(value.getLabel());
                        String allPattern = likePattern(AgeGroup.ALL.getLabel());
                        var detail = root.join("detail", JoinType.LEFT);
                        predicates.add(cb.or(
                                cb.like(cb.lower(root.get("target")), labelPattern),
                                cb.like(cb.lower(root.get("target")), allPattern),
                                cb.like(cb.lower(detail.get("selectionCriteria")), labelPattern)
                        ));
                    });

            String targetKeyword = normalize(search.getTargetKeyword());
            if (targetKeyword != null) {
                String pattern = likePattern(targetKeyword);
                var detail = root.join("detail", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("target")), pattern),
                        cb.like(cb.lower(detail.get("selectionCriteria")), pattern),
                        cb.like(cb.lower(detail.get("contentText")), pattern)
                ));
            }

            parseEnum(ApplyStatus.class, search.getApplyStatus())
                    .filter(value -> value != ApplyStatus.EXPIRED)
                    .ifPresent(value -> predicates.add(cb.equal(root.get("applyStatus"), value)));

            switch (search.safeQuick()) {
                case "open" -> predicates.add(root.get("applyStatus").in(
                        ApplyStatus.OPEN,
                        ApplyStatus.CLOSING_SOON,
                        ApplyStatus.ALWAYS
                ));
                case "new" -> predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        today.minusDays(Math.max(newDays, 0)).atStartOfDay()
                ));
                default -> {
                }
            }

            if ("deadline".equals(search.safeSort()) && !isCountQuery(query.getResultType())) {
                query.orderBy(
                        cb.asc(cb.selectCase().when(cb.isNull(root.get("endDate")), 1).otherwise(0)),
                        cb.asc(root.get("endDate")),
                        cb.desc(root.get("createdAt"))
                );
            }

            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean isCountQuery(Class<?> resultType) {
        return resultType == Long.class || resultType == long.class;
    }

    private static String likePattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.isBlank() ? null : result;
    }

    private static <E extends Enum<E>> Optional<E> parseEnum(Class<E> type, String value) {
        String normalized = normalize(value);
        if (normalized == null) return Optional.empty();
        try {
            return Optional.of(Enum.valueOf(type, normalized.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
