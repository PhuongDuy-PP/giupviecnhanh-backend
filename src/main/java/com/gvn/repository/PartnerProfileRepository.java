package com.gvn.repository;

import com.gvn.entity.PartnerProfile;
import com.gvn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnerProfileRepository extends JpaRepository<PartnerProfile, UUID> {
    Optional<PartnerProfile> findByUser(User user);
}

