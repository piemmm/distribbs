package org.prowl.distribbs.services.messages;


import org.junit.jupiter.api.Test;
import org.prowl.distribbs.objects.messages.Message;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest {

    public static final long TEST_DATE = 1234567890l;
    public static final String TEST_TO = "G0SGY";
    public static final String TEST_FROM = "G0TAI";
    public static final String TEST_SUBJECT = "This is a test message subject";
    public static final String TEST_BODY = " This is a test message main body text\nwithseveral\nlines\n\n73\n and a leading space";

    /**
     * Serialisation test
     */
    @Test
    public void testSerialise() {

        Message message = new Message();
        message.setBody(TEST_BODY.getBytes());
        message.setDate(TEST_DATE);
        message.setFrom(TEST_FROM);
        message.setGroup(TEST_TO);
        message.setSubject(TEST_SUBJECT);

        byte[] serialised = message.toPacket();

        try {
            Message message2 = new Message().fromPacket(new DataInputStream(new ByteArrayInputStream(serialised)));
            assertArrayEquals(TEST_BODY.getBytes(), message2.getBody());
            assertEquals(TEST_DATE, message2.getDate());
            assertEquals(TEST_TO, message2.getGroup());
            assertEquals(TEST_SUBJECT, message2.getSubject());
            assertEquals(TEST_FROM, message2.getFrom());
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Threw exception whilst running test");
        }
    }

}
