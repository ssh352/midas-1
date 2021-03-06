package com.cyanspring.common.event.account;

import java.util.List;

import com.cyanspring.common.account.Account;
import com.cyanspring.common.account.User;
import com.cyanspring.common.event.RemoteAsyncEvent;

public class UserLoginReplyEvent extends RemoteAsyncEvent {
	private boolean ok;
	private String message; 
	private String txId;
	private User user;
	private Account defaultAccount;
	private List<Account> accounts;

	public UserLoginReplyEvent(String key, String receiver, User user,
			Account defaultAccount, List<Account> accounts, boolean ok,
			String message, String txId) {
		super(key, receiver);
		this.user = user;
		this.defaultAccount = defaultAccount;
		this.accounts = accounts;
		this.ok = ok;
		this.message = message;
		this.txId = txId;
	}

	public boolean isOk() {
		return ok;
	}

	public String getMessage() {
		return message;
	}

	public String getTxId() {
		return txId;
	}

	public User getUser() {
		return user;
	}

	public Account getDefaultAccount() {
		return defaultAccount;
	}

	public List<Account> getAccounts() {
		return accounts;
	}
	
}
