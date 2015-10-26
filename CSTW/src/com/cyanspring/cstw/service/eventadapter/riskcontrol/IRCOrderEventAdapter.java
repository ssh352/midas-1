package com.cyanspring.cstw.service.eventadapter.riskcontrol;

import java.util.List;

import com.cyanspring.cstw.service.localevent.riskmgr.FrontRCParentOrderUpdateLocalEvent;
import com.cyanspring.cstw.service.model.riskmgr.RCOrderRecordModel;

/**
 * 
 * @author GuoWei
 * @create date 2015/08/28
 *
 */
public interface IRCOrderEventAdapter {

	List<RCOrderRecordModel> getOrderModelListByUpdateEvent(
			FrontRCParentOrderUpdateLocalEvent event);
}