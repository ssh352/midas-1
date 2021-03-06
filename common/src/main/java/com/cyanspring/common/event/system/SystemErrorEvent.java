package com.cyanspring.common.event.system;

import com.cyanspring.common.event.RemoteAsyncEvent;

public class SystemErrorEvent extends RemoteAsyncEvent {
	private int errorCode;
	private String message;
	
	public SystemErrorEvent(String key, String receiver, int errorCode,
			String message) {
		super(key, receiver);
		this.errorCode = errorCode;
		this.message = message;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return message;
	}
	
	
}
