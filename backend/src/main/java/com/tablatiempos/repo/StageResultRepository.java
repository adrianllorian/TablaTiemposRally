package com.tablatiempos.repo;

import com.tablatiempos.domain.StageResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageResultRepository extends JpaRepository<StageResult, Long> {

    List<StageResult> findByRallyId(Long rallyId);

    List<StageResult> findByRallyIdAndEntrantId(Long rallyId, Long entrantId);

    Optional<StageResult> findByStageIdAndEntrantId(Long stageId, Long entrantId);

    long countByRallyId(Long rallyId);

    void deleteByStageId(Long stageId);

    void deleteByEntrantId(Long entrantId);

    void deleteByRallyId(Long rallyId);
}
