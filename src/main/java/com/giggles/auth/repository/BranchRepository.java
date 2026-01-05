package com.giggles.auth.repository;

import com.giggles.auth.entity.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, Long> {
    
    List<BranchEntity> findByIsActiveTrueAndDeletedFalse();
    
    Optional<BranchEntity> findByIdAndDeletedFalse(Long id);
    
    List<BranchEntity> findByCityAndIsActiveTrueAndDeletedFalse(String city);
}

