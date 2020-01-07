package org.prowl.distribbs.objectstorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 * Storage class allows objects to be stored on disk to keep things simple.
 * 
 * Directory based storage, where parts of the object are split up into pieces
 * and recombined when needed as we have plenty of CPU horsepower.
 */
public class Storage {

   private HierarchicalConfiguration config;

   private File                      locationDir = new File("storage");

   public Storage(HierarchicalConfiguration config) {
      this.config = config;
   }

   /**
    * Store an object.  
    * 
    * @param module The module name (message, newsmessage, qsl) - 1 chars max
    * @param dateMillis The date of the item in milliseconds (to be split up into day sized chunks)
    * @param pieceNumber The piece ID of the object to store, or -1 indicating the whole object. 0-65535
    * @param id The message id (usually callsign) 
    * @param object The object to store, as all objects will be less than 6M in size, then we can get away
    *               with a simple byte array
    */
   public void store(String module, String second, String id, int pieceNumber, byte[] object) throws IOException {
//      // Split this down into directories about 1 day (86400000millis ish) apart
//      //String dateStr = Long.toString((int)(dateMillis / 100000000d));
//      
//      // Create the file path we will store all the parts under.  Each part (apart from the last) is a max size of 192 bytes
//      File itemDir = new File(locationDir.getAbsolutePath()+File.separator+module+File.separator+dateStr+File.separator+id);
//      itemDir.mkdirs();
//      
//      if (!itemDir.exists()) {
//         throw new IOException("Unable to create directory: "+itemDir.getAbsolutePath());
//      }
//  
//      // Actually try to save the file
//      File file = new File(itemDir, Integer.toString(pieceNumber));
//      try (FileOutputStream fos = new FileOutputStream(file)) {
//         fos.write(object);;
//         fos.flush();
//         fos.close();
//      } catch(Throwable e) {
//         throw new IOException("Unable to persist file: "  + file.getAbsolutePath());
//      }
      
   }
}