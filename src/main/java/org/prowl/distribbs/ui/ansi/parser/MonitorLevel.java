package org.prowl.distribbs.ui.ansi.parser;

import java.util.Arrays;

public enum MonitorLevel {

   NONE,
   ANNOUNCE,
   ALL;
   
   public static MonitorLevel findByName(final String name) {
      return Arrays.stream(values()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
   }

}
