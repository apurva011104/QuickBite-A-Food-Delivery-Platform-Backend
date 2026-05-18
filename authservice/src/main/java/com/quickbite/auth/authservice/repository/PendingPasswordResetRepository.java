package com.quickbite.auth.authservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.auth.authservice.entity.PendingPasswordReset;

@Repository
public interface PendingPasswordResetRepository extends JpaRepository<PendingPasswordReset, String> {

    List<PendingPasswordReset> findAllByUserId(Long userId);

    void deleteAllByExpiresAtBefore(LocalDateTime threshold);
}
