package org.prowl.distribbs.objectstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.prowl.distribbs.services.Priority;
import org.prowl.distribbs.services.newsgroups.NewsMessage;

@TestMethodOrder(OrderAnnotation.class)
class TestStorage {
   
   private static Storage storage;
   private static NewsMessage testMessage;
   
   @BeforeAll
   public static void setup() {
      // Setup the storage class
      HierarchicalConfiguration config = new HierarchicalConfiguration();
      storage = new Storage(config);
      
      // Create a test message
      testMessage = new NewsMessage();
      testMessage.setSubject("This is a test subject");
      testMessage.setBody("This is the message body");
      testMessage.setFrom("G0SGY");
      testMessage.setGroup("TESTING");
      testMessage.setPriority(Priority.LOW);
      testMessage.setDate(1579103633244l);
      
      // Ensure our test message does not exist
      storage.getNewsMessageFile(testMessage).delete();
      
      
   }
    
   
   @Test
   @Order(1)
   void testWrite() {
 
      assertFalse(storage.doesNewsMessageExist(testMessage));
      try {
         storage.storeNewsMessage(testMessage);
      } catch(IOException e) {
         fail(e.getMessage(), e);
      }
      assertTrue(storage.doesNewsMessageExist(testMessage));
    
   }
   
   @Test
   @Order(2)
   void testRead() {

       File f = storage.getNewsMessageFile(testMessage);
       try {
          NewsMessage retrieved = storage.loadNewsMessage(f);
          assertEquals(retrieved.getBody(), testMessage.getBody());
          assertEquals(retrieved.getDate(), testMessage.getDate());
          assertEquals(retrieved.getFrom(), testMessage.getFrom());
          assertEquals(retrieved.getGroup(), testMessage.getGroup());
          assertEquals(retrieved.getSubject(), testMessage.getSubject());
          
       } catch(IOException e) { 
          fail(e.getMessage(),e);
       }
      
      
   }

   
   @AfterAll
   public static void cleanUp() {
      storage.getNewsMessageFile(testMessage).delete();
   }
}
