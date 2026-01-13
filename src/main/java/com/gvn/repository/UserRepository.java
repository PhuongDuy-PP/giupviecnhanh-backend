package com.gvn.repository;

import com.gvn.entity.User;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Use native query to bypass all JPA caching mechanisms
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "false"),
        @QueryHint(name = "jakarta.persistence.cache.retrieveMode", value = "BYPASS"),
        @QueryHint(name = "jakarta.persistence.cache.storeMode", value = "BYPASS")
    })
    @Query(value = "SELECT * FROM users WHERE phone_number = :phoneNumber", nativeQuery = true)
    Optional<User> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    // Use native query with COUNT to ensure direct database query
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "false"),
        @QueryHint(name = "jakarta.persistence.cache.retrieveMode", value = "BYPASS"),
        @QueryHint(name = "jakarta.persistence.cache.storeMode", value = "BYPASS")
    })
    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE phone_number = :phoneNumber)", nativeQuery = true)
    boolean existsByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    // Native query delete to ensure immediate database deletion
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM users WHERE phone_number = :phoneNumber", nativeQuery = true)
    void deleteByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    Optional<User> findByEmail(String email);
}

