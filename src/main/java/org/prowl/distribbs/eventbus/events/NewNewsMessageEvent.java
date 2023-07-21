package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.objects.messages.Message;

public class NewNewsMessageEvent extends BaseEvent {

   private Message message;

   public NewNewsMessageEvent(Message message) {
      super();
      this.message = message;
   }

   public Message getMessage() {
      return message;
   }

}
