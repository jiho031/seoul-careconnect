package com.seoulcareconnect.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "`policy`")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;      // 정책명

    private String agency;     // 주관기관

    @Column(length = 1000)
    private String summary;    // 요약 내용

    private String category;   // 카테고리 (예: 주거, 일자리 등)

    private String region;     // 지역

    private String status;     // 상태 (진행중, 마감 등)
}