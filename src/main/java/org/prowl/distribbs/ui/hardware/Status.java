package org.prowl.distribbs.ui.hardware;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.core.Node;
import org.prowl.distribbs.eventbus.ServerBus;
import org.prowl.distribbs.eventbus.events.RxRFPacket;
import org.prowl.distribbs.eventbus.events.TxRFPacket;
import org.prowl.distribbs.lcd.US2066;
import org.prowl.distribbs.leds.StatusLeds;
import org.prowl.distribbs.utils.Tools;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

public class Status {

   private static final Log LOG = LogFactory.getLog("Status");

   private US2066           lcd;
   private StatusLeds       leds;
   private Timer            tickerTimer;

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
       
      tickerTimer = new Timer();
      tickerTimer.schedule(new TimerTask() {
         private int screen = 0;

         public void run() {

            try {
               switch (screen % 3) {
                  case 0:
                     screen0();
                     break;
                  case 1:
                     screen1();
                     break;
                  case 2:
                     screen2();
                     break;
               }
            } catch (Throwable e) {
               LOG.debug(e.getMessage(), e);
            }

            screen++;

         }
      }, 2000, 5000);

      // Register our interest in events.
      ServerBus.INSTANCE.register(this);
   }

   public void stop() {
      // Stop listening to events
      ServerBus.INSTANCE.unregister(this);
   }

   public void screen0() {

      String topString = "Idle";
      String bottomString = "No new messages";

      setText(topString.toString(), bottomString.toString());
   }

   public void screen1() {

      String topString = "MHeard:";
      String bottomString = " none";

      List<Node> heardList = DistriBBS.INSTANCE.getStatistics().getHeard().listHeard();
      StringBuilder b = new StringBuilder();
      if (heardList.size() > 0) {
         bottomString = "";
         for (Node n : heardList) {
            String callsign = n.getCallsign();
            if (b.length() < 20 - callsign.length()) {
               topString += callsign + " ";
            } else {
               bottomString += callsign + " ";
            }

         }
      }

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

   @Subscribe
   public void pulseTRX(RxRFPacket packet) {
      leds.pulseTRX(50);
   }

   @Subscribe
   public void pulseTRX(TxRFPacket packet) {
      leds.pulseTRX(50);
   }

}
