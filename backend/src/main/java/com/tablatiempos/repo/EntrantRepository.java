package com.tablatiempos.repo;

import com.tablatiempos.domain.Entrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EntrantRepository extends JpaRepository<Entrant, Long> {

    List<Entrant> findByRallyIdOrderByPositionAsc(Long rallyId);

    @Query("select distinct e.name from Entrant e " +
            "where e.name is not null and trim(e.name) <> '' order by e.name")
    List<String> findDistinctNames();

    @Query("select distinct e.car from Entrant e " +
            "where e.car is not null and trim(e.car) <> '' order by e.car")
    List<String> findDistinctCars();
}
