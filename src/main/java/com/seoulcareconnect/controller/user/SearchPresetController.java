package com.seoulcareconnect.controller.user;

import com.seoulcareconnect.dto.user.SearchPresetRequest;
import com.seoulcareconnect.dto.user.SearchPresetResponse;
import com.seoulcareconnect.dto.user.MyPageResponse;
import com.seoulcareconnect.service.user.SearchPresetService;
import com.seoulcareconnect.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search-presets")
public class SearchPresetController {

    private final SearchPresetService searchPresetService;
    private final UserService userService;

    @GetMapping
    public List<SearchPresetResponse> findAll(Authentication authentication) {
        return searchPresetService.findAll(currentUserId(authentication));
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody SearchPresetRequest request,
            Authentication authentication
    ) {
        try {
            SearchPresetResponse preset = searchPresetService.create(currentUserId(authentication), request);
            return ResponseEntity.ok(preset);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{presetId}")
    public ResponseEntity<?> delete(
            @PathVariable Long presetId,
            Authentication authentication
    ) {
        try {
            searchPresetService.delete(currentUserId(authentication), presetId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Long currentUserId(Authentication authentication) {
        MyPageResponse user = userService.getMyPage(authentication);
        return user.getUserId();
    }
}
