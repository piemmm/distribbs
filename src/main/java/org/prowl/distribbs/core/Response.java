package org.prowl.distribbs.core;

public class Response {

   private long   responseTime = 0;
   private String from;

   public Response() {

   }

   public long getResponseTime() {
      return responseTime;
   }

   public void setResponseTime(long responseTime) {
      this.responseTime = responseTime;
   }

   public String getFrom() {
      return from;
   }

   public void setFrom(String from) {
      this.from = from;
   }

}
