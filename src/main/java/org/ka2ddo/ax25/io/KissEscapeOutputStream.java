package org.ka2ddo.ax25.io;
/*
* Copyright (C) 2011-2019 Andrew Pavlin, KA2DDO
* This file is part of YAAC (Yet Another APRS Client).
*
*  YAAC is free software: you can redistribute it and/or modify
*  it under the terms of the GNU Lesser General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  YAAC is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  and GNU Lesser General Public License along with YAAC.  If not,
*  see <http://www.gnu.org/licenses/>.
*/

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This filtering OutputStream adds the KISS protocol escape sequences for the
 * body of a KISS frame. It also calculates G8BPQ's CRC for the frame if needed
 * for MKISS operations.
 * @author Andrew Pavlin, KA2DDO
*/
public class KissEscapeOutputStream extends OutputStream {
    /**
     * Byte value for end-of-frame flag byte in KISS protocol.
     */
    public static final int FEND = 0xC0;
    /**
     * Byte value of prefix for escaped byte value (use of protocol byte value in frame body).
     */
    public static final int FESC = 0xDB;
    /**
     * Escaped value for literal FEND character.
     */
    public static final int TFEND = 0xDC;
    /**
     * Escaped value for literal FESC character.
     */
    public static final int TFESC = 0xDD;

    private int byteCount = 0;
    private final DataOutput out;
    private final OutputStream os;
    private byte g8bpqCrc = 0;

    /**
     * This is some weird thing that is not in spec and looks like a hack claiming something about kenwood 'features'
     * which unfortunately it does not describe what these are or do.
     *
     * it is now disabled by default as it breaks normal KISS connections
     */
    private final boolean escCForKenwood;

    /**
     * Create a KissEscapeOutputStream wrapped around an implementation of the java.io.DataOutput
     * interface.
     * @param out DataOutput interface implementer to wrap with this stream
     */
    public KissEscapeOutputStream(DataOutput out) {
        this.out = out;
        if (out instanceof OutputStream) {
            this.os = (OutputStream)out;
        } else {
            this.os = null;
        }
        escCForKenwood = false;
    }

    /**
     * Create a KissEscapeOutputStream wrapped around an OutputStream.
     * @param os OutputStream to receive KISS-encoded frames
     */
    public KissEscapeOutputStream(OutputStream os) {
        if (os instanceof DataOutput) {
            this.out = (DataOutput)os;
        } else {
            this.out = null;
        }
        this.os = os;
        escCForKenwood = false;
    }

    /**
     * Create a KissEscapeOutputStream wrapped around an implementation of the java.io.DataOutput
     * interface.
     * @param out DataOutput interface implementer to wrap with this stream
     * @param escCForKenwood boolean, if true also escape 'C' with FESC to protect against Kenwood "features"
     */
    public KissEscapeOutputStream(DataOutput out, boolean escCForKenwood) {
        this.out = out;
        if (out instanceof OutputStream) {
            this.os = (OutputStream)out;
        } else {
            this.os = null;
        }
        this.escCForKenwood = escCForKenwood;
    }

    /**
     * Create a KissEscapeOutputStream wrapped around an OutputStream.
     * @param os OutputStream to receive KISS-encoded frames
     * @param escCForKenwood boolean, if true also escape 'C' with FESC to protect against Kenwood "features"
     */
    public KissEscapeOutputStream(OutputStream os, boolean escCForKenwood) {
        if (os instanceof DataOutput) {
            this.out = (DataOutput)os;
        } else {
            this.out = null;
        }
        this.os = os;
        this.escCForKenwood = escCForKenwood;
    }

    /**
     * Get the number of bytes passed through this stream (counting escape codes injected by the stream).
     * @return byte count
     */
    public int getByteCount() {
        return byteCount;
    }

    /**
     * Reset the statistics fields for this stream.
     */
    public void resetByteCount() {
        this.byteCount = 0;
        this.g8bpqCrc = 0;
    }

    /**
     * Write one byte to the output stream.
     * @param b byte value to encode
     * @throws IOException if wrapped stream throws an IOException
     */
    public void write(int b) throws IOException {
        OutputStream os;
        if ((os = this.os) != null) {
            if (b == FEND) {
                os.write(FESC);
                os.write(TFEND);
                byteCount += 2;
            } else if (b == FESC) {
                os.write(FESC);
                os.write(TFESC);
                byteCount += 2;
            } else if ('C' == b && escCForKenwood) {
                os.write(FESC);
                os.write('C');
                byteCount += 2;
            } else {
                os.write(b);
                byteCount++;
            }
        } else {
            if (b == FEND) {
                out.write(FESC);
                out.write(TFEND);
                byteCount += 2;
            } else if (b == FESC) {
                out.write(FESC);
                out.write(TFESC);
                byteCount += 2;
            } else if ('C' == b && escCForKenwood) {
                out.write(FESC);
                out.write('C');
                byteCount += 2;
            } else {
                out.write(b);
                byteCount++;
            }
        }
        g8bpqCrc ^= (byte)b;
    }

    /**
     * Get the G8BPQ CRC value for the last sent KISS frame.
     * @return one-byte CRC as used by G8BPQ
     */
    public byte getG8bpqCrc() {
        return g8bpqCrc;
    }

    /**
     * States of a KISS frame decoder.
     */
    public enum RcvState {
        /**
         * KISS decoder has not received the first FEND byte since (re-)initialization.
         */
        IDLE,
        /**
         * KISS decoder has received an FEND byte and is waiting for more body bytes.
         */
        IN_FRAME,
        /**
         * KISS decoder has received an FESC byte and is waiting for the TFEND or TFESC byte to indicate
         * which byte code was escaped.
         */
        IN_ESC
    }
}