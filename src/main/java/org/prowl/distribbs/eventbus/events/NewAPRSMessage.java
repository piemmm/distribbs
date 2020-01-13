package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.services.aprs.APRSMessage;

public class NewAPRSMessage extends BaseEvent {

   private APRSMessage message;

   public NewAPRSMessage(APRSMessage message) {
      this.message = message;
   }

   public APRSMessage getMessage() {
      return message;
   }

}
