package org.prowl.distribbs.uilocal.ansi.parser;

import java.util.Arrays;

public enum Mode {

   
   CMD,
   DXCC,
   MESSAGE;
   
   
   public static Mode findByName(final String name) {
      return Arrays.stream(values()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
   }

}
