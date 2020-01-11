package org.prowl.distribbs.node.connectivity.sx127x;

import java.util.Arrays;

/**
 * A list of modulations that the SX127x line of chips support
 */
public enum Modulation {

   LoRa,
   MSK,
   FSK,
   GMSK,
   GFSK,
   OOK;

   public static Modulation findByName(final String name) {
      return Arrays.stream(values()).filter(value -> value.name().equals(name)).findFirst().orElse(null);
   }

}
