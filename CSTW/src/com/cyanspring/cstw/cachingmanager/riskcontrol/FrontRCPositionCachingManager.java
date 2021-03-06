package com.cyanspring.cstw.cachingmanager.riskcontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cyanspring.common.account.ClosedPosition;
import com.cyanspring.common.account.OpenPosition;
import com.cyanspring.common.account.OverallPosition;
import com.cyanspring.common.cstw.position.IPositionChangeListener;
import com.cyanspring.common.event.AsyncEvent;
import com.cyanspring.cstw.cachingmanager.BasicCachingManager;
import com.cyanspring.cstw.service.localevent.riskmgr.caching.FrontRCPositionUpdateCachingLocalEvent;

/**
 * 
 * @author NingXiaoFeng
 * @create date 2015/10/23
 *
 */
public final class FrontRCPositionCachingManager extends BasicCachingManager {

	private static FrontRCPositionCachingManager instance;

	private IPositionChangeListener positionChangeListener;

	// AccountId -> OverallPositionId -> OverallPosition just for guowei.
	private Map<String, Map<String, OverallPosition>> accountPositionMap;

	public static FrontRCPositionCachingManager getInstance() {
		if (instance == null) {
			instance = new FrontRCPositionCachingManager();
		}
		return instance;
	}

	private FrontRCPositionCachingManager() {
		super();

		initData();
		initListener();
	}

	private void initData() {
		accountPositionMap = new HashMap<String, Map<String, OverallPosition>>();
		List<OverallPosition> list = business.getAllPositionManager()
				.getOverAllPositionList();
		if (list != null) {
			for (OverallPosition position : list) {
				String account = position.getAccount();
				if (accountPositionMap.get(account) == null) {
					Map<String, OverallPosition> map = new HashMap<String, OverallPosition>();
					accountPositionMap.put(account, map);
				}
				accountPositionMap.get(account).put(
						position.getAccount() + position.getSymbol(), position);
			}
			sendPositionUpdateEvent();
		}
	}

	private void initListener() {
		positionChangeListener = new IPositionChangeListener() {

			@Override
			public void ClosedPositionChange(ClosedPosition arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void OpenPositionChange(OpenPosition arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void OverAllPositionChange(List<OverallPosition> list) {
				if (list != null) {
					for (OverallPosition position : list) {
						String account = position.getAccount();
						if (accountPositionMap.get(account) == null) {
							Map<String, OverallPosition> map = new HashMap<String, OverallPosition>();
							accountPositionMap.put(account, map);
						}
						accountPositionMap.get(account).put(
								position.getAccount() + position.getSymbol(),
								position);
					}
					sendPositionUpdateEvent();
				}
			}

		};

		business.getAllPositionManager().addIPositionChangeListener(
				positionChangeListener);
	}

	protected void sendPositionUpdateEvent() {
		FrontRCPositionUpdateCachingLocalEvent updateEvent = new FrontRCPositionUpdateCachingLocalEvent(
				accountPositionMap);
		business.getEventManager().sendEvent(updateEvent);
	}

	@Override
	protected List<Class<? extends AsyncEvent>> getReplyEventList() {
		List<Class<? extends AsyncEvent>> list = new ArrayList<Class<? extends AsyncEvent>>();
		return list;
	}

	@Override
	protected void processAsyncEvent(AsyncEvent event) {

	}

}
