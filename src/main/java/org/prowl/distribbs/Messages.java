package org.prowl.distribbs;

import java.util.ResourceBundle;

public final class Messages {

    private static ResourceBundle bundle;

    public static final void init() {
        bundle = ResourceBundle.getBundle("messages");
    }

    public static final String get(String key) {
        return bundle.getString(key);
    }

}
