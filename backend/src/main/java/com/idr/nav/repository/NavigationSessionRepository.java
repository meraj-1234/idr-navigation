package com.idr.nav.repository;

import com.idr.nav.entity.NavigationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NavigationSessionRepository extends JpaRepository<NavigationSession, String> {
    Optional<NavigationSession> findFirstByStatusOrderByStartTimeDesc(String status);
}
