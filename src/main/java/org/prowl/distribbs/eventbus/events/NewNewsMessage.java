package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.services.newsgroups.NewsMessage;

public class NewNewsMessage extends BaseEvent {

   private NewsMessage message;

   public NewNewsMessage(NewsMessage message) {
      this.message = message;
   }

   public NewsMessage getMessage() {
      return message;
   }

}
