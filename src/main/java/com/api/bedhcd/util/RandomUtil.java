package com.api.bedhcd.util;

import java.security.SecureRandom;
import java.util.function.Predicate;

public class RandomUtil {
    private static final SecureRandom random = new SecureRandom();

    public static Long generate6DigitId(Predicate<Long> existsCheck) {
        Long id;
        do {
            id = 100000L + random.nextInt(900000);
        } while (existsCheck.test(id));
        return id;
    }
}
