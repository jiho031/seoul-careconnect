package com.seoulcareconnect.repository.user;

import com.seoulcareconnect.entity.user.EmailVerify;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerifyRepository extends JpaRepository<EmailVerify, Long> {

    Optional<EmailVerify> findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email,
            String purpose
    );
}