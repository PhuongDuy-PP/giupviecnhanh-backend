package com.gvn.repository;

import com.gvn.entity.ServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOptionRepository extends JpaRepository<ServiceOption, String> {
    
    /**
     * Find all active options by source and service_id ordered by display_order
     */
    List<ServiceOption> findBySourceAndServiceIdAndIsActiveTrueOrderByDisplayOrderAsc(
            String source, Integer serviceId
    );
}
