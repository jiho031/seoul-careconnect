package com.seoulcareconnect.dto.policy;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PolicySearchDTO {

    private String keyword;
    private String district;
    private String ageGroup;
    private String category;
    private String targetKeyword;
    private String applyStatus;
    private String sort = "deadline";
    private String quick = "all";
    private Integer page = 0;
    private Integer size;

    public int safePage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int safeSize(int defaultSize, int maxSize) {
        if (size == null || size < 1) return defaultSize;
        return Math.min(size, maxSize);
    }

    public String safeSort() {
        if ("latest".equalsIgnoreCase(sort)) return "latest";
        if ("popular".equalsIgnoreCase(sort)) return "popular";
        return "deadline";
    }

    public String safeQuick() {
        if ("open".equalsIgnoreCase(quick)) return "open";
        if ("new".equalsIgnoreCase(quick)) return "new";
        if ("recent".equalsIgnoreCase(quick)) return "recent";
        return "all";
    }
}
