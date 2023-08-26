package org.prowl.distribbs.node.connectivity.ax25;

public class InterfaceStatus {

    private State state;
    private String message;

    public InterfaceStatus(State state, String message) {
        this.state = state;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public State getState() {
        return state;
    }

    public enum State {
        OK,
        WARN,
        ERROR;
    }
}
