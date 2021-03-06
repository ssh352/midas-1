/*******************************************************************************
 * Copyright (c) 2011-2012 Cyan Spring Limited
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms specified by license file attached.
 *
 * Software distributed under the License is released on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 ******************************************************************************/
package com.cyanspring.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.cyanspring.common.business.ChildOrder;
import com.cyanspring.common.business.Execution;
import com.cyanspring.common.business.Instrument;
import com.cyanspring.common.business.MultiInstrumentStrategyData;
import com.cyanspring.common.business.OrderField;
import com.cyanspring.common.business.ParentOrder;
import com.cyanspring.common.data.DataObject;
import com.cyanspring.common.event.AsyncEventProcessor;
import com.cyanspring.common.event.IAsyncEventManager;
import com.cyanspring.common.event.IRemoteEventManager;
import com.cyanspring.common.event.account.InternalResetAccountRequestEvent;
import com.cyanspring.common.event.account.PmEndOfDayRollEvent;
import com.cyanspring.common.event.account.ResetAccountRequestEvent;
import com.cyanspring.common.event.marketsession.EndOfDayEvent;
import com.cyanspring.common.event.order.AllExecutionSnapshotReplyEvent;
import com.cyanspring.common.event.order.AllExecutionSnapshotRequestEvent;
import com.cyanspring.common.event.order.AllStrategySnapshotReplyEvent;
import com.cyanspring.common.event.order.AllStrategySnapshotRequestEvent;
import com.cyanspring.common.event.order.ChildOrderSnapshotEvent;
import com.cyanspring.common.event.order.ChildOrderSnapshotRequestEvent;
import com.cyanspring.common.event.order.ChildOrderUpdateEvent;
import com.cyanspring.common.event.order.ExecutionSnapshotEvent;
import com.cyanspring.common.event.order.ExecutionSnapshotRequestEvent;
import com.cyanspring.common.event.order.ParentOrderUpdateEvent;
import com.cyanspring.common.event.order.StrategySnapshotEvent;
import com.cyanspring.common.event.order.StrategySnapshotRequestEvent;
import com.cyanspring.common.event.order.UpdateChildOrderEvent;
import com.cyanspring.common.event.order.UpdateParentOrderEvent;
import com.cyanspring.common.event.strategy.MultiInstrumentStrategyUpdateEvent;
import com.cyanspring.common.event.strategy.SingleInstrumentStrategyUpdateEvent;
import com.cyanspring.common.staticdata.CumQuantitySaver;
import com.cyanspring.common.staticdata.OrderSaver;
import com.cyanspring.common.type.ExecType;
import com.cyanspring.common.util.DualKeyMap;
import com.cyanspring.common.util.DualMap;

public class OrderManager {
	private static final Logger log = LoggerFactory
			.getLogger(OrderManager.class);
	private DualKeyMap<String, String, ParentOrder> parentOrders = new DualKeyMap<String, String, ParentOrder>();
	private HashMap<String, Map<String, ChildOrder>> archiveChildOrders = new HashMap<String, Map<String, ChildOrder>>();
	private HashMap<String, Map<String, ChildOrder>> activeChildOrders = new HashMap<String, Map<String, ChildOrder>>();
	private HashMap<String, List<Execution>> executions = new HashMap<String, List<Execution>>();
	private DualKeyMap<String, String, MultiInstrumentStrategyData> strategyData = new DualKeyMap<String, String, MultiInstrumentStrategyData>();
	private DualKeyMap<String, String, Instrument> instruments = new DualKeyMap<String, String, Instrument>();
	private List<String> removedOrders = new ArrayList<>();
	private boolean publishChildOrder;

	@Autowired
	private IRemoteEventManager eventManager;
	private Map<String, String> clientChildOrderSubscription = new HashMap<String, String>();
	private Map<String, String> clientExecutionSubscription = new HashMap<String, String>();

    @Autowired(required = false)
    OrderSaver parentOrderSaver;

    @Autowired(required = false)
    OrderSaver childOrderSaver;

    @Autowired(required = false)
    CumQuantitySaver cumQuantitySaver;

	@Autowired
	@Qualifier("fixToOrderMap")
	private DualMap<Integer, String> fixToOrderMap;

