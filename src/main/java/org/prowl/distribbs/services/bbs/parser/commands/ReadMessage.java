package org.prowl.distribbs.services.bbs.parser.commands;

import org.prowl.distribbs.DistriBBS;
import org.prowl.distribbs.annotations.BBSCommand;
import org.prowl.distribbs.core.PacketTools;
import org.prowl.distribbs.objects.Storage;
import org.prowl.distribbs.objects.messages.Message;
import org.prowl.distribbs.services.bbs.parser.Mode;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Reads a message on the BBS
 */
@BBSCommand
public class ReadMessage extends Command {

    // So we can paginate.
    private int readMessageStartingPoint = 0;

    // The current message being read
    private Message currentMessage;

    // A 'buffer' of lines we can paginate on.
    private List<String> messageLines;


    @Override
    public boolean doCommand(String[] data) throws IOException {
        Mode mode = getMode();
        if ((mode.equals(Mode.CMD) || mode.equals(Mode.MESSAGE_LIST_PAGINATION)) && data[0].equals("r")) {
            pushModeToStack(mode);
            try {
                readMessage(Long.parseLong(data[1]));
            } catch (NumberFormatException e) {
                write("Invalid message number" + CR);
            }
        } else if (mode.equals(Mode.MESSAGE_READ_PAGINATION)) {
            sendMessage();
        }
        return true;
    }


    public void readMessage(long messageId) throws IOException {
        Storage storage = DistriBBS.INSTANCE.getStorage();
        Message message = storage.getMessage(messageId);
        readMessageStartingPoint = 0;
        messageLines = new ArrayList<>();

        // Get the message and write it to the messageLines array so we can later paginate on it correctly.
        if (message == null) {
            write("Message not found" + CR);
        } else {
            currentMessage = message;
            messageLines.add("From: " + message.getFrom());
            messageLines.add("To: " + message.getGroup());
            messageLines.add("Subject: " + message.getSubject());
            messageLines.add("Date: " + message.getDate());
            messageLines.add("Route: " + message.getRoute());
            messageLines.add("TSLD: " + message.getTSLD());
            messageLines.add("Size: " + message.getBody().length);
            messageLines.add("");
            StringTokenizer st = new StringTokenizer(PacketTools.textOnly(message.getBody()), "\n\r");
            while (st.hasMoreTokens()) {
                messageLines.add(st.nextToken());
            }
            sendMessage();
        }
    }

    public void sendMessage() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMM/hhmm");
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        nf.setGroupingUsed(false);

        int messageSentCounter = 0;
        for (int i = readMessageStartingPoint; i < messageLines.size(); i++) {

            write(messageLines.get(i) + CR);

            if (++messageSentCounter >= 22) { // todo '10' should be configurable by the user
                setMode(Mode.MESSAGE_READ_PAGINATION);
                readMessageStartingPoint += messageSentCounter;
                return;
            }
        }
        // Reading done, return to previous mode.
        popModeFromStack();
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"read", "r", ""};
    }
}
