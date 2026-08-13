package com.api.bedhcd.shared.util;

public final class CccdUtil {

    private CccdUtil() {
    }

    public static String normalizeCccd(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
