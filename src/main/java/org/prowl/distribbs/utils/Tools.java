package org.prowl.distribbs.utils;

import net.sf.marineapi.nmea.util.Position;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.utils.compression.deflatehuffman.huffman.AdaptiveHuffmanCompress;
import org.prowl.distribbs.utils.compression.deflatehuffman.huffman.AdaptiveHuffmanDecompress;
import org.prowl.distribbs.utils.compression.deflatehuffman.huffman.BitInputStream;
import org.prowl.distribbs.utils.compression.deflatehuffman.huffman.BitOutputStream;


import java.io.*;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.Enumeration;

public class Tools {

    private static final Log LOG = LogFactory.getLog("Tools");

    /**
     * Convert a hex string (AABBCCDEFF030201) to a byte array
     *
     * @param s The string to convert
     * @return a byte array
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Convert a byte array to a hex string
     *
     * @param output
     * @return a String
     */
    public static String byteArrayToHexString(byte[] output) {
        if (output == null) {
            return "null array";
        }
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < output.length; i++) {
            hexString.append(String.format("%02X", output[i]));
            hexString.append(" ");
        }
        return hexString.toString();
    }

    /**
     * Mainly for debug, strip binary content and show text only data (like strings
     * in unix). Obviously this doesn't do unicode.
     *
     * @param binaryContent
     * @return the text only component of the binary content
     */
    public static String textOnly(byte[] binaryContent) {
        if (binaryContent == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int b : binaryContent) {
            if ((b > 31 && b < 194) || b == 9) {
                sb.append((char) b);
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
     *
     * @param input
     * @return
     */
    public static final String md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input);
            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);
            while (md5.length() < 32)
                md5 = "0" + md5;
            return md5;
        } catch (NoSuchAlgorithmException e) {
            return "1";
        }
    }

    public static final void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public static final byte[] longToByte(long l) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos);) {
            dos.writeLong(l);
            dos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    public static final InetAddress getDefaultOutboundIP() {

        InetAddress ip = null;
        // Try to get the default IP for outbound data
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress();
        } catch (Throwable e) {
            // Ignore
        }

        // Try a more convoluted way.
        try {
            if (ip == null || ip.isLoopbackAddress()) {
                Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                while (en.hasMoreElements()) {
                    NetworkInterface ni = en.nextElement();
                    if (ni.isUp()) {
                        Enumeration<InetAddress> ee = ni.getInetAddresses();
                        while (ee.hasMoreElements()) {
                            InetAddress i = (InetAddress) ee.nextElement();
                            if (!i.isLoopbackAddress()) {
                                return ip;
                            }
                        }
                    }

                }
            }
        } catch (Throwable e) {
            // Ignore.
        }

        return ip;
    }

    /**
     * Compress a packet
     *
     * @param data
     * @return
     */
    public static final byte[] compress(byte[] data) {
        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
            BitOutputStream bitOut = new BitOutputStream(bos);
            AdaptiveHuffmanCompress.compress(new ByteArrayInputStream(data), bitOut);
            bitOut.close();
            return bos.toByteArray();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Decompress a packet
     *
     * @param compressed
     * @return
     */
    public static final byte[] decompress(byte[] compressed) throws EOFException {
        long start = System.currentTimeMillis();
        try (
                ByteArrayOutputStream dec = new ByteArrayOutputStream();
                ByteArrayInputStream bin = new ByteArrayInputStream(compressed);) {

            AdaptiveHuffmanDecompress.decompress(new BitInputStream(bin), dec);
            dec.close();
            return dec.toByteArray();

        } catch (EOFException e) {
            throw e; // rethrow past ioe in this catch block
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert from GPS to maidenhead locator
     *
     * @return
     */
    public static final String convertToMaidenhead(Position position) {

        double longitude = position.getLongitude() + 180;
        longitude /= 2;
        char lonFirst = (char) ('A' + (longitude / 10));
        char lonSecond = (char) ('0' + longitude % 10);
        char lonThird = (char) ('A' + (longitude % 1) * 24);

        double latitude = position.getLatitude() + 90;
        char latFirst = (char) ('A' + (latitude / 10));
        char latSecond = (char) ('0' + latitude % 10);
        char latThird = (char) ('A' + (latitude % 1) * 24);

        StringBuilder sb = new StringBuilder();
        sb.append(lonFirst);
        sb.append(latFirst);
        sb.append(lonSecond);
        sb.append(latSecond);
        sb.append(("" + lonThird).toLowerCase());
        sb.append(("" + latThird).toLowerCase());

        return sb.toString();
    }

    /**
     * Does a byte array contain char X?
     *
     * @param needle
     * @param haystack
     * @return
     */
    public static final int indexOf(char needle, byte[] haystack, int start) {
        for (int i = start; i < haystack.length; i++) {
            if (haystack[i] == needle) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Compare 2 byte arrays, because Arrays.equal(b[],b[]) is badly documented.
     *
     * @param a
     * @param b
     * @return
     */
    public static final boolean arraysEqual(byte[] a, byte[] b) {
        if ((a == null && b != null) || (a != null && b == null)) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }


    public static final void runOnThread(Runnable runme) {
        Thread t = new Thread(runme);
        t.start();
    }

    /**
     * Used for things like rate limiting, avoiding spinning exception loops chewing CPU etc.
     *
     * @param millis
     */
    public static void delay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Convenience method
     *
     * @param number
     * @return
     */
    public static final String toHex(int number) {
        return Integer.toHexString(number);
    }

    /**
     * Convenience method to return the sha-256 hash of a string
     *
     * @param s
     * @return
     */
    public static String hashString(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(s.getBytes());
            byte[] digest = md.digest();
            return byteArrayToHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }



    public static String byteArrayToReadableASCIIString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            if (b < 0x20 || b > 0xFA) {
                sb.append(ANSI.YELLOW + "<");
                sb.append(String.format("%02X", b));
                sb.append(">" + ANSI.NORMAL);
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    public static String getNiceFrequency(long frequency) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);
        nf.setMinimumFractionDigits(3);
        String freq = "";
        if (frequency != 0) {
            freq = nf.format(frequency / 1000000d);
        }
        return freq;
    }

    public static boolean isValidITUCallsign(String callsignToValidate) {
        return callsignToValidate.matches("\\A\\d?[a-zA-Z]{1,2}\\d{1,4}[a-zA-Z]{1,3}(-\\d+)?\\Z");
    }

}
