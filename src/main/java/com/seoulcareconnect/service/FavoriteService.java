package com.seoulcareconnect.service.user;

import com.seoulcareconnect.dto.user.FavoriteDetailDto;
import com.seoulcareconnect.entity.user.Favorite;
import com.seoulcareconnect.entity.user.Policy;
import com.seoulcareconnect.repository.user.FavoriteRepository;
import com.seoulcareconnect.repository.user.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PolicyRepository policyRepository; // [수정] Policy 조회를 위해 추가

    // [수정] Policy 실제 데이터를 조회해서 title/category/summary 채움
    public List<FavoriteDetailDto> getFavoritesWithDetails(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        return favorites.stream().map(f -> {
            Policy policy = policyRepository.findById(f.getPolicyId()).orElse(null);

            return FavoriteDetailDto.builder()
                    .favoriteId(f.getId())
                    .policyId(f.getPolicyId())
                    .title(policy != null ? policy.getTitle() : "삭제된 정책입니다")
                    .category(policy != null ? policy.getCategory() : "미분류")
                    .summary(policy != null ? policy.getSummary() : null)
                    .createdAt(f.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void addFavorite(Long userId, Long policyId) {
        // [수정] 이미 찜한 정책이면 409 Conflict 던짐 (프론트 JS가 이 응답을 기대함)
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
