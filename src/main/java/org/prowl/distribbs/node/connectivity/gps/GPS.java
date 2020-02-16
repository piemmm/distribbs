package org.prowl.distribbs.node.connectivity.gps;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.node.connectivity.Connector;
import org.prowl.distribbs.ui.hardware.Status;

import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPort;
import com.pi4j.io.serial.StopBits;
import com.pi4j.system.SystemInfo.BoardType;

import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.GLLSentence;
import net.sf.marineapi.nmea.sentence.GSASentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.util.GpsFixStatus;
import net.sf.marineapi.nmea.util.Position;

public class GPS implements Connector {

   private static final Log          LOG = LogFactory.getLog("GPS");

   private HierarchicalConfiguration config;
   private Serial                    serial;
   private SentenceFactory           sf;
   private static Position           currentPosition;

   public GPS(HierarchicalConfiguration config) {
      this.config = config;
      sf = SentenceFactory.getInstance();
   }

   public void start() throws IOException {

      serial = SerialFactory.createInstance();
      // create and register the serial data listener
      serial.addListener(new SerialDataEventListener() {
         StringBuilder sb = new StringBuilder();

         @Override
         public void dataReceived(SerialDataEvent event) {

            /**
             * Read the data from the serial port
             */
            try {

               byte[] dat = event.getBytes();
               // Parse the serial rx data into each GPS sentence
               for (int b : dat) {
                  if (b > 31) {
                     sb.append((char) b);
                  } else if (b == 10 || b == 13) {
                     String sentence = sb.toString();
                     sb.delete(0, sb.length());
                     if (sentence.length() > 0) {
                        parseSentence(sentence);
                     }
                  }
                  // If for some reason we never get a return, then
                  // discard the data rather than run out of ram.
                  if (sb.length() > 100000) {
                     sb.delete(0, sb.length());
                  }
               }
            } catch (Throwable e) {
               LOG.error(e);
            }

         }
      });

      // create serial config object
      SerialConfig config = new SerialConfig();

      try {

         config.device(SerialPort.getDefaultPort())
               .baud(Baud._9600)
               .dataBits(DataBits._8)
               .parity(Parity.NONE)
               .stopBits(StopBits._1)
               .flowControl(FlowControl.NONE);

         // open the default serial device/port with the configuration settings
         serial.open(config);

      } catch (Throwable e) {
         // Try  3B+
         try {
            config.device("/dev/ttyS0")
            .baud(Baud._9600)
            .dataBits(DataBits._8)
            .parity(Parity.NONE)
            .stopBits(StopBits._1)
            .flowControl(FlowControl.NONE);
            serial.open(config);
         } catch(Throwable ex) {
             // Rethrow as IOE
             throw new IOException(e);
         }
      
      }

   }

   public void stop() {

   }

   /**
    * Parse the GPS sentence into easier to manage positional data.
    * 
    * @param sentence the raw NMEA GPS data (GPGLL, GPGSV, etc)
    */
   public void parseSentence(String sentence) {
      try {
         Sentence parsed = sf.createParser(sentence);
         if (parsed instanceof GLLSentence) {
            currentPosition = ((GLLSentence) parsed).getPosition();
            DistriBBS.INSTANCE.getStatus().pulseGPS(2000);
         } else if (parsed instanceof GSASentence) {
            GpsFixStatus status = ((GSASentence) parsed).getFixStatus();
            if (status == GpsFixStatus.GPS_NA) {
               DistriBBS.INSTANCE.getStatus().pulseGPS(150);
               currentPosition = null;
            }
         }
      } catch (DataNotAvailableException e) {
         // GPS data not available
      } catch (Throwable e) {
         LOG.error(e);
      }
      
   }

   /**
    * Returns the curent known position, or null if no fix
    * 
    * @return
    */
   public static Position getCurrentPosition() {
      return currentPosition;
   }

   public String getName() {
      return getClass().getName();
   }

}