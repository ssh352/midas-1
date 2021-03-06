package com.cyanspring.common.event.account;

import com.cyanspring.common.account.User;
import com.cyanspring.common.event.EventPriority;
import com.cyanspring.common.event.RemoteAsyncEvent;

public class CreateUserEvent extends RemoteAsyncEvent {
	private User user;
	private String txId;
	private String country;
	private String language;

	public CreateUserEvent(String key, String receiver, User user, String country, String language, String txId) {
		super(key, receiver);
		this.user = user;
		this.country = country;
		this.language = language;
		this.txId = txId;
		setPriority(EventPriority.HIGH);
	}

	public User getUser() {
		return user;
	}

	public String getTxId() {
		return txId;
	}
	
	public String getCountry() {
		return country;
	}
	
	public String getLanguage() {
		return language;
	}
}
