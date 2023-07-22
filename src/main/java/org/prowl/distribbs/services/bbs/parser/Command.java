package org.prowl.distribbs.services.bbs.parser;

import java.util.Arrays;

public enum Command {

    A, // Short for abort
    ABORT, // Abort

    R,
    READ, // Read message

    B, // Bye
    H, // Help text
    HELP, // Help text
    BYE, // Logout (close connection)
    QUIT, // Logout (close connection)
    EXIT, // Logout (close connection)
    END, // Logout (close connection)
    LOGOUT, // Logout (close connection)
    LOGOFF, // Logout (close connection)

    CC, // Colour toggle

    PORTS, // List ports

    LIST, // List BBS messages
    L, // List BBS messages

    MH, // List heard stations
    MHEARD, // List heard stations
    HEARD, // List heard stations

    UNHEARD, // List of nearby nodes we cannot hear
    UH,// List of nearby nodes we cannot hear

    ENTER_KEY; // blank line just enter pressed.


    public static Command findByName(final String name) {

        // Special case for some well known characters
        if (name.startsWith("?")) {
            return H;
        }

        if (name.length() == 0) {
            return ENTER_KEY;
        }

        return Arrays.stream(values()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
    }

}
