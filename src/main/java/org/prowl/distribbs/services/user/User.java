package org.prowl.distribbs.services.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.services.InvalidMessageException;
import org.prowl.distribbs.services.messages.MailMessage;
import org.prowl.distribbs.utils.Tools;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This represents a user.
 *
 * A user has a name, priviliges, and a callsign.
 */
public class User {

    private static final Log LOG         = LogFactory.getLog("User");

    private String name;
    private String baseCallsign;
    private String privFlags;
    private String location;

    public User() {

    }


    public User(String name, String baseCallsign, String privFlags) {

        this.name = name;
        this.baseCallsign = baseCallsign;
        this.privFlags = privFlags;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseCallsign() {
        return baseCallsign;
    }

    public void setBaseCallsign(String baseCallsign) {
        this.baseCallsign = baseCallsign;
    }

    public boolean hasPriv(String priv) {
        return privFlags.contains(priv);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }




    /**
     * Serialise into a byte array. Keeping the size to a minimum is important.
     * Length, data, length, data format for all the fields.
     *
     * @return A byte array representing the serialised message
     */
    public byte[] toPacket() {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream( 30);
             DataOutputStream dout = new DataOutputStream(bos)) {

            // String.length measures UTF units, which is no good to use, so we will use the
            // byte array size.
            byte[] bname = name.getBytes();
            byte[] bbaseCallsign = baseCallsign.getBytes();
            byte[] bprivFlags = privFlags.getBytes();
            byte[] blocation = location.getBytes();

            dout.writeInt(bname.length);
            dout.write(bname);

            dout.writeInt(bbaseCallsign.length);
            dout.write(bbaseCallsign);

            dout.writeInt(bprivFlags.length);
            dout.write(bprivFlags);

            dout.writeInt(blocation.length);
            dout.write(blocation);

            dout.flush();
            dout.close();
            return bos.toByteArray();

        } catch (Throwable e) {
            LOG.error("Unable to serialise message", e);
        }
        return null;
    }

    public String getPrivFlags() {
        return privFlags;
    }

    public void setPrivFlags(String privFlags) {
        this.privFlags = privFlags;
    }

    /**
     * Deserialise from a byte array
     **/
    public User fromPacket(DataInputStream din) throws InvalidMessageException {

        try {

            String name = Tools.readString(din, din.readInt());
            String baseCallsign = Tools.readString(din, din.readInt());
            String privFlags = Tools.readString(din, din.readInt());
            String location = Tools.readString(din, din.readInt());

            setName(name);
            setBaseCallsign(baseCallsign);
            setPrivFlags(privFlags);
            setLocation(location);

        } catch (Throwable e) {
            LOG.error("Unable to build message from packet", e);
            throw new InvalidMessageException(e.getMessage(), e);
        }
        return this;
    }

}
