package org.prowl.distribbs.uiremote.text.parser;

import java.util.Arrays;

public enum Command {

   HELP, // Help text
   BYE, // Logout (close connection)
   QUIT, // Logout (close connection)
   EXIT, // Logout (close connection)
   END, // Logout (close connection)
   LOGOUT, // Logout (close connection)
   LOGOFF, // Logout (close connection)

   PORTS, // List ports
   HEARD; // List heard stations


   public static Command findByName(final String name) {
      return Arrays.stream(values()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
   }

}
