package com.tablatiempos.repo;

import com.tablatiempos.domain.Rally;
import com.tablatiempos.domain.RallyStatus;
import com.tablatiempos.dto.RallyRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RallyRepository extends JpaRepository<Rally, Long> {

    Optional<Rally> findFirstByStatusOrderByCreatedAtDesc(RallyStatus status);

    Optional<Rally> findByInviteToken(String inviteToken);

    List<Rally> findAllByOrderByCreatedAtDesc();

    @Query("select new com.tablatiempos.dto.RallyRef(r.id, r.name, r.date) from Rally r")
    List<RallyRef> findAllRefs();
}
