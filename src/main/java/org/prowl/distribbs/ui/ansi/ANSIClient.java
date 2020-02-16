package org.prowl.distribbs.ui.ansi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminal;

public class ANSIClient extends Thread {
   
   private static final Log          LOG = LogFactory.getLog("ANSIClient");

   private TelnetTerminal terminal;
   private TerminalScreen screen;
   private MultiWindowTextGUI gui;
   private BasicWindow desktop;
   
   public ANSIClient(TelnetTerminal terminal) {
      this.terminal = terminal;
   }
   
   public void start() {
      try {

         screen = new TerminalScreen(terminal);
         
         screen.startScreen();
         terminal.clearScreen();
         
         
         gui = new MultiWindowTextGUI(screen);

         gui.setTheme(LanternaThemes.getRegisteredTheme("blaster")); //blaster,businessmachine
         buildDesktop();
          
      
      } catch(Throwable e) {
         LOG.error(e.getMessage(), e);
      }
      
      try { screen.close(); } catch(Throwable e) { }
   }
   
   public void buildDesktop() {
      
      Panel content = new Panel();
      TextInputDialog dialog = new TextInputDialogBuilder().setTitle("Login").setDescription("Descriptiopn").setInitialContent("InitialContent").build();
      //content.addComponent();
       
      desktop = new BasicWindow();
      desktop.setComponent(content);
      
      
      //gui.addWindowAndWait(desktop);
      
      
      dialog.showDialog(gui);

      
   }
   
}