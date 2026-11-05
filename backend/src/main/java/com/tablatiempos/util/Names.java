package com.tablatiempos.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Names {

    private Names() {
    }

    /**
     * Clave canónica para agrupar récords y autocompletar:
     * minúsculas, sin acentos y con espacios normalizados.
     */
    public static String key(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
        return normalized;
    }

    public static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
