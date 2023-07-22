package org.prowl.distribbs.objects;

public class InvalidMessageException extends Exception {

    public InvalidMessageException(String message, Throwable e) {
        super(message, e);
    }

}
