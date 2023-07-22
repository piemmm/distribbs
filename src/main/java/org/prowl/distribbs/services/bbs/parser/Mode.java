package org.prowl.distribbs.services.bbs.parser;

import java.util.Arrays;

public enum Mode {


    CMD,
    DXCC,
    MESSAGE_READ_PAGINATION,
    MESSAGE_LIST_PAGINATION;


    public static Mode findByName(final String name) {
        return Arrays.stream(values()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
    }

}
