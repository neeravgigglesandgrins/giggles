package com.giggles.auth.repository;

import com.giggles.auth.entity.AdminSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminSessionRepository extends JpaRepository<AdminSessionEntity, Long> {
    
    Optional<AdminSessionEntity> findByToken(String token);
    
    boolean existsByToken(String token);
}

