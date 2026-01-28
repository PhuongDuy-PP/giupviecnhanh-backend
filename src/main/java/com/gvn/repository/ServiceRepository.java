package com.gvn.repository;

import com.gvn.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {
    
    /**
     * Find all active services by category ordered by name
     */
    List<Service> findByCategoryIdAndIsActiveTrueOrderByNameAsc(Integer categoryId);
    
    /**
     * Find all active services
     */
    List<Service> findByIsActiveTrueOrderByNameAsc();
    
    /**
     * Find service by code
     */
    Optional<Service> findByCode(String code);
}
