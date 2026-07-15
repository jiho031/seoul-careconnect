package com.seoulcareconnect.controller.user;

import com.seoulcareconnect.dto.user.FavoriteDetailDto;
import com.seoulcareconnect.service.user.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 1. 관심 정책 추가
    @PostMapping("/{policyId}")
    public ResponseEntity<Void> addFavorite(@PathVariable Long userId, @PathVariable Long policyId) {
        favoriteService.addFavorite(userId, policyId);
        return ResponseEntity.ok().build();
    }

    // 2. 관심 정책 삭제
    @DeleteMapping("/{policyId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long userId, @PathVariable Long policyId) {
        favoriteService.removeFavorite(userId, policyId);
        return ResponseEntity.ok().build();
    }

    // 3. 관심 정책 상세 목록 조회 (마이페이지용)
    @GetMapping
    public ResponseEntity<List<FavoriteDetailDto>> getFavorites(@PathVariable Long userId) {
        return ResponseEntity.ok(favoriteService.getFavoritesWithDetails(userId));
    }
}