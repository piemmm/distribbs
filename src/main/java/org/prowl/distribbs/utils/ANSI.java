package org.prowl.distribbs.utils;
/**
 * ANSI.java
 *
 * Statics holding common-used codes to make telnet/ansi terminals
 * look pretty.
 *
 * @author Ian Hawkins.
 */
public class ANSI {
    public static final String NORMAL = new String(new byte[]{27, 91, 48, 109});
    public static final String BOLD = new String(new byte[]{27, 91, 49, 109});
    public static final String UNDERLINE = new String(new byte[]{27, 91, 51, 109});
    public static final String RED = new String(new byte[]{27, 91, 51, 49, 109});
    public static final String MAGENTA = new String(new byte[]{27, 91, 51, 53, 109});
    public static final String YELLOW = new String(new byte[]{27, 91, 51, 51, 109});
    public static final String GREEN = new String(new byte[]{27, 91, 51, 50, 109});
    public static final String BLUE = new String(new byte[]{27, 91, 51, 52, 109});
    public static final String CYAN = new String(new byte[]{27, 91, 51, 54, 109});
    public static final String WHITE = new String(new byte[]{27, 91, 51, 55, 109});


    public static String removeANSI(String s) {
        return s.replace(NORMAL,"")
                .replace(BOLD,"")
                .replace(UNDERLINE,"")
                .replace(RED,"")
                .replace(MAGENTA,"")
                .replace(YELLOW,"")
                .replace(GREEN,"")
                .replace(BLUE,"")
                .replace(CYAN,"")
                .replace(WHITE,"");
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
