package com.idr.nav.repository;

import com.idr.nav.entity.EstimatedPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstimatedPositionRepository extends JpaRepository<EstimatedPositionEntity, Long> {
    List<EstimatedPositionEntity> findTop500BySessionIdOrderByTimestampAsc(String sessionId);
    List<EstimatedPositionEntity> findTop100BySessionIdOrderByTimestampDesc(String sessionId);
}
