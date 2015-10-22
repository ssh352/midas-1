package com.cyanspring.common.cstw.position;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cyanspring.common.account.ClosedPosition;
import com.cyanspring.common.account.OpenPosition;
import com.cyanspring.common.account.OverallPosition;
import com.cyanspring.common.account.UserGroup;
import com.cyanspring.common.event.AsyncEvent;
import com.cyanspring.common.event.IAsyncEventListener;
import com.cyanspring.common.event.IRemoteEventManager;
import com.cyanspring.common.event.account.ClosedPositionUpdateEvent;
import com.cyanspring.common.event.account.OpenPositionDynamicUpdateEvent;
import com.cyanspring.common.event.account.OpenPositionUpdateEvent;
import com.cyanspring.common.event.account.OverAllPositionReplyEvent;
import com.cyanspring.common.event.account.OverAllPositionRequestEvent;
import com.cyanspring.common.util.IdGenerator;
import com.cyanspring.common.util.PriceUtils;

public class AllPositionManager implements IAsyncEventListener {

	private static final Logger log = LoggerFactory.getLogger(AllPositionManager.class);
	private IRemoteEventManager eventManager;
	private String server = "";
	private List<String> accountIdList = null;
	private UserGroup loginUser = null;
	private ConcurrentHashMap<String, List<OverallPosition>> allPositionMap = new ConcurrentHashMap<String, List<OverallPosition>>();
	private ConcurrentHashMap<String, List<OpenPosition>> openPositionMap = new ConcurrentHashMap<String, List<OpenPosition>>();
	private ConcurrentHashMap<String, List<ClosedPosition>> closedPositionMap = new ConcurrentHashMap<String, List<ClosedPosition>>();
	private List<IPositionChangeListener> listenerList;

	public AllPositionManager() {
		
	}
	
	public AllPositionManager(IRemoteEventManager eventManager) {
		this.eventManager = eventManager;
	}
	
	@Override
	public void onEvent(AsyncEvent event) {
		if(event instanceof OverAllPositionReplyEvent){
			OverAllPositionReplyEvent e = (OverAllPositionReplyEvent) event;
			updateOpenPositionList(e.getOpenPositionList());
			updateClosedPositionList(e.getClosedPositionList());
			refreshOverallPosition(null);
		}else if(event instanceof OpenPositionUpdateEvent){
			OpenPositionUpdateEvent e = (OpenPositionUpdateEvent) event;
			updatePosition(e.getPosition(),true);			
		}else if(event instanceof OpenPositionDynamicUpdateEvent){
			OpenPositionDynamicUpdateEvent e = (OpenPositionDynamicUpdateEvent) event;
			updatePosition(e.getPosition(),true);			
		}else if(event instanceof ClosedPositionUpdateEvent){
			ClosedPositionUpdateEvent e = (ClosedPositionUpdateEvent) event;
			updatePosition(e.getPosition(),true);
		}
	}
	
	private void updateClosedPositionList(
			List<ClosedPosition> closedPositionList) {
		closedPositionMap.clear();
		for(ClosedPosition pos : closedPositionList){
			updatePosition(pos,false);
		}		
	}

	private void updateOpenPositionList(List<OpenPosition> openPositionList) {
		openPositionMap.clear();
		for(OpenPosition pos : openPositionList){
			updatePosition(pos,false);
		}
	}
	
	private void updatePosition(OpenPosition position,boolean isDynamic){
		if( null == position || !inGroup(position.getAccount()))
			return;
		
		String account = position.getAccount();
		List<OpenPosition> oldList = openPositionMap.get(account);
		if(null == oldList){
			oldList = new ArrayList<OpenPosition>();
			oldList.add(position);
			openPositionMap.put(account, oldList);
		}else{
			List <OpenPosition>tempList = new ArrayList<OpenPosition>();
			boolean isNewSymbol = true;
			for(OpenPosition op:oldList){
				if(position.getSymbol().equals(op.getSymbol())){
					isNewSymbol = false;
					tempList.add(position);
				}else{
					tempList.add(op);
				}
			}
			if(isNewSymbol){
				tempList.add(position);
			}
			openPositionMap.put(account, tempList);
		}
		
		if(isDynamic){
			refreshOverallPosition(position.getId());
			notifyOpenPositionChange(position);
		}
	}