    private int asyncSendBatch = 3000;
    private long asyncSendInterval = 2000;
    private long orderLimit = 15000;
	private AsyncEventProcessor eventProcessor = new AsyncEventProcessor() {

		@Override
		public void subscribeToEvents() {
			subscribeToEvent(StrategySnapshotRequestEvent.class, null);
			subscribeToEvent(ChildOrderSnapshotRequestEvent.class, null);
			subscribeToEvent(ExecutionSnapshotRequestEvent.class, null);
			subscribeToEvent(UpdateParentOrderEvent.class, null);
			subscribeToEvent(UpdateChildOrderEvent.class, null);
			subscribeToEvent(SingleInstrumentStrategyUpdateEvent.class, null);
			subscribeToEvent(MultiInstrumentStrategyUpdateEvent.class, null);
			subscribeToEvent(InternalResetAccountRequestEvent.class, null);
			subscribeToEvent(EndOfDayEvent.class, null);
			subscribeToEvent(AllStrategySnapshotRequestEvent.class,null);
			subscribeToEvent(AllExecutionSnapshotRequestEvent.class,null);

		}

		@Override
		public IAsyncEventManager getEventManager() {
			return eventManager;
		}
	};

	public boolean parentOrderExists(String id) {
		return parentOrders.containsKey(id);
	}

	public OrderManager() {
	}

	public void processAllExecutionSnapshotRequestEvent(AllExecutionSnapshotRequestEvent event) {
		log.info("start send AllExecutionSnapshotRequestEvent:{}",event.getSender());
		asyncSendExecutionSnapshot(event);
	}
	
    private void asyncSendExecutionSnapshot(final AllExecutionSnapshotRequestEvent event) {
        
    	Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				List<String> idList = event.getAccountIdList();
				List<Execution> exeList = new ArrayList<>();
				
				if(null == idList){ // get all
					Iterator <List<Execution>>ite = executions.values().iterator();
					while(ite.hasNext()){
						exeList.addAll(ite.next());
						if(isOverOrderLimit(exeList))
							return;
					}
					
				}else{
					for(String id:idList){
						
						Iterator <List<Execution>>ite = executions.values().iterator();
						while(ite.hasNext()){
							List<Execution> tmpList = ite.next();
							if(null == tmpList)
								continue;
					
							for(Execution exe:tmpList){
								if(exe.getAccount().equals(id)){
									exeList.add(exe);
    							}
							}
						}
						
						if(isOverOrderLimit(exeList))
							return;
					}
				}
				log.info("total executions:{} ready to send",exeList.size());
				
				List<Execution> tempList = new ArrayList<>();
				for(Execution exe : exeList){
					tempList.add(exe);
					if(tempList.size()>= asyncSendBatch){
						sendExecutionReplyEvent(tempList);
						tempList.clear();
					}
				}
				
				if(tempList.size()>0){
					sendExecutionReplyEvent(tempList);
					tempList.clear();
				}
			}

			private void sendExecutionReplyEvent(List<Execution> tempList) {
                try {
                	log.info("send AllExecutionSnapshotReplyEvent:{}",tempList.size());
					AllExecutionSnapshotReplyEvent e = new AllExecutionSnapshotReplyEvent(event.getKey()
							,event.getSender(),true,"",tempList);
					eventManager.sendRemoteEvent(e);
					Thread.sleep(asyncSendInterval);
				} catch (InterruptedException e) {
					log.warn(e.getMessage(),e);
				} catch (Exception e){
					log.warn(e.getMessage(),e);
				}
			}

