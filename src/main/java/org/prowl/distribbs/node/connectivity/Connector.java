package org.prowl.distribbs.node.connectivity;

import java.io.IOException;

public interface Connector {

   public void start() throws IOException ;
   
   public void stop();
   
   public String getName();
   
}
