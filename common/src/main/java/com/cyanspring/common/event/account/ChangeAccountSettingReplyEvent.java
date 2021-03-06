package com.cyanspring.common.event.account;

import com.cyanspring.common.account.AccountSetting;
import com.cyanspring.common.event.RemoteAsyncEvent;
import com.cyanspring.common.message.ExtraEventMessage;
import com.cyanspring.common.message.ExtraEventMessageBuilder;

public class ChangeAccountSettingReplyEvent extends RemoteAsyncEvent {
	private AccountSetting accountSetting;
	private boolean ok;
	private String message;
	private ExtraEventMessageBuilder messageBuilder;
	public ChangeAccountSettingReplyEvent(String key, String receiver,
			AccountSetting accountSetting, boolean ok, String message,ExtraEventMessageBuilder messageBuilder) {
		super(key, receiver);
		this.accountSetting = accountSetting;
		this.ok = ok;
		this.message = message;
		this.messageBuilder = messageBuilder;
	}

	public AccountSetting getAccountSetting() {
		return accountSetting;
	}

	public boolean isOk() {
		return ok;
	}

	public String getMessage() {
		return message;
	}

	public String getStopLiveTradingStartTime(){
		
		if( null == messageBuilder)
			return null;
		
		return messageBuilder.getMessage(ExtraEventMessage.USER_STOP_LIVE_TRADING_START_TIME);
	}
	public String getStopLiveTradingEndTime(){
		
		if( null == messageBuilder)
			return null;
		
		return messageBuilder.getMessage(ExtraEventMessage.USER_STOP_LIVE_TRADING_END_TIME);
	}
}
