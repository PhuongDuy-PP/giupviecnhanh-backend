package com.gvn.repository;

import com.gvn.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    /**
     * Find all active categories ordered by display_order ascending
     */
    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();
}
