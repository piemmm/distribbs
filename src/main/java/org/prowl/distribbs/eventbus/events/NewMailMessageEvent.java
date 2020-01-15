package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.services.messages.MailMessage;

public class NewMailMessageEvent extends BaseEvent {

   private MailMessage message;

   public NewMailMessageEvent(MailMessage message) {
      super();
      this.message = message;
   }

   public MailMessage getMessage() {
      return message;
   }

}
