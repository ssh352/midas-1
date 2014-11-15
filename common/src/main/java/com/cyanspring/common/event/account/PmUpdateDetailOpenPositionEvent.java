package com.cyanspring.common.event.account;

import com.cyanspring.common.account.OpenPosition;
import com.cyanspring.common.event.AsyncEvent;

public class PmUpdateDetailOpenPositionEvent extends AsyncEvent {
	private OpenPosition position;

	public PmUpdateDetailOpenPositionEvent(String key, OpenPosition position) {
		super(key);
		this.position = position;
	}

	public OpenPosition getPosition() {
		return position;
	}
	
	
}
