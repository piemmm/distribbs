package org.prowl.distribbs;

import org.prowl.distribbs.utils.ANSI;

import java.util.ResourceBundle;

public final class Messages {

    private static ResourceBundle bundle;

    public static final void init() {
        bundle = ResourceBundle.getBundle("messages");
    }

    public static String get(String key) {
        return bundle.getString(key);
    }

}
