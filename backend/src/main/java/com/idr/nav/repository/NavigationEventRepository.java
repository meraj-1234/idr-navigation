package com.idr.nav.repository;

import com.idr.nav.entity.NavigationEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NavigationEventRepository extends JpaRepository<NavigationEventEntity, Long> {
    List<NavigationEventEntity> findTop100BySessionIdOrderByTimestampDesc(String sessionId);
    List<NavigationEventEntity> findTop50ByOrderByTimestampDesc();
}
