package com.cyanspring.cstw.service.eventadapter.riskcontrol;

import java.util.List;

import com.cyanspring.cstw.service.localevent.riskmgr.caching.BasicRCPositionUpdateCachingLocalEvent;
import com.cyanspring.cstw.service.model.riskmgr.RCOpenPositionModel;

/**
 * 
 * @author NingXiaoFeng
 * @create date 2015/08/28
 *
 */
public interface IRCOpenPositionEventAdapter {
	List<RCOpenPositionModel> getOpenPositionModelListByEvent(
			BasicRCPositionUpdateCachingLocalEvent event);
}
