package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.services.aprs.APRSMessage;

public class NewAPRSMessageEvent extends BaseEvent {

   private APRSMessage message;

   public NewAPRSMessageEvent(APRSMessage message) {
      super();
      this.message = message;
   }

   public APRSMessage getMessage() {
      return message;
   }

}
