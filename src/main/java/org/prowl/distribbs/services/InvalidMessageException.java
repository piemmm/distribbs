package org.prowl.distribbs.services;

public class InvalidMessageException extends Exception {

   public InvalidMessageException(String message, Throwable e) {
      super(message, e);
   }
   
}
