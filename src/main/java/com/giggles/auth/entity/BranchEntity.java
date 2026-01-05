package com.giggles.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "branches")
public class BranchEntity extends BaseEntity {
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "city", nullable = false)
    private String city;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

