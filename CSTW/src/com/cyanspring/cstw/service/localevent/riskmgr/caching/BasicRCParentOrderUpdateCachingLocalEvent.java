/**
 * 
 */
package com.cyanspring.cstw.service.localevent.riskmgr.caching;

import java.util.Map;

import com.cyanspring.common.business.ParentOrder;
import com.cyanspring.common.event.AsyncEvent;

/**
 * @author Yu-Junfeng
 * @create 14 Sep 2015
 * designed for CachingManager
 */
public class BasicRCParentOrderUpdateCachingLocalEvent extends AsyncEvent {

private static final long serialVersionUID = 1L;
	
	private Map<String, ParentOrder> orderMap;
	
	public BasicRCParentOrderUpdateCachingLocalEvent(Map<String, ParentOrder> orderMap) {
		this.orderMap = orderMap;
	}
	
	public Map<String, ParentOrder> getOrderMap() {
		return orderMap;
	}

}
