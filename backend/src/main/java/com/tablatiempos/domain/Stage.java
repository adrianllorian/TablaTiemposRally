package com.tablatiempos.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "stage")
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rally_id", nullable = false)
    public Rally rally;

    public int position;

    @Column(length = 160)
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public StageType type = StageType.TC;

    public String code() {
        return type == StageType.SHAKEDOWN ? "SD" : "TC-" + position;
    }
}
