package com.quickbite.auth.authservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.auth.authservice.entity.PendingLogin;

@Repository
public interface PendingLoginRepository extends JpaRepository<PendingLogin, String> {

    List<PendingLogin> findAllByUserId(Long userId);

    void deleteAllByExpiresAtBefore(LocalDateTime threshold);
}
