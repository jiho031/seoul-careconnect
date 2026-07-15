package com.seoulcareconnect.repository.user;

import com.seoulcareconnect.entity.user.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    // 기본적인 CRUD 메서드(save, findById, findAll 등)가 JpaRepository에 포함되어 있습니다.
}