package org.prowl.distribbs.utils.compression;

/**
 * Output stream that compresses data. A compressed block
 * is generated and transmitted once a given number of bytes
 * have been written, or when the flush method is invoked.
 *
 * Copyright 2005 - Philip Isenhour - http://javatechniques.com/
 *
 * This software is provided 'as-is', without any express or
 * implied warranty. In no event will the authors be held liable
 * for any damages arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any
 * purpose, including commercial applications, and to alter it and
 * redistribute it freely, subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you
 *     must not claim that you wrote the original software. If you
 *     use this software in a product, an acknowledgment in the
 *     product documentation would be appreciated but is not required.
 *
 *  2. Altered source versions must be plainly marked as such, and
 *     must not be misrepresented as being the original software.
 *
 *  3. This notice may not be removed or altered from any source
 *     distribution.
 *
 * $Id: CompressedBlockOutputStream.java,v 1.1 2015/06/16 20:23:42 ihawkins Exp $
 */
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

public class CompressedBlockOutputStream extends FilterOutputStream {

   /**
    * Buffer for input data
    */
   private byte[]       inBuf    = null;

   /**
    * Buffer for compressed data to be written
    */
   private byte[]       outBuf   = null;

   /**
    * Number of bytes in the buffer
    */
   private volatile int len      = 0;

   /**
    * Deflater for compressing data
    */
   private Deflater     deflater = null;

   /**
    * Constructs a CompressedBlockOutputStream that writes to the given underlying
    * output stream 'os' and sends a compressed block once 'size' byte have been
    * written. The default compression strategy and level are used.
    */
   public CompressedBlockOutputStream(OutputStream os, int size) throws IOException {
      this(os, size, Deflater.BEST_COMPRESSION, Deflater.DEFAULT_STRATEGY);
   }

   /**
    * Constructs a CompressedBlockOutputStream that writes to the given underlying
    * output stream 'os' and sends a compressed block once 'size' byte have been
    * written. The compression level and strategy should be specified using the
    * constants defined in java.util.zip.Deflator.
    */
   public CompressedBlockOutputStream(OutputStream os, int size, int level, int strategy) throws IOException {
      super(os);
      this.inBuf = new byte[size];
      this.outBuf = new byte[size + 64];
      this.deflater = new Deflater(level);
      this.deflater.setStrategy(strategy);
      this.deflater.setLevel(Deflater.BEST_COMPRESSION);
      this.deflater.setDictionary(Dictionary.get());
      this.deflater.reset();

   }

   protected void compressAndSend(int leng) throws IOException {
      if (len > 0) {

         deflater.setInput(inBuf, 0, len);

         deflater.finish();

         deflater.setDictionary(Dictionary.get());
         int size = deflater.deflate(outBuf);

//System.out.println("Size:" + len+"/"+size+" ("+(((int)(((double)size/len)*100d)))+")"+"  ");//+dictionary.size()+" "+len);
         if (size == 0 || len < 10 || size + 4 > len) {
            // Uncompressed
            out.write((len >> 8) & 0xFF);
            out.write((len >> 0) & 0xFF);
            out.write(0);
            out.write(0);
            // System.out.print("u");

            out.write(inBuf, 0, len);
            // System.out.println("USize:"+len+" for:'"+new String(inBuf,0,len)+"'");
         } else {
            // Write the size of the compressed data, followed
            // by the size of the uncompressed data
            // compressed
            out.write((size >> 8) & 0xFF);
            out.write((size >> 0) & 0xFF);

            out.write((len >> 8) & 0xFF);
            out.write((len >> 0) & 0xFF);
            out.write(outBuf, 0, size);

         }
         out.flush();

         deflater.reset();

      }
      len = 0;

   }

   public void write(int b) throws IOException {
      if (len > inBuf.length) {
         len = 0;
      }
      inBuf[len++] = (byte) b;
      if (len == inBuf.length) {
         compressAndSend(inBuf.length);
      }
   }

   public void write(byte[] b, int boff, int blen) throws IOException {
      while ((len + blen) > inBuf.length) {
         int toCopy = inBuf.length - len;
         System.arraycopy(b, boff, inBuf, len, toCopy);
         len += toCopy;
         compressAndSend(len);
         boff += toCopy;
         blen -= toCopy;
      }
      System.arraycopy(b, boff, inBuf, len, blen);
      len += blen;
   }

   public void flush() throws IOException {
      compressAndSend(len);
      out.flush();
   }

   public void close() throws IOException {
      compressAndSend(len);
      out.close();
   }
}