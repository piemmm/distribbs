package org.prowl.distribbs.uiremote.text.parser;

import java.util.Arrays;

public enum Command {

   B,
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

   MH, // List heard stations
   MHEARD, // List heard stations
   HEARD; // List heard stations



   public static Command findByName(final String name) {

      // Special case for some well known characters
      if (name.startsWith("?")) {
         return H;
      }

      return Arrays.stream(values()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
   }

}
