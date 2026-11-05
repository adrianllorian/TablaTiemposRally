package com.tablatiempos.repo;

import com.tablatiempos.domain.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StageRepository extends JpaRepository<Stage, Long> {

    List<Stage> findByRallyIdOrderByPositionAsc(Long rallyId);

    @Query("select distinct s.name from Stage s " +
            "where s.name is not null and trim(s.name) <> '' order by s.name")
    List<String> findDistinctNames();
}
