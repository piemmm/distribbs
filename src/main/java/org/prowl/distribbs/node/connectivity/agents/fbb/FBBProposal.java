package org.prowl.distribbs.node.connectivity.agents.fbb;

import java.util.StringTokenizer;

/**
 * Class representing an FBB proposal
 *
 * FB : Identifies the type of the command (proposal)
 * P : Type of message (P = Private, B = Bulletin).
 * F6FBB : Sender (from field).
 * FC1GHV : BBS of recipient (@field).
 * FC1MVP : Recipient (to field).
 * 24657_F6FBB : BID ou MID.
 * 1345 : Size of message in bytes.
 * F> : End of proposal.
 */
public class FBBProposal {

    private String sender;
    private String recipientBBS;
    private String recipient;
    private String BID_MID;
    private String type;
    private long size;


    public FBBProposal(String line) {
        StringTokenizer st = new StringTokenizer(line," ", false);
        st.nextToken();
        this.type = st.nextToken();
        this.sender = st.nextToken();
        this.recipientBBS = st.nextToken(); // if bulletin will be distribution or something like "WW"
        this.recipient = st.nextToken(); // If bulletin will be 'group' "ASTRO", "PIC", etc, etc.
        this.BID_MID = st.nextToken();
        this.size = Long.parseLong(st.nextToken());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipientBBS() {
        return recipientBBS;
    }

    public void setRecipientBBS(String recipientBBS) {
        this.recipientBBS = recipientBBS;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getBID_MID() {
        return BID_MID;
    }

    public void setBID_MID(String BID_MID) {
        this.BID_MID = BID_MID;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
