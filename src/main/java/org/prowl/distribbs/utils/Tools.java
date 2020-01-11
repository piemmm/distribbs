package org.prowl.distribbs.utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Tools {

   
   /**
    * Convert a hex string (AABBCCDEFF030201) to a byte array
    * @param s The string to convert
    * @return a byte array
    */
   public static byte[] hexStringToByteArray(String s) {
      int len = s.length();
      byte[] data = new byte[len / 2];
      for (int i = 0; i < len; i += 2) {
         data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
               + Character.digit(s.charAt(i+1), 16));
      }
      return data;
   }

   /**
    * Convert a byte array to a hex string
    * @param output
    * @return a String
    */
   public static String byteArrayToHexString(byte[] output) {
      StringBuffer hexString = new StringBuffer();
      for (int i=0; i<output.length; i++)
         hexString.append(String.format("%02X", output[i]));
      return hexString.toString();
   }
   
   /**
    * Mainly for debug, strip binary content and show text only data (like strings in unix). Obviously this doesn't do unicode.
    * @param binaryContent
    * @return the text only component of the binary content
    */
   public static String textOnly(byte[] binaryContent) {
      StringBuilder sb = new StringBuilder();
      for (int b: binaryContent) {
         if ((b > 31 && b < 127) || b == 9) {
            sb.append((char)b);
         }
      }
      return sb.toString();
   }
   
   /**
    * Convenience method to read X amount of bytes from a stream
    * 
    * @param din
    * @param length
    * @return
    */
   public static final String readString(DataInputStream din, int length) throws IOException {
      byte[] data = new byte[length];
      din.read(data, 0, length);
      return new String(data);
   }
   
   /**
    * Convenience method to read X amount of bytes from a stream
    * 
    * @param din
    * @param length
    * @return
    */
   public static final byte[] readBytes(DataInputStream din, int length) throws IOException {
      byte[] data = new byte[length];
      din.read(data, 0, length);
      return data;
   }
   
   /**
    * MD5sum stuff.
    * @param input
    * @return
    */
   public static final String md5(byte[] input) {
      try     {
         MessageDigest md = MessageDigest.getInstance("MD5");
         byte[] messageDigest = md.digest(input);
         BigInteger number = new BigInteger(1,messageDigest);
         String md5 = number.toString(16);
         while (md5.length() < 32)
            md5 = "0" + md5;
         return md5;
      } catch(NoSuchAlgorithmException e) {
         return "1";
      }
   }

   
}
