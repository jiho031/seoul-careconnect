package com.seoulcareconnect.integration.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalPolicyDocument {
    private String text;
    private String evidence;
    private String sourceType;
    private String attachmentName;
    private String downloadUrl;
    private Integer confidence;
}
