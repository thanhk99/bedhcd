package com.api.bedhcd.util;

import java.security.SecureRandom;
import java.util.function.Predicate;

public class RandomUtil {
    private static final SecureRandom random = new SecureRandom();

    public static String generate6DigitId(Predicate<String> existsCheck) {
        String id;
        do {
            id = String.valueOf(100000 + random.nextInt(900000));
        } while (existsCheck.test(id));
        return id;
    }
}
