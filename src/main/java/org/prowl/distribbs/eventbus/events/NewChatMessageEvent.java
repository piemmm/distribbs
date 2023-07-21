package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.objects.chat.ChatMessage;

public class NewChatMessageEvent extends BaseEvent {

   private ChatMessage message;

   public NewChatMessageEvent(ChatMessage message) {
      super();
      this.message = message;
   }

   public ChatMessage getMessage() {
      return message;
   }

}
