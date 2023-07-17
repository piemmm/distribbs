package org.prowl.distribbs;

import org.prowl.distribbs.utils.ANSI;

import java.util.ResourceBundle;

public final class Messages {

    private static ResourceBundle bundle;

    public static final void init() {
        bundle = ResourceBundle.getBundle("messages");
    }

    public static String get(String key) {
        String b = bundle.getString(key);
        b = tokenizeColour(b);
        return b;
    }

    public static String tokenizeColour(String b) {
        b = b.replace("%NORMAL%", ANSI.NORMAL);
        b = b.replace("%BOLD%", ANSI.BOLD);
        b = b.replace("%UNDERLINE%", ANSI.UNDERLINE);
        b = b.replace("%RED%", ANSI.RED);
        b = b.replace("%MAGENTA%", ANSI.MAGENTA);
        b = b.replace("%YELLOW%", ANSI.YELLOW);
        b = b.replace("%GREEN%", ANSI.GREEN);
        b = b.replace("%BLUE%", ANSI.BLUE);
        b = b.replace("%CYAN%", ANSI.CYAN);
        b = b.replace("%WHITE%", ANSI.WHITE);
        return b;
    }
}
