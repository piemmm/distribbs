package org.prowl.distribbs.ui.ansi.parser;

import java.util.Arrays;

public enum Command {

   HELP, // Help text
   BYE, // Logout (close connection)
   PORT, // Change Port: 'port 1'
   PORTS, // List ports
   HEARD, // List heard stations
   PING; // Perform a ping

   public static Command findByName(final String name) {
      return Arrays.stream(values()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
   }

}
