package com.gvn.repository;

import com.gvn.entity.PartnerService;
import com.gvn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerServiceRepository extends JpaRepository<PartnerService, UUID> {
    
    /**
     * Find all active partner services by user
     */
    List<PartnerService> findByUserAndIsActiveTrue(User user);
    
    /**
     * Find partner service by user and service
     */
    boolean existsByUserAndServiceIdAndIsActiveTrue(User user, Integer serviceId);
    
    /**
     * Find all partner services by service
     */
    List<PartnerService> findByServiceId(Integer serviceId);
}
