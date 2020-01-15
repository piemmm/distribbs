package org.prowl.distribbs.eventbus.events;

import org.prowl.distribbs.services.newsgroups.NewsMessage;

public class NewNewsMessageEvent extends BaseEvent {

   private NewsMessage message;

   public NewNewsMessageEvent(NewsMessage message) {
      super();
      this.message = message;
   }

   public NewsMessage getMessage() {
      return message;
   }

}
