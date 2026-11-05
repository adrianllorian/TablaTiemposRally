package com.tablatiempos.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stage_result", uniqueConstraints = @UniqueConstraint(
        name = "uk_stage_entrant", columnNames = {"stage_id", "entrant_id"}))
public class StageResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "rally_id", nullable = false)
    public Long rallyId;

    @Column(name = "stage_id", nullable = false)
    public Long stageId;

    @Column(name = "entrant_id", nullable = false)
    public Long entrantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ResultKind kind = ResultKind.VALID;

    public Long millis;

    public Instant updatedAt = Instant.now();
}
