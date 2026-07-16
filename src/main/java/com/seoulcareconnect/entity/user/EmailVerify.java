package com.seoulcareconnect.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "email_verify")
public class EmailVerify {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long verifyId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 10)
    private String verifyCode;

    @Column(nullable = false, length = 30)
    private String purpose = "SIGNUP";

    @Column(nullable = false)
    private Integer attemptCount = 0;

    @Column(nullable = false)
    private Boolean verifiedYn = false;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.purpose == null) this.purpose = "SIGNUP";
        if (this.attemptCount == null) this.attemptCount = 0;
        if (this.verifiedYn == null) this.verifiedYn = false;
    }
}