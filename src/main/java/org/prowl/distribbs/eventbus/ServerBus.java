package org.prowl.distribbs.eventbus;

import org.prowl.distribbs.eventbus.events.BaseEvent;

import com.google.common.eventbus.EventBus;

public enum ServerBus {
	
	INSTANCE;
	
	private EventBus eventBus;
	
	ServerBus() {
		eventBus = new EventBus();
	}
	 
	public void post(BaseEvent event) {
		eventBus.post(event);
	}
	
	public void register(Object o) {
		eventBus.register(o);
	}
	
	public void post(Object o) {
	   eventBus.post(o);
	}
	
	
}