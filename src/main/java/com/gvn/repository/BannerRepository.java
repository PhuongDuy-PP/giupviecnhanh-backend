package com.gvn.repository;

import com.gvn.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {
    
    /**
     * Find all active banners ordered by display_order ascending
     */
    List<Banner> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    /**
     * Find all banners ordered by display_order ascending
     */
    List<Banner> findAllByOrderByDisplayOrderAsc();
}
