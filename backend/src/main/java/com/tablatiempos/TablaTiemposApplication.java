package com.tablatiempos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TablaTiemposApplication {

    public static void main(String[] args) {
        Path dataDir = resolverCarpetaDatos();
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            System.err.println("No se pudo crear la carpeta de datos " + dataDir + ": " + e.getMessage());
        }
        System.setProperty("app.data.dir", dataDir.toAbsolutePath().toString().replace('\\', '/'));
        SpringApplication.run(TablaTiemposApplication.class, args);
    }

    private static Path resolverCarpetaDatos() {
        String override = System.getProperty("app.data.dir");
        if (override == null || override.isBlank()) {
            override = System.getenv("APP_DATA_DIR");
        }
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }

        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            for (String entrada : classpath.split(File.pathSeparator)) {
                if (entrada.toLowerCase().endsWith(".jar")) {
                    Path jar = Paths.get(entrada).toAbsolutePath().normalize();
                    if (Files.isRegularFile(jar)) {
                        return jar.getParent().resolve("data");
                    }
                }
            }
        }

        try {
            Path code = Paths.get(TablaTiemposApplication.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            if (Files.isRegularFile(code)) {
                return code.getParent().resolve("data");
            }
        } catch (Exception ignored) {
            // En desarrollo (target/classes) se usa la carpeta de trabajo.
        }

        return Paths.get(System.getProperty("user.dir"), "data").toAbsolutePath().normalize();
    }
}
