package com.quickbite.auth.authservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.auth.authservice.entity.Role;
import com.quickbite.auth.authservice.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findById(Long userId);

    List<User> findAllByRole(Role role);
    
    List<User> findByNameContainingIgnoreCase(String name);
    
    void deleteById(Long userId);

}
