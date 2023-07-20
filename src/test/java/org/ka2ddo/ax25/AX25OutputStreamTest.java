package org.ka2ddo.ax25;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class AX25OutputStreamTest {

    private static final AX25Callsign SRC_CALL = new AX25Callsign("G0ABC");
    private static final AX25Callsign DST_CALL = new AX25Callsign("GB7DEF");

    public static final byte[] SMALL_BUFFER = "this is a small buffer".getBytes();

    public static byte[] LARGE_BUFFER = new byte[612]; // 3 and a bit packets
    static {
        Random random = new Random();
        for (int i =0; i < LARGE_BUFFER.length; i++) {
            LARGE_BUFFER[i] = (byte)(random.nextInt(255 & 0xFF));
        }
    }

    /**
     * Test small data packet handling
     */
    @Test
    public void testSmallBuffer() throws IOException  {

        // Create a mock AX25 system
        MockTransmitting mockTransmitting = new MockTransmitting();
        AX25Stack stack = new AX25Stack(255, 7, 1200);
        stack.setTransmitting(mockTransmitting);
        ConnState state = new ConnState(SRC_CALL, DST_CALL, stack);
        state.setConnector(mockTransmitting);
        state.setConnType(ConnState.ConnType.MOD8);
        AX25OutputStream out = new AX25OutputStream(state, 255);
        out.write(SMALL_BUFFER,0, SMALL_BUFFER.length);
        out.flush();

        // Generate a single test packet and see if it is created correctly.
        List<AX25Frame> frames = mockTransmitting.getSentFrames();
        StringBuffer data = new StringBuffer();
        for (AX25Frame frame: frames) {
            data.append(frame.getAsciiFrame());
        }
        Assert.assertArrayEquals(data.toString().getBytes(), SMALL_BUFFER);

    }

    /**
     * Test oversized buffer handling
     */
    @Test
    public void testLargeBuffer() throws IOException {

        // Create a mock AX25 system
        MockTransmitting mockTransmitting = new MockTransmitting();
        AX25Stack stack = new AX25Stack(255, 7, 1200);
        stack.setTransmitting(mockTransmitting);
        ConnState state = new ConnState(SRC_CALL, DST_CALL, stack);
        state.setConnector(mockTransmitting);
        state.setConnType(ConnState.ConnType.MOD8);
        AX25OutputStream out = new AX25OutputStream(state, 255);
        out.write(LARGE_BUFFER ,0, LARGE_BUFFER.length);
        out.flush();

        // Generate a packet that takes multiple frames
        List<AX25Frame> frames = mockTransmitting.getSentFrames();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (AX25Frame frame: frames) {
           bos.write(frame.getBody());
        }
        Assert.assertArrayEquals(bos.toByteArray(), LARGE_BUFFER);



    }
}
