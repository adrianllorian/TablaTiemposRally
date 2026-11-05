package com.tablatiempos.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_settings")
public class AppSettings {

    @Id
    public Long id = 1L;

    @Column(nullable = false, length = 200)
    public String passwordHash;

    @Column(nullable = false, length = 160)
    public String appName = "Tabla Tiempos Rally";

    @Column(length = 200)
    public String logoFileName;

    @Column(length = 100)
    public String logoContentType;

    public boolean setupComplete;

    public Instant updatedAt = Instant.now();
}
