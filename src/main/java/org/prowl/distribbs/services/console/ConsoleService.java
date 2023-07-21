package org.prowl.distribbs.services.console;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.objects.user.User;
import org.prowl.distribbs.services.Service;

import com.googlecode.lanterna.terminal.ansi.TelnetTerminalServer;

/**
 * ANSI UI implementation using lanterna
 */
public class ConsoleService extends Service {

   private static final Log          LOG = LogFactory.getLog("ConsoleService");

   private boolean stop;

   private HierarchicalConfiguration config;

   public ConsoleService(HierarchicalConfiguration config) {
      super(config);
   }

   @Override
   public void acceptedConnection(User user, InputStream in, OutputStream out) {
      ConsoleClientHandler client = new ConsoleClientHandler(user, in, out);
      client.start();
   }

   @Override
   public String getCallsign() {
      return null;
   }

   public void start() {

   }
   
   public void stop() {
      stop = true;
   }
}