			private boolean isOverOrderLimit(List<Execution> exeList) {
				if(exeList.size() > orderLimit){
					log.info("Execution list over limit number:{},{}",exeList.size(),orderLimit);
					AllExecutionSnapshotReplyEvent e = new AllExecutionSnapshotReplyEvent(event.getKey()
							,event.getSender(),false,"Over transfer limit:"+orderLimit,null);
					try {
						eventManager.sendRemoteEvent(e);
					} catch (Exception e1) {
						log.warn(e1.getMessage(),e1);
					}
					return true;
				}
				return false;
			}
        	
        });
        thread.start();
    }
	
	public void processAllStrategySnapshotRequestEvent(AllStrategySnapshotRequestEvent event) {
		log.info("start send All StrategySnapshotRequest");
		asyncSendStrategySnapshot(event);
	}

    private void asyncSendStrategySnapshot(final AllStrategySnapshotRequestEvent event) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
            	List <String> ids = event.getAccountIdList();
        		List<ParentOrder> orderList = null;
        		List<Instrument> instList = null;
        		List<MultiInstrumentStrategyData> misdList = null;

        		orderList = new ArrayList<ParentOrder>();
        		instList = new ArrayList<Instrument>(instruments.getMap(event.getKey()).values());
        		misdList = new ArrayList<MultiInstrumentStrategyData>(strategyData.getMap(event.getKey()).values());

        		if(null != ids){
            		for(String id: ids){
            			log.info("account id:{}",id);
            			if(null != parentOrders.getMap(id).values()
            					&& parentOrders.getMap(id).values().size()!=0){
            				orderList.addAll(parentOrders.getMap(id).values());
            			}

            			if (null != instruments.getMap(id).values()
            					&& instruments.getMap(id).values().size()!=0) {
            				instList.addAll(instruments.getMap(id).values());
            			}

            			if (null != strategyData.getMap(id).values()
            					&& strategyData.getMap(id).values().size()!=0) {
            				misdList.addAll(strategyData.getMap(id).values());
            			}
            		}
            	} else {
            		orderList.addAll(parentOrders.values());
            		instList.addAll(instruments.values());
            		misdList.addAll(strategyData.values());
            	}

        		log.info("orderList size:{},instList:{},misdList:{}"
        				,new Object[]{orderList.size(),instList.size(),misdList.size()});

        		if(orderList.size()+instList.size()+misdList.size()>orderLimit){
        			log.info("Over order transfer limit:"+orderLimit);
        			AllStrategySnapshotReplyEvent reply = new AllStrategySnapshotReplyEvent(event.getKey()
        					,event.getSender(), null, null, null, false, "Over order transfer limit:" + orderLimit);
                    try {
						eventManager.sendRemoteEvent(reply);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
                    return;
        		}

        		order:{
        			if (null == orderList || orderList.size() <= 0) {
        				break order;
        			}
            		List<ParentOrder> tempOrder = new ArrayList<ParentOrder>();
            		int count = 0;
            		for (ParentOrder order : orderList) {
             			count++;
            			tempOrder.add(order);
            			if (count % asyncSendBatch == 0 || orderList.size() == count) {
                			AllStrategySnapshotReplyEvent reply = new AllStrategySnapshotReplyEvent(event.getKey(),
                					event.getSender(), tempOrder, null, null, true, "");
                            try {
                            	log.info("send orders:{},{}", event.getSender(), tempOrder.size());
								eventManager.sendRemoteEvent(reply);
                                Thread.sleep(asyncSendInterval);
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							}
            				tempOrder.clear();
            			}
            		}
        		}

        		instrus:{
        			if (null == instList || instList.size() <= 0) {
        				break instrus;
        			}
            		List<Instrument> tempInstrus = new ArrayList<Instrument>();
            		int count = 0;
            		for (Instrument instrument : instList) {
            			count++;
            			tempInstrus.add(instrument);
            			if (count % asyncSendBatch == 0 || instList.size() == count) {
                			AllStrategySnapshotReplyEvent reply = new AllStrategySnapshotReplyEvent(event.getKey(),
                					event.getSender(), null, tempInstrus, null, true, "");
                            try {
                            	log.info("send instrus: {}, {}", event.getSender(), tempInstrus.size());
								eventManager.sendRemoteEvent(reply);
                                Thread.sleep(asyncSendInterval);
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							}
                            tempInstrus.clear();
            			}
            		}
        		}


        		strategys:{

        			if (null == misdList || misdList.size() <=0) {
        				break strategys;
        			}
            		List<MultiInstrumentStrategyData> tempStrategys = new ArrayList<MultiInstrumentStrategyData>();
            		int count = 0;
            		for (MultiInstrumentStrategyData misd : misdList) {
            			count++;
            			tempStrategys.add(misd);
            			if (count % asyncSendBatch == 0 || misdList.size() == count) {
                			AllStrategySnapshotReplyEvent reply = new AllStrategySnapshotReplyEvent(event.getKey(),
                					event.getSender(), null, null, tempStrategys, true, "");
                            try {
                            	log.info("send Strategys: {}, {}", event.getSender(), tempStrategys.size());
								eventManager.sendRemoteEvent(reply);
                                Thread.sleep(asyncSendInterval);
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							}
                            tempStrategys.clear();
            			}
            		}
        		}
            }
        });
        thread.start();
	}

	public void processInternalResetAccountRequestEvent(InternalResetAccountRequestEvent event) {
		ResetAccountRequestEvent evt = event.getEvent();
		String account = evt.getAccount();
		Map<String, ParentOrder> map = parentOrders.removeMap(account);
		if (null == map) {
			return;
		}
		for (ParentOrder parent: map.values()) {
			archiveChildOrders.remove(parent.getId());
			activeChildOrders.remove(parent.getId());
			executions.remove(parent.getId());
			removedOrders.add(parent.getId());
		}
		log.info("Reset account removes orders: " + map.size() + ", account: " + account);
	}

	public void processChildOrderSnapshotRequestEvent(
			ChildOrderSnapshotRequestEvent event) throws Exception {
		String client = event.getSender();
		String id = event.getKey();
		// Only allow one child order subscription per client for time being to
		// avoid flooding transport
		clientChildOrderSubscription.put(client, id);
		log.debug("Adding child order subscription: " + client + " : " + id);

		Map<String, ChildOrder> map = activeChildOrders.get(id);
		List<ChildOrder> orders = null;
		if (null != map) {
			orders = new ArrayList<ChildOrder>(map.values());
		} else {
			orders = new ArrayList<ChildOrder>();
		}
		ChildOrderSnapshotEvent reply = new ChildOrderSnapshotEvent(id, client,
				orders);
		eventManager.sendRemoteEvent(reply);
	}

	public void processExecutionSnapshotRequestEvent(
			ExecutionSnapshotRequestEvent event) throws Exception {
		String client = event.getSender();
		String id = event.getKey();
		// Only allow one execution subscription per client for time being to
		// avoid flooding transport
		clientExecutionSubscription.put(client, id);
		log.debug("Adding execution subscription: " + client + " : " + id);

		ExecutionSnapshotEvent reply = new ExecutionSnapshotEvent(id, client,
				executions.get(id));
		eventManager.sendRemoteEvent(reply);
	}

	public void processMultiInstrumentStrategyUpdateEvent(
			MultiInstrumentStrategyUpdateEvent event) throws Exception {
		String id = event.getStrategyData().getId();
		String account = event.getStrategyData().getAccount();
		strategyData.put(id, account, event.getStrategyData());
		eventManager.sendRemoteEvent(event);
	}

	public void processSingleInstrumentStrategyUpdateEvent(
			SingleInstrumentStrategyUpdateEvent event) throws Exception {
		String id = event.getInstrument().getId();
		String account = event.getInstrument().getAccount();
		instruments.put(id, account, event.getInstrument());
		eventManager.sendRemoteEvent(event);
	}

	public void processUpdateChildOrderEvent(UpdateChildOrderEvent event)
			throws Exception {
		log.debug("Received child order update: " + event.getExecType() + " - "
				+ event.getOrder());

		ChildOrder child = event.getOrder();
		if (child.getOrdStatus().isCompleted()) {
			Map<String, ChildOrder> actives = activeChildOrders.get(child
					.getStrategyId());
			if (null == actives) {
				log.warn("Completed child order without existing record: "
						+ child + "; " + child.getStrategyId());
			} else {
				actives.remove(child.getId());
			}

			Map<String, ChildOrder> archives = archiveChildOrders.get(child
					.getStrategyId());
			if (null == archives) {
				archives = new HashMap<String, ChildOrder>();
				archiveChildOrders.put(child.getStrategyId(), archives);
			}
			archives.put(child.getId(), child);
		} else {
			Map<String, ChildOrder> children = activeChildOrders.get(child
					.getStrategyId());
			if (null == children) {
				log.warn("Cant find child order group: " + child + "; "
						+ child.getStrategyId());
				children = new HashMap<String, ChildOrder>();
				activeChildOrders.put(child.getStrategyId(), children);
			}
			children.put(child.getId(), child);
		}

		if (event.getExecution() != null) {
			addExecution(event.getExecution());
		}
		log.info("send child order update:{},{}",publishChildOrder,child.getAccount());
		if (publishChildOrder) {
			eventManager.sendRemoteEvent(new ChildOrderUpdateEvent(child
					.getStrategyId(), null, event.getExecType(),
					child, event.getExecution(), null));
		} else {
			// loop through child order subscriptions to check which CSTW is
			// interested in
			// this child order
			for (Entry<String, String> entry : clientChildOrderSubscription
					.entrySet()) {
				if (entry.getValue().equals(child.getStrategyId())) {
					eventManager.sendRemoteEvent(new ChildOrderUpdateEvent(child
							.getStrategyId(), entry.getKey(), event.getExecType(),
							child, event.getExecution(), null));
				}
			}
		}

	}

	private void addExecution(Execution execution) {
		List<Execution> list = executions.get(execution.getStrategyId());
		if (null == list) {
			list = new ArrayList<Execution>();
			executions.put(execution.getStrategyId(), list);
		}
		list.add(execution);
	}

	public void processStrategySnapshotRequestEvent(
			StrategySnapshotRequestEvent event) throws Exception {
		log.info("Received StrategySnapshotRequestEvent: " + event.getKey() + ", " + event.getTxId());
		List<ParentOrder> orderList = null;
		List<Instrument> instList = null;
		List<MultiInstrumentStrategyData> misdList = null;

		orderList = new ArrayList<ParentOrder>(parentOrders.getMap(event.getKey()).values());
		instList = new ArrayList<Instrument>(instruments.getMap(event.getKey()).values());
		misdList = new ArrayList<MultiInstrumentStrategyData>(strategyData.getMap(event.getKey()).values());

		StrategySnapshotEvent reply = new StrategySnapshotEvent(event.getKey(),
					event.getSender(),
						orderList,
						instList,
						misdList,
						event.getTxId());

		eventManager.sendRemoteEvent(reply);

	}

	private boolean checkNeedFixUpdate(ExecType execType,
			Map<String, Object> changes) {
		if (!(execType.equals(ExecType.RESTATED) || execType
				.equals(ExecType.REPLACE))) {
			return true;
		}

		if (changes == null) {
			return false;
		}

		Collection<String> col = fixToOrderMap.values();
		ArrayList<String> list = new ArrayList<String>(col);
		// add fields that can be changed and may relate to FIX and may trigger
		// an update
		list.add(OrderField.QUANTITY.value());
		list.add(OrderField.PRICE.value());
		list.add(OrderField.CUMQTY.value());
		list.add(OrderField.AVGPX.value());
		list.add(OrderField.LAST_SHARES.value());
		list.add(OrderField.LAST_PX.value());

		for (String str : list) {
			if (changes.containsKey(str)) {
				return true;
			}
		}
		return false;
	}

	public void processUpdateParentOrderEvent(UpdateParentOrderEvent event)
			throws Exception {
		log.debug("Received parent order update: " + event.getExecType() + ", "
				+ event.getParent());
		ParentOrder newParent = event.getParent();
		boolean needFixUpdate = false;
		Map<String, Object> diff = null;
		if (!event.getExecType().equals(ExecType.NEW)
				&& !event.getExecType().equals(ExecType.PENDING_NEW)) {

			ParentOrder old = parentOrders.get(newParent.getId());
			if (old == null) {
				if (!removedOrders.contains(newParent.getId()))
					log.error("Cant find parentOrder id during udpate: "
						+ newParent.getId());
				return;
			}
			diff = old.diff(newParent);

			log.debug("UpdateParentOrderEvent difference: " + diff);

		}

		newParent.touch();
		parentOrders.put(newParent.getId(), newParent.getAccount(), newParent);

		String source = newParent.get(String.class, OrderField.SOURCE.value());
		boolean isFIX = newParent.get(false, Boolean.TYPE,
				OrderField.IS_FIX.value());
		needFixUpdate = isFIX && checkNeedFixUpdate(event.getExecType(), diff);

		eventManager.sendRemoteEvent(new ParentOrderUpdateEvent(newParent.getAccount(), null,
				event.getExecType(), event.getTxId(), newParent, event
						.getInfo()));

		if (needFixUpdate) {
			log.debug("Send internal update: " + source + ", "
					+ event.getExecType() + ", " + event.getTxId() + ", "
					+ newParent);
			eventManager.sendEvent(new ParentOrderUpdateEvent(source, null,
					event.getExecType(), event.getTxId(), newParent, event
							.getInfo()));
		}

	}

	public void processEndOfDayEvent(EndOfDayEvent event) {
        if (parentOrderSaver != null) {
        	Collection<ParentOrder> cParentOrders = parentOrders.values();
        	List<ParentOrder> lstParentOrder = new ArrayList<>();
        	if (cParentOrders instanceof List) {
        		lstParentOrder = (List<ParentOrder>)cParentOrders;
        	} else {
        		lstParentOrder = new ArrayList<>(cParentOrders);
        	}

        	parentOrderSaver.addOrderList(lstParentOrder);
        	parentOrderSaver.saveToFile();
        }

        if (childOrderSaver != null) {
        	List<ChildOrder> lstChildOrder = new ArrayList<>();

        	for (Map<String, ChildOrder> map : archiveChildOrders.values()) {
        		lstChildOrder.addAll(map.values());
            }

        	for (Map<String, ChildOrder> map : activeChildOrders.values()) {
        		lstChildOrder.addAll(map.values());
            }

        	childOrderSaver.addOrderList(lstChildOrder);
        	childOrderSaver.saveToFile();
        }

        if (cumQuantitySaver != null) {
        	Collection<ParentOrder> cParentOrders = parentOrders.values();
        	List<ParentOrder> lstParentOrder = new ArrayList<>();
        	if (cParentOrders instanceof List) {
        		lstParentOrder = (List<ParentOrder>)cParentOrders;
        	} else {
        		lstParentOrder = new ArrayList<>(cParentOrders);
        	}

        	cumQuantitySaver.addOrderList(lstParentOrder);
        	cumQuantitySaver.saveToFile();
		}
    }

	public void init() throws Exception {
		// subscribe to events
		eventProcessor.setHandler(this);
		eventProcessor.init();
		if (eventProcessor.getThread() != null) {
			eventProcessor.getThread().setName("OrderManager");
		}
	}

	public void uninit() {
		eventProcessor.uninit();
	}

	public void injectStrategies(List<DataObject> list) {
		for (DataObject obj : list) {
			if (obj instanceof ParentOrder) {
				ParentOrder parentOrder = (ParentOrder)obj;
				parentOrders.put(parentOrder.getId(), parentOrder.getAccount(), parentOrder);
			} else if (obj instanceof ChildOrder) {
				ChildOrder childOrder = (ChildOrder)obj;
				Map<String, ChildOrder> child = new HashMap<>();
				child.put(childOrder.getId(), childOrder);
				if (childOrder.getOrdStatus().isCompleted()) {
					archiveChildOrders.put(childOrder.getStrategyId(), child);
				} else {
					activeChildOrders.put(childOrder.getStrategyId(), child);
				}
			} else if (obj instanceof MultiInstrumentStrategyData) {
				MultiInstrumentStrategyData data = (MultiInstrumentStrategyData)obj;
				strategyData.put(data.getId(), data.getAccount(), data);
			} else if (obj instanceof Instrument) {
				Instrument instrument = (Instrument)obj;
				instruments.put(instrument.getId(), instrument.getAccount(), instrument);
			} else {
				log.error("Unknow recovery object: " + obj.getClass());
			}
		}
	}

	public void injectExecutions(List<Execution> list) {
		for (Execution execution : list) {
			addExecution(execution);
		}

	}

	// getters and setters
	public boolean isPublishChildOrder() {
		return publishChildOrder;
	}

	public void setPublishChildOrder(boolean publishChildOrder) {
		this.publishChildOrder = publishChildOrder;
	}

	public long getOrderLimit() {
		return orderLimit;
	}

	public void setOrderLimit(long orderLimit) {
		this.orderLimit = orderLimit;
	}

}
