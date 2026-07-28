// 실제 프로젝트 경로: src/main/java/com/seoulcareconnect/service/user/FavoriteService.java
// [수정] getFavoriteCards()에서 삭제된 정책 카드에 .deleted(true)만 추가했습니다.
// 나머지 메서드/로직은 전부 기존 그대로입니다.
package com.seoulcareconnect.service.user;

import com.seoulcareconnect.dto.policy.PolicyDTO;
import com.seoulcareconnect.dto.policy.PolicySearchDTO;
import com.seoulcareconnect.dto.user.FavoriteCardDto;
import com.seoulcareconnect.dto.user.FavoriteDetailDto;
import com.seoulcareconnect.entity.user.Favorite;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.mapper.policy.PolicyMapper;
import com.seoulcareconnect.repository.user.FavoriteRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.service.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PolicyRepository policyRepository;
    private final PolicyMapper policyMapper;
    private final PolicyService policyService;

    // Policy 실제 데이터를 조회해서 title/category/summary 채움 (마이페이지 요약용)
    public List<FavoriteDetailDto> getFavoritesWithDetails(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        return favorites.stream().map(f -> {
            Policy policy = policyRepository.findById(f.getPolicyId()).orElse(null);

            if (policy == null) {
                return FavoriteDetailDto.builder()
                        .favoriteId(f.getId())
                        .policyId(f.getPolicyId())
                        .title("삭제된 정책입니다")
                        .category("미분류")
                        .summary(null)
                        .createdAt(f.getCreatedAt())
                        .build();
            }

            PolicyDTO dto = policyMapper.toDto(policy);

            return FavoriteDetailDto.builder()
                    .favoriteId(f.getId())
                    .policyId(f.getPolicyId())
                    .title(dto.getTitle())
                    .category(dto.getCategoryLabel())
                    .summary(dto.getSummary())
                    .createdAt(f.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    // 관심 정책 페이지(/favorites)용 상세 카드 목록
    public List<FavoriteCardDto> getFavoriteCards(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        return favorites.stream().map(f -> {
            Policy policy = policyRepository.findById(f.getPolicyId()).orElse(null);

            if (policy == null) {
                return FavoriteCardDto.builder()
                        .favoriteId(f.getId())
                        .policyId(f.getPolicyId())
                        .title("삭제된 정책입니다")
                        .categoryLabel("미분류")
                        .favoritedAt(f.getCreatedAt())
                        .deleted(true) // [추가]
                        .build();
            }

            PolicyDTO dto = policyMapper.toDto(policy);

            return FavoriteCardDto.builder()
                    .favoriteId(f.getId())
                    .policyId(f.getPolicyId())
                    .title(dto.getTitle())
                    .categoryLabel(dto.getCategoryLabel())
                    .summary(dto.getSummary())
                    .target(dto.getTarget())
                    .ageGroupDisplay(dto.getAgeGroupDisplay())
                    .regionDisplay(dto.getRegionDisplay())
                    .applyStatusLabel(dto.getApplyStatusLabel())
                    .applyPeriod(dto.getApplyPeriod())
                    .dDayLabel(dto.getDDayLabel())
                    .dDayCssClass(dto.getDDayCssClass())
                    .favoritedAt(f.getCreatedAt())
                    .deleted(false) // [추가]
                    .build();
        }).collect(Collectors.toList());
    }

    // 찜한 정책들의 카테고리를 기반으로 관련 정책 추천 (부족하면 인기 정책으로 보충)
    public List<PolicyDTO> getRecommendedPolicies(Long userId, int limit) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        Set<Long> favoritedIds = favorites.stream()
                .map(Favorite::getPolicyId)
                .collect(Collectors.toSet());

        // 찜한 정책들의 카테고리 집계
        Map<String, Long> categoryCounts = favorites.stream()
                .map(f -> policyRepository.findById(f.getPolicyId()).orElse(null))
                .filter(Objects::nonNull)
                .map(policy -> policyMapper.toDto(policy).getCategory())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        List<PolicyDTO> recommended = new ArrayList<>();

        // 가장 많이 찜한 카테고리 찾기
        String topCategory = categoryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (topCategory != null) {
            PolicySearchDTO searchDTO = new PolicySearchDTO();
            searchDTO.setCategory(topCategory);
            searchDTO.setSize(limit + favoritedIds.size());

            recommended = policyService.search(searchDTO).getContent().stream()
                    .filter(p -> !favoritedIds.contains(p.getPolicyId()))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        // 같은 카테고리 정책이 부족하면(또는 찜한 정책이 없으면) 인기 정책으로 채움
        if (recommended.size() < limit) {
            Set<Long> excludeIds = recommended.stream()
                    .map(PolicyDTO::getPolicyId)
                    .collect(Collectors.toSet());
            excludeIds.addAll(favoritedIds);

            List<PolicyDTO> fillers = policyService.popular(limit + excludeIds.size()).stream()
                    .filter(p -> !excludeIds.contains(p.getPolicyId()))
                    .limit(limit - recommended.size())
                    .collect(Collectors.toList());

            recommended.addAll(fillers);
        }

        return recommended;
    }

    @Transactional
    public void addFavorite(Long userId, Long policyId) {
        if (favoriteRepository.existsByUserIdAndPolicyId(userId, policyId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 관심 정책으로 등록된 정책입니다.");
        }

        Favorite favorite = Favorite.builder()
                .userId(userId)
                .policyId(policyId)
                .build();
        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long userId, Long policyId) {
        favoriteRepository.deleteByUserIdAndPolicyId(userId, policyId);
    }
}
