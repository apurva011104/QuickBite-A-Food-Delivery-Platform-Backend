package com.quickbite.auth.authservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.auth.authservice.entity.PendingRegistration;

@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {

    List<PendingRegistration> findAllByEmailOrPhoneNumber(String email, String phoneNumber);

    void deleteAllByExpiresAtBefore(LocalDateTime threshold);
}
