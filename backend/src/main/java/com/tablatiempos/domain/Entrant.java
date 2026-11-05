package com.tablatiempos.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "entrant")
public class Entrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rally_id", nullable = false)
    public Rally rally;

    public int position;

    @Column(nullable = false, length = 120)
    public String name;

    @Column(length = 160)
    public String car;

    public boolean retired;

    public Integer retiredAtPosition;
}
