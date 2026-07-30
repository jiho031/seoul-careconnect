package com.seoulcareconnect.service.report;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Slf4j
@Service
public class ReportPhotoStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final String SUB_DIR = "reports";

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.report-photo.max-count:5}")
    private int maxCount;

    public List<String> storeAll(List<MultipartFile> photos) {
        List<String> storedUrls = new ArrayList<>();

        if (photos == null || photos.isEmpty()) {
            return storedUrls;
        }

        List<MultipartFile> actualFiles = photos.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (actualFiles.isEmpty()) {
            return storedUrls;
        }

        if (actualFiles.size() > maxCount) {
            throw new IllegalArgumentException(
                    "사진은 최대 " + maxCount + "장까지 첨부할 수 있습니다."
            );
        }

        for (MultipartFile file : actualFiles) {
            storedUrls.add(store(file));
        }

        return storedUrls;
    }

    private String store(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "이미지 파일(jpg, png, gif, webp)만 첨부할 수 있습니다."
            );
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : ""
        );
        String extension = resolveExtension(originalName, contentType);
        String storedName = UUID.randomUUID() + extension;

        try {
            Path targetDir = Paths.get(uploadDir, SUB_DIR).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + SUB_DIR + "/" + storedName;
        } catch (IOException e) {
            log.error("신고 첨부 사진 저장 실패: {}", originalName, e);
            throw new IllegalStateException("사진 저장 중 오류가 발생했습니다.", e);
        }
    }

    private String resolveExtension(String originalName, String contentType) {
        if (originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        }

        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
