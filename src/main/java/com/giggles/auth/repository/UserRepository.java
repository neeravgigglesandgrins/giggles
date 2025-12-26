package com.giggles.auth.repository;

import com.giggles.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    Optional<UserEntity> findByPhoneNumberAndDeletedFalse(String phoneNumber);
    
    Optional<UserEntity> findByEmailAndDeletedFalse(String email);
    
    boolean existsByPhoneNumberAndDeletedFalse(String phoneNumber);
    
    boolean existsByEmailAndDeletedFalse(String email);
}

