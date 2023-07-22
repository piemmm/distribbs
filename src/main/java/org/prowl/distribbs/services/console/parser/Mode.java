package org.prowl.distribbs.services.console.parser;

import java.util.Arrays;

public enum Mode {


    CMD,
    DXCC,
    MESSAGE;


    public static Mode findByName(final String name) {
        return Arrays.stream(values()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
    }

}
