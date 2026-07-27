package com.seoulcareconnect.repository.user;

import com.seoulcareconnect.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(
            String provider,
            String providerId
    );

    long countByCreatedAtBetween(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    long countByIsActiveFalse();

    long countByRole(String role);
}