	private void updatePosition(ClosedPosition position,boolean isDynamic){
		if( null == position || !inGroup(position.getAccount()))
			return;
				
		String account = position.getAccount();
		if(isDynamic){
			List<OpenPosition> tempList = new ArrayList<OpenPosition>();
			if(openPositionMap.containsKey(account)){
				List <OpenPosition>oldList = openPositionMap.get(account);
				for(OpenPosition op:oldList){
					if(!position.getSymbol().equals(op.getSymbol())){
						tempList.add(op);
					}
				}
				openPositionMap.put(account, tempList);
			}
		}
	
		List<ClosedPosition> oldList = closedPositionMap.get(account);
		if(null == oldList){
			oldList = new ArrayList<ClosedPosition>();
		}
		oldList.add(position);
		closedPositionMap.put(account, oldList);
		
		if(isDynamic){
			refreshOverallPosition(position.getId());
			notifyClosedPositionChange(position);
		}
	}
	
	private void notifyOpenPositionChange(OpenPosition position) {
		for(IPositionChangeListener listener : listenerList){
			listener.OpenPositionChange(position);
		}
	}
	
	private void notifyClosedPositionChange(ClosedPosition position) {
		for(IPositionChangeListener listener : listenerList){
			listener.ClosedPositionChange(position);
		}		
	}
	
	private void notifyOverallPositionChange() {
		for(IPositionChangeListener listener : listenerList){
			listener.OverAllPositionChange(getOverAllPositionList());
		}	
	}

	synchronized private void refreshOverallPosition(String positionAccount) {
		Set<String> idSet = new HashSet<String>();
		
		if(null == positionAccount){
			if( null != accountIdList && !accountIdList.isEmpty())
				idSet.addAll(accountIdList);
			else{
				idSet.addAll(openPositionMap.keySet());
				idSet.addAll(closedPositionMap.keySet());
			}
		}else{
			idSet.add(positionAccount);
		}

		
		for(String id : idSet){
			List<OpenPosition> oList = openPositionMap.get(id);
			List<ClosedPosition> cList = closedPositionMap.get(id);
			if(null == oList)
				oList = new ArrayList<OpenPosition>();
			if(null == cList)
				cList = new ArrayList<ClosedPosition>();
			
			List<OverallPosition> allList = allPositionMap.get(id);
			if( null == allList)
				allList = new ArrayList<OverallPosition>();
			
			Set <String>symbolSet = collectSymbol(id);
			
			for(String symbol : symbolSet){
				OverallPosition oap = new OverallPosition();
				oap.setAccount(id);
				oap.setSymbol(symbol);
				for(OpenPosition op : oList){
					if(!op.getSymbol().equals(symbol))
						continue;
					
					double qty = op.getQty();
					double price = op.getPrice();
					if(op.isBuy()){
						if(PriceUtils.isZero(oap.getBuyPrice()))
							oap.setBuyPrice(price);
						else
							oap.setBuyPrice((oap.getBuyPrice()+price)/2);
						
						oap.setBuyQty(oap.getBuyQty()+Math.abs(qty));
						
					}else{
						if(PriceUtils.isZero(oap.getSellPrice()))
							oap.setSellPrice(price);
						else
							oap.setSellPrice((oap.getSellPrice()+price)/2);
						
						oap.setSellQty(oap.getSellQty()+Math.abs(qty));
						
					}
					
					oap.setPnL(op.getPnL()+oap.getPnL());
					oap.setUrPnL(op.getPnL()+oap.getUrPnL());
				}
				
				for(ClosedPosition cp : cList){
					if(!cp.getSymbol().equals(symbol))
						continue;
					
					double qty = cp.getQty();
					
					if(PriceUtils.isZero(oap.getBuyPrice()))
						oap.setBuyPrice(cp.getBuyPrice());
					else
						oap.setBuyPrice((oap.getBuyPrice()+cp.getBuyPrice())/2);
					
					if(PriceUtils.isZero(oap.getSellPrice()))
						oap.setSellPrice(cp.getSellPrice());
					else
						oap.setSellPrice((oap.getSellPrice()+cp.getSellPrice())/2);
					
					if(cp.isBuy())
						oap.setBuyQty(oap.getBuyQty()+Math.abs(qty));
					else
						oap.setSellQty(oap.getSellQty()+Math.abs(qty));

					oap.setPnL(cp.getPnL()+oap.getPnL());

				}
				
				oap.setLastUpdate(new Date());
				allList.add(oap);
			}
			
		}
		notifyOverallPositionChange();
	}

