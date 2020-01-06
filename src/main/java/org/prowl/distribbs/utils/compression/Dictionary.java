package org.prowl.distribbs.utils.compression;

/**
 * A Dictionary containing commonly held phrases sent between 2 systems
 */
public class Dictionary {

   // A carefully generated dictionary that is the same for all running nodes.
   private static final byte[] DICTIONARY = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t               . . . . . . . . . . . . . . . .................===================-------------Subject: Date: From: To: Time: 73 de ".getBytes();
   
   public static final byte[] get() {
      return DICTIONARY;
   }
}
