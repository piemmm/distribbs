package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.services.messages.MailMessage;

public class NewMailMessage extends BaseEvent {

   private MailMessage message;

   public NewMailMessage(MailMessage message) {
      super();
      this.message = message;
   }

   public MailMessage getMessage() {
      return message;
   }

}
