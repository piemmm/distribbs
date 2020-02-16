package org.prowl.distribbs.ui.hardware;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.lcd.US2066;
import org.prowl.distribbs.leds.StatusLeds;
import org.prowl.distribbs.objectstorage.Storage;
import org.prowl.distribbs.services.newsgroups.NewsMessage;
import org.prowl.distribbs.utils.Tools;

public class Status {

   private US2066     lcd;
   private StatusLeds leds;
   private Timer      tickerTimer;

   public Status() {
      init();
   }

   public void init() {

      lcd = new US2066();
      leds = new StatusLeds();
      lcd.writeText(DistriBBS.VERSION_STRING, DistriBBS.INFO_TEXT);

      start();
   }

   public void start() {
      StringBuilder topString = new StringBuilder();
      StringBuilder bottomString = new StringBuilder();

      tickerTimer = new Timer();
      tickerTimer.schedule(new TimerTask() {
         private int screen = 0;

         public void run() {

            switch (screen % 3) {
               case 0: screen0(); break;
               case 1: screen1(); break;
               case 2: screen2(); break;
            }
            

            screen++;

         }
      }, 2000, 5000);

   }
   
   public void screen0() {

      String topString = "Idle";
      String bottomString = "No new messages";

      setText(topString.toString(), bottomString.toString());
   }
   
   public void screen1() {

      String topString = "MHeard:";
      String bottomString = " none";

      setText(topString.toString(), bottomString.toString());
   }


   public void screen2() {
      String topString = "Status: OK";
      String bottomString = "IP: -";
      bottomString = Tools.getDefaultOutboundIP().getHostAddress();
       
      setText(topString.toString(), bottomString.toString());
   }
   
   
   public void setText(String line1, String line2) {
      lcd.writeText(line1, line2);
   }

   public void pulseGPS(long time) {
      leds.pulseGPS(time);
   }

   public void setMessageBlink(boolean shouldBlink, long blinkRate) {
      leds.setMessageBlink(shouldBlink, blinkRate);
   }

   public void setLink(boolean on) {
      leds.setLink(on);
   }

   public void pulseTRX(long time) {
      leds.pulseTRX(time);
   }

}
