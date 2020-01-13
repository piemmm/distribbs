package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.services.chat.ChatMessage;

public class NewChatMessage extends BaseEvent {

   private ChatMessage message;

   private NewChatMessage(ChatMessage message) {
      this.message = message;
   }

   public ChatMessage getMessage() {
      return message;
   }

}
