package org.prowl.distribbs.node.connectivity.gps;

import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.node.connectivity.Connector;

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

public class GPS implements Connector {

   private static final Log          LOG = LogFactory.getLog("GPS");

   private HierarchicalConfiguration config;
   private Serial                    serial;

   public GPS(HierarchicalConfiguration config) {
      this.config = config;
   }

   public void start() throws IOException {

      serial = SerialFactory.createInstance();
      // create and register the serial data listener
      serial.addListener(new SerialDataEventListener() {
         @Override
         public void dataReceived(SerialDataEvent event) {

            // NOTE! - It is extremely important to read the data received from the
            // serial port. If it does not get read from the receive buffer, the
            // buffer will continue to grow and consume memory.

            // print out the data received to the console
            try {
               LOG.debug("[ASCII DATA] " + event.getAsciiString());
               
               
               
               
               
            } catch (Throwable e) {
               LOG.error(e);
            }
            
         }
      });

      // create serial config object
      SerialConfig config = new SerialConfig();

      
      try {
      // set default serial settings (device, baud rate, flow control, etc)
      //
      // by default, use the DEFAULT com port on the Raspberry Pi (exposed on GPIO
      // header)
      // NOTE: this utility method will determine the default serial port for the
      // detected platform and board/model. For all Raspberry Pi models
      // except the 3B, it will return "/dev/ttyAMA0". For Raspberry Pi
      // model 3B may return "/dev/ttyS0" or "/dev/ttyAMA0" depending on
      // environment configuration.
      config.device(SerialPort.getDefaultPort())
            .baud(Baud._9600)
            .dataBits(DataBits._8)
            .parity(Parity.NONE)
            .stopBits(StopBits._1)
            .flowControl(FlowControl.NONE);

      
      // open the default serial device/port with the configuration settings
      serial.open(config);

      } catch(InterruptedException e) {
         // Rethrow as IOE
         throw new IOException(e);
      }
 
   }

   public void stop() {

   }

   public String getName() {
      return getClass().getName();
   }

}