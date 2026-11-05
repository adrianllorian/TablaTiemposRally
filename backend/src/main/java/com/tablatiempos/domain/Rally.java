package com.tablatiempos.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rally")
public class Rally {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 160)
    public String name;

    public LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public RallyStatus status = RallyStatus.ACTIVE;

    public boolean shakedownEnabled;

    @Column(length = 160)
    public String shakedownName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public PenaltyMode penaltyMode = PenaltyMode.WORST_PLUS;

    public long penaltyValueMs = 30_000L;

    @Column(unique = true, length = 64)
    public String inviteToken;

    public long version = 0L;

    public Instant createdAt = Instant.now();

    public Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "rally", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    public List<Stage> stages = new ArrayList<>();

    @OneToMany(mappedBy = "rally", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    public List<Entrant> entrants = new ArrayList<>();

    public Stage shakedown() {
        return stages.stream().filter(s -> s.type == StageType.SHAKEDOWN).findFirst().orElse(null);
    }

    public List<Stage> tcStages() {
        return stages.stream().filter(s -> s.type == StageType.TC).toList();
    }
}
