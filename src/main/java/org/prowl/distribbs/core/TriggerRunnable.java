package org.prowl.distribbs.core;

import java.util.Timer;
import java.util.TimerTask;

public abstract class TriggerRunnable {

    private static final long MAX_TIMEOUT = 1000l * 60l; // 60 seconds.

    private long expiresAt = -1;
    private String triggerTo;
    private String triggerFrom;
    private String triggerResponse;
    private byte[] triggerPayload;
    private boolean expired = false;

    private Timer timer;

    public TriggerRunnable() {

    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
        long timeout = (expiresAt - System.currentTimeMillis());

        if (timeout > MAX_TIMEOUT)
            throw new RuntimeException("Timeout exceeds max of: " + MAX_TIMEOUT + "ms");

        // Max 60 second timeout.
        if (expiresAt > 0) {
            timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    expired = true;
                    runExpired();

                    try {
                        timer.cancel();
                    } catch (Throwable e) {
                    }
                }
            }, timeout);
        }
    }

    public void run(String from, String to, String response, byte[] payload) {
        if (expired)
            throw new RuntimeException("Trigger already expired");
        try {
            timer.cancel();
        } catch (Throwable e) {
        }
    }

    /**
     * Run this when expired
     */
    public void runExpired() {
    }

    public String getTriggerFrom() {
        return triggerFrom;
    }

    public void setTriggerFrom(String triggerFrom) {
        this.triggerFrom = triggerFrom;
    }

    public String getTriggerResponse() {
        return triggerResponse;
    }

    public void setTriggerResponse(String triggerResponse) {
        this.triggerResponse = triggerResponse;
    }

    public byte[] getTriggerPayload() {
        return triggerPayload;
    }

    public void setTriggerPayload(byte[] triggerPayload) {
        this.triggerPayload = triggerPayload;
    }

    public String getTriggerTo() {
        return triggerTo;
    }

    public void setTriggerTo(String triggerTo) {
        this.triggerTo = triggerTo;
    }

    /**
     * An expiry time for the trigger
     *
     * @return
     */
    public boolean expired() {
        if (expiresAt == -1) {
            return false;
        }
        return System.currentTimeMillis() > expiresAt;
    }

}
