package com.api.bedhcd.shared.domain;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

public class UuidFactory {
    private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

    /**
     * Generate a UUID v7 (time-ordered), optimized for B-tree indexing.
     * @return String representation of UUID v7
     */
    public static String generate() {
        return GENERATOR.generate().toString();
    }
}
