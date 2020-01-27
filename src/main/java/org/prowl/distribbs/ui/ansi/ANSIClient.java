package org.prowl.distribbs.ui.ansi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminal;

public class ANSIClient extends Thread {
   
   private static final Log          LOG = LogFactory.getLog("ANSIClient");

   private TextColor background = new TextColor.RGB(0, 0, 64);
   private TextColor foreground = new TextColor.RGB(192,192,192);

   private TelnetTerminal terminal;
   private TerminalScreen screen;
   private MultiWindowTextGUI gui;
   private BasicWindow desktop;
   
   public ANSIClient(TelnetTerminal terminal) {
      this.terminal = terminal;
   }
   
   public void start() {
      try {
         terminal.setBackgroundColor(background);
         terminal.setForegroundColor(foreground);
         terminal.clearScreen();
         screen = new TerminalScreen(terminal);
         screen.startScreen();
         gui = new MultiWindowTextGUI(screen);

         desktop = new BasicWindow();
         
      
      } catch(Throwable e) {
         LOG.error(e.getMessage(), e);
      }
      
      try { screen.close(); } catch(Throwable e) { }
   }
   
   
}