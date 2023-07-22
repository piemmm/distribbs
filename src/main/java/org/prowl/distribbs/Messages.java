package org.prowl.distribbs;

import org.prowl.distribbs.utils.Tools;

import java.util.ResourceBundle;

public final class Messages {

    private static ResourceBundle bundle;

    public static void init() {
        bundle = ResourceBundle.getBundle("messages");
    }

    public static String get(String key) {
        return bundle.getString(key);
    }

}
