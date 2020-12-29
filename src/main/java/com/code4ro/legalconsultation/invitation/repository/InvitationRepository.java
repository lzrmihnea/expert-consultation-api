package com.code4ro.legalconsultation.invitation.repository;

import com.code4ro.legalconsultation.invitation.model.persistence.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    Optional<Invitation> findByCode(String code);
    void deleteByUserId(UUID userId);
}
