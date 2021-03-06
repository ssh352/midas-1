package com.cyanspring.common.event.alert;

import java.util.List;

import com.cyanspring.common.event.RemoteAsyncEvent;
import com.cyanspring.common.alert.TradeAlert;

public class QueryOrderAlertReplyEvent extends RemoteAsyncEvent{
	private List<TradeAlert> trades;
	private String txId;
	private String userId;
	private boolean ok;
	private String message;
	/*
	 * if Success , ok = true ,message = "";
	 * if reject , ok = false ,message = error msg ;
	 * */
	public QueryOrderAlertReplyEvent(String key, String receiver,
			List<TradeAlert> trades, String txId, String userId, boolean ok, String message) {
		super(key, receiver);
		this.trades = trades;
		this.txId = txId;
		this.userId = userId ;
		this.ok = ok;
		this.message = message;
	}
	
	public String getTxId() {
		return txId;
	}
	public boolean isOk() {
		return ok;
	}
	public String getMessage() {
		return message;
	}
	public List<TradeAlert> getTradeAlertList() {
		return trades;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}