	private Set<String> collectSymbol(String id) {
		Set <String> symbolSet = new HashSet<String>();
		List<OpenPosition> oList = openPositionMap.get(id);
		List<ClosedPosition> cList = closedPositionMap.get(id);
		
		for(OpenPosition op : oList){
			symbolSet.add(op.getSymbol());
		}
		
		for(ClosedPosition cp : cList){
			symbolSet.add(cp.getSymbol());
		}
		
		return symbolSet;
	}

	private boolean inGroup(String account){
		
		if(null != loginUser && loginUser.isAdmin())
			return true;
		
		if(null != accountIdList && accountIdList.contains(account))
			return true;

		return false;
	}
	
	public void init(IRemoteEventManager eventManager
			,String server,List<String> accountIdList,UserGroup loginUser){
		
		if(null == eventManager || !StringUtils.hasText(server)){
			log.info("init error: eventManager is null or server is empty");
		}
		
		this.eventManager = eventManager;
		this.server = server;
		this.accountIdList = accountIdList;
		this.loginUser = loginUser;
		
		subEvent(OverAllPositionReplyEvent.class);
		subEvent(OpenPositionUpdateEvent.class);
		subEvent(OpenPositionDynamicUpdateEvent.class);
		subEvent(ClosedPositionUpdateEvent.class);
		requestOverAllPosition(accountIdList);
	}
	
	public void unInit(){
		unSubEvent(OverAllPositionReplyEvent.class);
		unSubEvent(OpenPositionUpdateEvent.class);
		unSubEvent(OpenPositionDynamicUpdateEvent.class);
		unSubEvent(ClosedPositionUpdateEvent.class);
	}
	
	
	public void addIPositionChangeListener(IPositionChangeListener listener) {
		listenerList.add(listener);
	}

	public void removeIPositionChangeListener(IPositionChangeListener listener) {
		listenerList.remove(listener);
	}
	
	public void requestOverAllPosition(List<String> accountIdList) {
		OverAllPositionRequestEvent event = new OverAllPositionRequestEvent(IdGenerator
				.getInstance().getNextID(), server, accountIdList);
		try {
			eventManager.sendRemoteEvent(event);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}
	
	private void subEvent(Class<? extends AsyncEvent> clazz) {
		eventManager.subscribe(clazz, this);
	}

	private void unSubEvent(Class<? extends AsyncEvent> clazz) {
		eventManager.unsubscribe(clazz, this);
	}
	
	public List<OverallPosition> getOverAllPositionList(){
		if( null == allPositionMap)
			return null;
		
		List <OverallPosition> list = new ArrayList<OverallPosition>();
		Iterator <List<OverallPosition>> positionIte = allPositionMap.values().iterator();
		while(positionIte.hasNext()){
			List ovps = positionIte.next();
			list.addAll(ovps);
		}
		return list;
	}
}
