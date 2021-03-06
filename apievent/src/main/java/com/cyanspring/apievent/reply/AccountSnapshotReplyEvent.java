package com.cyanspring.apievent.reply;

import com.cyanspring.apievent.obj.Account;
import com.cyanspring.apievent.obj.Execution;
import com.cyanspring.apievent.obj.OpenPosition;
import com.cyanspring.common.event.RemoteAsyncEvent;

import java.util.List;

/**
 * @author elviswu
 * @version 1.0
 * @since 1.0
 */

public class AccountSnapshotReplyEvent extends RemoteAsyncEvent {
	private Account account;
	private List<OpenPosition> openPositions;
//	private List<ClosedPosition> closedPositions;
	private List<Execution> executions;
	private String txId;

	public AccountSnapshotReplyEvent(String key, String receiver,
									 Account account,
									 List<OpenPosition> openPositions,
//									 List<ClosedPosition> closedPositions,
									 List<Execution> executions,
									 String txId) {
		super(key, receiver);
		this.account = account;
		this.openPositions = openPositions;
//		this.closedPositions = closedPositions;
		this.executions = executions;
		this.txId = txId;
	}

	public Account getAccount() {
		return account;
	}
	public List<OpenPosition> getOpenPositions() {
		return openPositions;
	}
//	public List<ClosedPosition> getClosedPositions() {
//		return closedPositions;
//	}
	public List<Execution> getExecutions() {
		return executions;
	}
	public String getTxId() {
		return txId;
	}
	
}
