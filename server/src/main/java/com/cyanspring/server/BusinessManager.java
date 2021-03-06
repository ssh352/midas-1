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
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import webcurve.util.PriceUtils;

import com.cyanspring.common.Clock;
import com.cyanspring.common.Default;
import com.cyanspring.common.account.Account;
import com.cyanspring.common.account.AccountException;
import com.cyanspring.common.account.AccountKeeper;
import com.cyanspring.common.account.AccountSetting;
import com.cyanspring.common.account.OpenPosition;
import com.cyanspring.common.account.OrderReason;
import com.cyanspring.common.business.FieldDef;
import com.cyanspring.common.business.GlobalSetting;
import com.cyanspring.common.business.MultiInstrumentStrategyDisplayConfig;
import com.cyanspring.common.business.OrderException;
import com.cyanspring.common.business.OrderField;
import com.cyanspring.common.business.ParentOrder;
import com.cyanspring.common.business.util.DataConvertException;
import com.cyanspring.common.business.util.GenericDataConverter;
import com.cyanspring.common.data.DataObject;
import com.cyanspring.common.downstream.DownStreamException;
import com.cyanspring.common.event.AsyncEventMultiProcessor;
import com.cyanspring.common.event.AsyncEventProcessor;
import com.cyanspring.common.event.AsyncTimerEvent;
import com.cyanspring.common.event.IAsyncEventManager;
import com.cyanspring.common.event.IRemoteEventManager;
import com.cyanspring.common.event.ScheduleManager;
import com.cyanspring.common.event.account.InternalResetAccountRequestEvent;
import com.cyanspring.common.event.account.ResetAccountReplyEvent;
import com.cyanspring.common.event.account.ResetAccountReplyType;
import com.cyanspring.common.event.account.ResetAccountRequestEvent;
import com.cyanspring.common.event.info.GlobalSettingReplyEvent;
import com.cyanspring.common.event.info.GlobalSettingRequestEvent;
import com.cyanspring.common.event.livetrading.LiveTradingEndEvent;
import com.cyanspring.common.event.marketsession.IndexSessionEvent;
import com.cyanspring.common.event.marketsession.MarketSessionEvent;
import com.cyanspring.common.event.order.AmendParentOrderEvent;
import com.cyanspring.common.event.order.AmendParentOrderReplyEvent;
import com.cyanspring.common.event.order.AmendStrategyOrderEvent;
import com.cyanspring.common.event.order.CancelParentOrderEvent;
import com.cyanspring.common.event.order.CancelParentOrderReplyEvent;
import com.cyanspring.common.event.order.CancelPendingOrderEvent;
import com.cyanspring.common.event.order.CancelStrategyOrderEvent;
import com.cyanspring.common.event.order.ClosePositionReplyEvent;
import com.cyanspring.common.event.order.ClosePositionRequestEvent;
import com.cyanspring.common.event.order.EnterParentOrderEvent;
import com.cyanspring.common.event.order.EnterParentOrderReplyEvent;
import com.cyanspring.common.event.order.InitClientEvent;
import com.cyanspring.common.event.order.InitClientRequestEvent;
import com.cyanspring.common.event.order.UpdateParentOrderEvent;
import com.cyanspring.common.event.statistic.TickTableReplyEvent;
import com.cyanspring.common.event.statistic.TickTableRequestEvent;
import com.cyanspring.common.event.strategy.AddStrategyEvent;
import com.cyanspring.common.event.strategy.NewMultiInstrumentStrategyEvent;
import com.cyanspring.common.event.strategy.NewMultiInstrumentStrategyReplyEvent;
import com.cyanspring.common.event.strategy.NewSingleInstrumentStrategyEvent;
import com.cyanspring.common.event.strategy.NewSingleInstrumentStrategyReplyEvent;
import com.cyanspring.common.livetrading.TradingUtil;
import com.cyanspring.common.marketsession.DefaultStartEndTime;
import com.cyanspring.common.marketsession.IndexSessionType;
import com.cyanspring.common.marketsession.MarketSessionData;
import com.cyanspring.common.marketsession.MarketSessionType;
import com.cyanspring.common.marketsession.WeekDay;
import com.cyanspring.common.message.ErrorMessage;
import com.cyanspring.common.message.MessageLookup;
import com.cyanspring.common.order.RiskOrderController;
import com.cyanspring.common.position.PositionKeeper;
import com.cyanspring.common.staticdata.AbstractTickTable;
import com.cyanspring.common.staticdata.IRefDataManager;
import com.cyanspring.common.staticdata.RefData;
import com.cyanspring.common.staticdata.TickTableManager;
import com.cyanspring.common.strategy.GlobalStrategySettings;
import com.cyanspring.common.strategy.IStrategy;
import com.cyanspring.common.strategy.IStrategyContainer;
import com.cyanspring.common.strategy.IStrategyFactory;
import com.cyanspring.common.strategy.StrategyException;
import com.cyanspring.common.type.ExecType;
import com.cyanspring.common.type.OrdStatus;
import com.cyanspring.common.type.OrderSide;
import com.cyanspring.common.type.OrderType;
import com.cyanspring.common.type.StrategyState;
import com.cyanspring.common.util.DualKeyMap;
import com.cyanspring.common.util.IdGenerator;
import com.cyanspring.common.util.TimeUtil;
import com.cyanspring.common.validation.ITransactionValidator;
import com.cyanspring.common.validation.OrderValidationException;
import com.cyanspring.common.validation.TransactionValidationException;
import com.cyanspring.server.account.CoinManager;
import com.cyanspring.server.order.MultiOrderCancelTracker;
import com.cyanspring.server.validation.ParentOrderDefaultValueFiller;
import com.cyanspring.server.validation.ParentOrderPreCheck;
import com.cyanspring.server.validation.ParentOrderValidator;
import com.cyanspring.strategy.multiinstrument.MultiInstrumentStrategy;
import com.cyanspring.strategy.singleinstrument.SingleInstrumentStrategy;
import com.cyanspring.strategy.singleorder.SingleOrderStrategy;
import com.mchange.v2.codegen.bean.BeangenUtils;

public class BusinessManager implements ApplicationContextAware {
	private static final Logger log = LoggerFactory
			.getLogger(BusinessManager.class);

	private ApplicationContext applicationContext;

	@Autowired
	private IRemoteEventManager eventManager;

	@Autowired
	IStrategyFactory strategyFactory;

	@Autowired
	private GenericDataConverter dataConverter;

	@Autowired
	private ParentOrderValidator parentOrderValidator;

	@Autowired
	ParentOrderPreCheck parentOrderPreCheck;

	@Autowired
	private ParentOrderDefaultValueFiller parentOrderDefaultValueFiller;

	@Autowired
	IRefDataManager refDataManager;

	@Autowired
	TickTableManager tickTableManager;

	@Autowired
	private DefaultStartEndTime defaultStartEndTime;

	@Autowired
	GlobalStrategySettings globalStrategySettings;

	@Autowired
	AccountKeeper accountKeeper;

	@Autowired
	PositionKeeper positionKeeper;

	@Autowired(required = false)
	RiskOrderController riskOrderController;

	@Autowired(required = false)
	CoinManager coinManager;

	@Autowired
	ITransactionValidator transactionValidator;

	ScheduleManager scheduleManager = new ScheduleManager();

	private int noOfContainers = 20;
	private ArrayList<IStrategyContainer> containers = new ArrayList<IStrategyContainer>();
	private DualKeyMap<String, String, ParentOrder> orders = new DualKeyMap<String, String, ParentOrder>();
	private boolean autoStartStrategy;
	private AsyncTimerEvent closePositionCheckEvent = new AsyncTimerEvent();
	private long closePositionCheckInterval = 10000;
	private Map<String, MultiOrderCancelTracker> cancelTrackers = new HashMap<String, MultiOrderCancelTracker>();
	private boolean cancelAllOrdersAtClose = false;
	private boolean closeAllPositionsAtClose = false;

	private AsyncTimerEvent cancelPendingOrderEvent = new AsyncTimerEvent();
	private WeekDay weekDay;
	private String cancelPendingOrderTime;

	public boolean isAutoStartStrategy() {
		return autoStartStrategy;
	}

	public void setAutoStartStrategy(boolean autoStartStrategy) {
		this.autoStartStrategy = autoStartStrategy;
	}

	public BusinessManager() {
	}

	private AsyncEventProcessor eventProcessor = new AsyncEventProcessor() {

		@Override
		public void subscribeToEvents() {
			subscribeToEvent(InitClientRequestEvent.class, null);
			subscribeToEvent(AmendParentOrderEvent.class, null);
			subscribeToEvent(CancelParentOrderEvent.class, null);
			subscribeToEvent(ResetAccountRequestEvent.class, null);
			subscribeToEvent(MarketSessionEvent.class, null);
			subscribeToEvent(LiveTradingEndEvent.class, null);
			subscribeToEvent(CancelPendingOrderEvent.class, null);
			subscribeToEvent(IndexSessionEvent.class, null);
			subscribeToEvent(TickTableRequestEvent.class, null);
			subscribeToEvent(GlobalSettingRequestEvent.class, null);
		}

		@Override
		public IAsyncEventManager getEventManager() {
			return eventManager;
		}

	};

	private AsyncEventMultiProcessor eventMultiProcessor = new AsyncEventMultiProcessor() {

		@Override
		public void subscribeToEvents() {
			subscribeToEvent(EnterParentOrderEvent.class, null);
			subscribeToEvent(NewSingleInstrumentStrategyEvent.class, null);
			subscribeToEvent(NewMultiInstrumentStrategyEvent.class, null);
			subscribeToEvent(ClosePositionRequestEvent.class, null);
		}

		@Override
		public IAsyncEventManager getEventManager() {
			return eventManager;
		}

	};

	public void processEnterParentOrderEvent(EnterParentOrderEvent event)
			throws Exception {
		Map<String, Object> fields = event.getFields();
		log.info("Received EnterParentOrderEvent: " + fields);

		boolean failed = false;
		String message = "";
		ParentOrder order = null;
		String user = (String) fields.get(OrderField.USER.value());
		String account = (String) fields.get(OrderField.ACCOUNT.value());

		try {
			transactionValidator.checkEnterOrder(event, account);

			String strategyName = (String) fields.get(OrderField.STRATEGY
					.value());
			if (null == strategyName) {
				throw new Exception("Strategy Field is missing");
			}

			HashMap<String, Object> map = null;
			// pre-fill any fields that are specified
			parentOrderDefaultValueFiller.fillDefaults(fields);

			// check parameters are presented and types are well defined
			parentOrderPreCheck.validate(fields, null);

			// convert string presentation into object
			map = convertOrderFields(fields, strategyName);

			// validate all parameters have valid values
			parentOrderValidator.validate(map, null);

			// stick in the txId for future updates
			if (null != event.getTxId()) {
				map.put(OrderField.CLORDERID.value(), event.getTxId());
			}

			// stick in source for FIX orders
			if (null != event.getKey()) {
				map.put(OrderField.SOURCE.value(), event.getKey());
			}

			map.put(OrderField.IS_FIX.value(), event.isFix());

			order = new ParentOrder(map);
			order.setSender(event.getSender());

			if (orders.containsKey(order.getId())) {
				throw new OrderValidationException(
						"Enter order: this order id already exists: "
								+ order.getId(), ErrorMessage.ORDER_ID_EXIST);
			}

			checkClosePositionPending(order.getAccount(), order.getSymbol());

			// create the strategy
			IStrategy strategy = strategyFactory.createStrategy(
					order.getStrategy(), new Object[] { refDataManager,
							tickTableManager, order });

			// add order to local map
			orders.put(order.getId(), order.getAccount(), order);

			// ack order
			EnterParentOrderReplyEvent reply = new EnterParentOrderReplyEvent(
					event.getKey(), event.getSender(), true, "",
					event.getTxId(), order, user, account);

			eventManager.sendLocalOrRemoteEvent(reply);

			// send to order manager
			UpdateParentOrderEvent updateEvent = new UpdateParentOrderEvent(
					order.getId(), ExecType.NEW, event.getTxId(), order, null);
			eventManager.sendEvent(updateEvent);

			IStrategyContainer container = getLeastLoadContainer();

			String note = order.get(String.class, OrderField.NOTE.value());
			if (null != note) {
				log.debug("strategy " + strategy.getId() + " : " + note);
			}

			log.debug("strategy " + strategy.getId()
					+ " assigned to container " + container.getId());
			AddStrategyEvent addStrategyEvent = new AddStrategyEvent(
					container.getId(), strategy, true);
			eventManager.sendEvent(addStrategyEvent);

		} catch (OrderValidationException e) {
			failed = true;
			// message = e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());
			log.warn(message);
			// log.warn(e.getMessage(), e);
		} catch (OrderException e) {
			failed = true;
			// message = e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());
			log.warn(message);
			// log.warn(e.getMessage(), e);
		} catch (DataConvertException e) {
			failed = true;
			// message = "DataConvertException: " + e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					"DataConvertException: " + e.getMessage());
			log.warn(message);
			// log.warn(e.getMessage(), e);
		} catch (DownStreamException e) {
			failed = true;
			// message = e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());
			log.warn(message);
			// log.warn(e.getMessage(), e);
		} catch (StrategyException e) {
			failed = true;
			// message = e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());
			log.warn(message);
			// log.warn(e.getMessage(), e);
		} catch (TransactionValidationException e) {
			failed = true;
			// message = e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());
			log.warn(message);
			// log.warn(e.getMessage(), e);
		} catch (Exception e) {
			failed = true;
			log.error(e.getMessage(), e);
			// e.printStackTrace();
			// message = "Enter order failed, please check server log";
			message = MessageLookup.buildEventMessage(
					ErrorMessage.ENTER_ORDER_ERROR,
					"Enter order failed, please check server log");
			log.warn(e.getMessage(), e);
		}

		if (failed) {
			log.debug("Enter order failed: " + message + ", " + user + ", "
					+ account + ", " + event.getFields());
			EnterParentOrderReplyEvent replyEvent = new EnterParentOrderReplyEvent(
					event.getKey(), event.getSender(), false, message,
					event.getTxId(), order, user, account);
			eventManager.sendLocalOrRemoteEvent(replyEvent);
		}

	}

	public void processAmendParentOrderEvent(AmendParentOrderEvent event)
			throws Exception {
		log.debug("processAmendParentOrderEvent received: " + event.getId()
				+ ", " + event.getTxId() + ", " + event.getFields());
		Map<String, Object> fields = event.getFields();

		boolean failed = false;
		String message = "";
		ParentOrder order = null;
		try {
			// check whether order is there
			String id = event.getId();
			order = orders.get(id);
			if (null == order) {
				throw new OrderException("Cant find this order id: " + id,
						ErrorMessage.ORDER_ID_NOT_FOUND);
			}

			transactionValidator.checkAmendOrder(event, order.getAccount());

			checkClosePositionPending(order.getAccount(), order.getSymbol());

			String strategyName = order.getStrategy();
			// convert string presentation into object
			HashMap<String, Object> map = convertOrderFields(fields,
					strategyName);

			Map<String, Object> ofields = order.getFields();
			Map<String, Object> changes = new HashMap<String, Object>();

			List<String> amendableFields = strategyFactory
					.getStrategyAmendableFields(order.getStrategy());
			for (Entry<String, Object> entry : map.entrySet()) {
				if (!amendableFields.contains(entry.getKey())) {
					log.debug("Field change not in amendable fields, ignored: "
							+ entry.getKey() + ", " + entry.getValue());
					continue;
				}
				Object oldValue = ofields.get(entry.getKey());
				if ((null != entry.getValue() && oldValue == null)
						|| (null == entry.getValue() && oldValue != null)) {
					changes.put(entry.getKey(), entry.getValue());
				} else if (null != entry.getValue() && oldValue != null) {
					boolean add = false;
					try {
						add = !oldValue.equals(entry.getValue());
					} catch (Exception e) {
						log.error(e.getMessage(), e);
						throw new OrderValidationException("Field "
								+ entry.getKey() + " comparison exception: "
								+ e.getMessage(),
								ErrorMessage.ORDER_VALIDATION_ERROR);
					}
					if (add) {
						changes.put(entry.getKey(), entry.getValue());
					}
				}

			}
			log.debug("Fields being changed: " + changes);

			// validate all parameters have valid values
			parentOrderValidator.validate(changes, order);

			// order is already completed
			if (order.getOrdStatus().isCompleted()) {
				message = MessageLookup.buildEventMessage(
						ErrorMessage.ORDER_ALREADY_COMPLETED,
						"Order already completed");

				AmendParentOrderReplyEvent replyEvent = new AmendParentOrderReplyEvent(
						event.getKey(), event.getSender(), false, message,
						event.getTxId(), order);
				eventManager.sendLocalOrRemoteEvent(replyEvent);
				return;
			}

			// Order already terminated, no longer managed by strategy
			if (order.getState().equals(StrategyState.Terminated)) {
				order.update(changes);
				order.touch();
				message = MessageLookup.buildEventMessage(
						ErrorMessage.ORDER_ALREADY_TERMINATED,
						"Order already terminated");

				AmendParentOrderReplyEvent replyEvent = new AmendParentOrderReplyEvent(
						event.getKey(), event.getSender(), false, message,
						event.getTxId(), order);
				eventManager.sendLocalOrRemoteEvent(replyEvent);

				UpdateParentOrderEvent updateEvent = new UpdateParentOrderEvent(
						order.getId(), ExecType.REPLACE, event.getTxId(),
						order, null);
				eventManager.sendEvent(updateEvent);
				return;
			}

			AmendStrategyOrderEvent amendEvent = new AmendStrategyOrderEvent(
					id, event.getSender(), event.getTxId(), event.getKey(),
					changes);
			eventManager.sendEvent(amendEvent);

		} catch (OrderValidationException e) {
			failed = true;
			// message = e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());

		} catch (OrderException e) {
			failed = true;
			// message = e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());

		} catch (DataConvertException e) {
			failed = true;
			// message = "DataConvertException: " + e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					"DataConvertException: " + e.getMessage());

		} catch (TransactionValidationException e) {
			failed = true;
			// message = e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());
			log.warn(message);
			// log.warn(e.getMessage(), e);
		} catch (Exception e) {
			failed = true;
			log.error(e.getMessage(), e);
			// e.printStackTrace();
			// message = "Amend order failed, please check server log";
			message = MessageLookup.buildEventMessage(
					ErrorMessage.AMEND_ORDER_ERROR,
					"Amend order failed, please check server log");

		}

		if (failed) {
			log.debug("Amend order failed: " + message);
			AmendParentOrderReplyEvent replyEvent = new AmendParentOrderReplyEvent(
					event.getKey(), event.getSender(), false, message,
					event.getTxId(), order);
			eventManager.sendLocalOrRemoteEvent(replyEvent);

		}
	}

	public void processCancelParentOrderEvent(CancelParentOrderEvent event)
			throws Exception {
		log.debug("processCancelParentOrderEvent received: " + event.getTxId()
					+ ", " + event.getOrderId());

		ParentOrder order = orders.get(event.getOrderId());
		String message = "";
		boolean failed = false;
		try {
			if (null == order) {
				throw new OrderException("Cant find this order id: " + event.getOrderId(),
						ErrorMessage.ORDER_ID_NOT_FOUND);
			}

			transactionValidator.checkCancelOrder(event, order.getAccount());

			if (order.getOrdStatus().isCompleted()) {
				throw new OrderException("Order already completed: " + order.getId(),
						ErrorMessage.ORDER_ALREADY_COMPLETED);
			}

		} catch (OrderException e) {
			failed = true;
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());

		} catch (TransactionValidationException e) {
			failed = true;
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());
		} catch (Exception e) {
			failed = true;
			log.error(e.getMessage(), e);
			message = MessageLookup.buildEventMessage(
					ErrorMessage.CANCEL_ORDER_ERROR,
					"Cancel order failed, please check server log");

		}

		if (failed) {
			CancelParentOrderReplyEvent reply = new CancelParentOrderReplyEvent(
					event.getKey(), event.getSender(), false, message,
					event.getTxId(), order);
			eventManager.sendLocalOrRemoteEvent(reply);
			return;
		}

		if (order.getState().equals(StrategyState.Terminated)) {
			order.setOrdStatus(OrdStatus.CANCELED);
			CancelParentOrderReplyEvent reply = new CancelParentOrderReplyEvent(
					event.getKey(), event.getSender(), true, null,
					event.getTxId(), order);
			eventManager.sendLocalOrRemoteEvent(reply);

			UpdateParentOrderEvent update = new UpdateParentOrderEvent(
					order.getId(), ExecType.CANCELED, event.getTxId(),
					order.clone(), null);
			eventManager.sendEvent(update);
			return;
		}

		CancelStrategyOrderEvent cancel = new CancelStrategyOrderEvent(
				order.getId(), event.getSender(), event.getTxId(),
				event.getKey(), null, event.isForce());
		eventManager.sendEvent(cancel);
	}

	private void checkClosePositionPending(String account, String symbol)
			throws OrderException {
		if (positionKeeper.checkAccountPositionLock(account, symbol)) {
			throw new OrderException("Account " + account
					+ " has pending close position action on symbol " + symbol,
					ErrorMessage.POSITION_PENDING);
		}
	}

	public void processClosePositionRequestEvent(ClosePositionRequestEvent event) {
		boolean ok = true;
		String message = null;
		ErrorMessage clientMessage = null;
		String eventAccount = event.getAccount();
		String eventSymbol = event.getSymbol();
		String eventSender = event.getSender();
		String eventKey = event.getKey();
		String eventTxId = event.getTxId();
		OrderReason eventReason = event.getReason();
		double eventQty = event.getQty();

		try {
			transactionValidator.checkClosePosition(event, eventAccount);

			Account account = accountKeeper.getAccount(eventAccount);
			if (null == account) {
				clientMessage = ErrorMessage.ACCOUNT_NOT_EXIST;
				throw new Exception("Cant find this account: " + eventAccount);
			}


			RefData refData = refDataManager.getRefData(eventSymbol);
			if (null == refData) {
				clientMessage = ErrorMessage.SYMBOL_NOT_FOUND;
				throw new Exception("Can't find this symbol: " + eventSymbol);
			}
			checkClosePositionPending(eventAccount, eventSymbol);

			OpenPosition position = positionKeeper.getOverallPosition(account,
					eventSymbol);
			double qty = Math.abs(position.getAvailableQty());

			if (PriceUtils.isZero(qty)) {
				clientMessage = ErrorMessage.POSITION_NOT_FOUND;
				throw new Exception(
						"Account " + eventAccount + " doesn't have a position at symbol " + eventSymbol);

			}

			if (!PriceUtils.isZero(eventQty)) {
				qty = Math.min(qty, eventQty);
			}

            processClosePosition(eventSender, eventTxId, eventKey, eventReason,
                    account, position.getSymbol(), position.getQty() > 0 ? OrderSide.Sell: OrderSide.Buy, qty);

		} catch (AccountException ae) {
			ok = false;
			message = MessageLookup.buildEventMessage(clientMessage,
					ae.getMessage());
			log.warn(ae.getMessage());
		} catch (TransactionValidationException e) {
			ok = false;
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					e.getMessage());
		} catch (Exception e) {
			ok = false;
			// message = e.getMessage();
			message = MessageLookup.buildEventMessage(clientMessage,
					e.getMessage());
		}

		try {
			eventManager.sendLocalOrRemoteEvent(new ClosePositionReplyEvent(
					eventKey, eventSender, eventAccount,
					eventSymbol, eventTxId, ok, message));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

    public void processClosePosition(String sender, String txID, String key, OrderReason reason,
                                     Account account, String symbol, OrderSide side, double qty) throws Exception {
        ParentOrder order = new ParentOrder(symbol, side,
                qty, 0.0, OrderType.Market);
        order.put(OrderField.STRATEGY.value(), "SDMA");
        order.setUser(account.getUserId());
        order.setAccount(account.getId());
        order.setSender(sender);
        // stick in the txId for future updates
        order.put(OrderField.CLORDERID.value(), txID);
        // stick in source for FIX orders
        order.put(OrderField.SOURCE.value(), key);
        order.setReason(reason);

        // add order to local map
        positionKeeper.lockAccountPosition(order);
        orders.put(order.getId(), order.getAccount(), order);

        // send to order manager
        UpdateParentOrderEvent updateEvent = new UpdateParentOrderEvent(
                order.getId(), ExecType.NEW, txID, order, null);
        eventManager.sendEvent(updateEvent);

        // create the strategy
        IStrategy strategy = strategyFactory.createStrategy(
                order.getStrategy(), new Object[] { refDataManager,
                        tickTableManager, order });
        IStrategyContainer container = getLeastLoadContainer();

        log.debug("Close position order " + strategy.getId()
                + " assigned to container " + container.getId());
        AddStrategyEvent addStrategyEvent = new AddStrategyEvent(
                container.getId(), strategy, true);
        eventManager.sendEvent(addStrategyEvent);
    }

    public void processUpdateParentOrderEvent(UpdateParentOrderEvent event) {
		ParentOrder order = event.getParent();
		log.info("Received UpdateParentOrderEvent: " + order);
		String account = order.getAccount();
		MultiOrderCancelTracker tracker = cancelTrackers.get(account);
		if (null != tracker) {
			if (tracker.checkParentOrderUpdate(event)) {
				log.info("Cancel tracker completed");
				cancelTrackers.remove(account);
				orders.removeMap(account);
				eventManager.sendEvent(new InternalResetAccountRequestEvent(
						tracker.getEvent()));
			}
		} else {
			log.warn("Receive order update but no matching tracker: " + order);
		}
	}

	public void processResetAccountRequestEvent(ResetAccountRequestEvent event) {
		String account = event.getAccount();
		log.info("Received ResetAccountRequestEvent: " + account);
		if (!accountKeeper.accountExists(account)) {
			try {
				// int code = 406;
				// String msg = ErrorLookup.lookup(code);

				String msg = MessageLookup
						.buildEventMessage(ErrorMessage.ACCOUNT_NOT_EXIST,
								"account doesn't exist");
				eventManager
						.sendRemoteEvent(new ResetAccountReplyEvent(event
								.getKey(), event.getSender(), event
								.getAccount(), event.getTxId(), event
								.getUserId(), event.getMarket(), event
								.getCoinId(), ResetAccountReplyType.LTSCORE,
								false, msg));
				return;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		Map<String, ParentOrder> map = orders.getMap(account);

		List<ParentOrder> list = new LinkedList<ParentOrder>();
		for (ParentOrder order : map.values()) {
			if (!order.getOrdStatus().isCompleted()) {
				list.add(order);
			}
		}

		if (null != list && list.size() > 0) {
			MultiOrderCancelTracker tracker = new MultiOrderCancelTracker(
					eventManager, eventProcessor, event);
			for (ParentOrder order : list) {
				tracker.add(order);
			}
			cancelTrackers.put(account, tracker);
		} else {
			eventManager.sendEvent(new InternalResetAccountRequestEvent(event));
		}
	}

	public void processAsyncTimerEvent(AsyncTimerEvent event) {
		if (event == this.closePositionCheckEvent) {
			for (ParentOrder order : positionKeeper.getTimeoutLocks()) {
				if (!order.getOrdStatus().isCompleted()) {
					log.debug("Close position action timeout, trying to cancel: "
							+ order);
					String source = order.get(String.class,
							OrderField.SOURCE.value());
					String txId = order.get(String.class,
							OrderField.CLORDERID.value());
					CancelStrategyOrderEvent cancel = new CancelStrategyOrderEvent(
							order.getId(), order.getSender(), txId, source,
							null, false);
					eventManager.sendEvent(cancel);
				} else {
					log.debug("Close position action completed, remove stale record: "
							+ order);
				}
				positionKeeper.unlockAccountPosition(order.getId());
			}
		} else if (event == this.cancelPendingOrderEvent) {
			eventManager.sendEvent(new CancelPendingOrderEvent(null, null));
		}
	}

	private HashMap<String, Object> convertOrderFields(
			Map<String, Object> fields, String strategyName)
			throws DataConvertException, StrategyException {
		HashMap<String, Object> map = new HashMap<String, Object>();

		Map<String, FieldDef> fieldDefs = strategyFactory
				.getStrategyFieldDef(strategyName);
		if (null == fieldDefs) {
			throw new DataConvertException(
					"Cant find field definition for strategy: " + strategyName,
					ErrorMessage.FIELD_DEFINITION_NOT_FOUND);
		}
		for (Entry<String, Object> entry : fields.entrySet()) {
			Object obj;
			if (entry.getValue() instanceof String) {
				FieldDef fieldDef = fieldDefs.get(entry.getKey());
				if (fieldDef == null) { // not found in input fields
					log.debug("Ignore field not defined in input fields: "
							+ entry.getKey());
					continue;
				}
				obj = dataConverter.fromString(fieldDef.getType(),
						entry.getKey(), (String) entry.getValue());
			} else { // upstream already converted it from string type
				obj = entry.getValue();
			}
			map.put(entry.getKey(), obj);
		}

		return map;
	}

	public void processNewMultiInstrumentStrategyEvent(
			NewMultiInstrumentStrategyEvent event) {
		// create the strategy
		boolean failed = false;
		String message = "";
		String strategyName = "";
		try {
			strategyName = (String) event.getStrategy().get(
					OrderField.STRATEGY.value());
			if (null == strategyName) {
				throw new StrategyException(
						"Strategy field not present in NewMultiInstrumentStrategyEvent");
			}
			IStrategy strategy = strategyFactory.createStrategy(
					strategyName,
					new Object[] { refDataManager, tickTableManager,
							event.getStrategy(), event.getInstruments() });
			IStrategyContainer container = getLeastLoadContainer();
			log.debug("strategy " + strategy.getId()
					+ " assigned to container " + container.getId());
			AddStrategyEvent addStrategyEvent = new AddStrategyEvent(
					container.getId(), strategy, true);
			eventManager.sendEvent(addStrategyEvent);
		} catch (Exception e) {
			message = strategyName + " " + e.getMessage();
			log.error(message, e);
			failed = true;
		}

		NewMultiInstrumentStrategyReplyEvent replyEvent = new NewMultiInstrumentStrategyReplyEvent(
				event.getKey(), event.getSender(), event.getTxId(), !failed,
				message);
		try {
			eventManager.sendLocalOrRemoteEvent(replyEvent);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}

	public void processNewSingleInstrumentStrategyEvent(
			NewSingleInstrumentStrategyEvent event) {
		// create the strategy
		boolean failed = false;
		String message = "";
		String strategyName = "";
		try {
			strategyName = (String) event.getInstrument().get(
					OrderField.STRATEGY.value());
			if (null == strategyName) {
				throw new StrategyException(
						"Strategy field not present in NewSingleInstrumentStrategyEvent",
						ErrorMessage.STRATEGY_NOT_PRESENT_IN_SINGLE_INSTRUMENT);
			}
			IStrategy strategy = strategyFactory.createStrategy(
					strategyName,
					new Object[] { refDataManager, tickTableManager,
							event.getInstrument() });
			IStrategyContainer container = getLeastLoadContainer();
			log.debug("strategy " + strategy.getId()
					+ " assigned to container " + container.getId());
			AddStrategyEvent addStrategyEvent = new AddStrategyEvent(
					container.getId(), strategy, true);
			eventManager.sendEvent(addStrategyEvent);
		} catch (StrategyException e) {
			// message = strategyName + " " + e.getMessage();
			message = MessageLookup.buildEventMessage(e.getClientMessage(),
					strategyName + " " + e.getMessage());
			log.error(message, e);
			failed = true;
		} catch (Exception e) {
			// message = strategyName + " " + e.getMessage();
			message = MessageLookup.buildEventMessage(
					ErrorMessage.STRATEGY_ERROR,
					strategyName + " " + e.getMessage());
			log.error(message, e);
			failed = true;
		}

		NewSingleInstrumentStrategyReplyEvent replyEvent = new NewSingleInstrumentStrategyReplyEvent(
				event.getKey(), event.getSender(), event.getTxId(), !failed,
				message);
		try {
			eventManager.sendLocalOrRemoteEvent(replyEvent);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}

	}

	public void processLiveTradingEndEvent(LiveTradingEndEvent event) {
		// close all position and orders accounts that has live trading
		try {
			if (!event.isNeedClearOrderPosition()) {
				return;
			}

			List<Account> accounts = accountKeeper.getAllAccounts();
			for (Account account : accounts) {
				AccountSetting accountSetting = accountKeeper
						.getAccountSetting(account.getId());

				if ( null != coinManager && !coinManager.canCheckDayTradingMode(account.getId())) {
					continue;
				}

				if (accountSetting.isLiveTrading()) {

					log.info("LiveTradingEndEvent:close position account:"
							+ account.getId());
					TradingUtil.closeAllPositoinAndOrder(account,
							positionKeeper, eventManager, false,
							OrderReason.DayTradingMode, riskOrderController);
				}
			}

		} catch (AccountException e) {
			log.error(e.getMessage(), e);
		}

	}

	private IStrategyContainer getLeastLoadContainer() {
		IStrategyContainer result = null;
		for (IStrategyContainer container : containers) {
			if (result == null
					|| container.getStrategyCount() < result.getStrategyCount()) {
				result = container;
			}
		}

		return result;
	}

	public int getNoOfContainers() {
		return noOfContainers;
	}

	public void setNoOfContainers(int noOfContainers) {
		this.noOfContainers = noOfContainers;
	}

	public void processInitClientRequestEvent(InitClientRequestEvent request)
			throws Exception {
		log.debug("Received InitClientRequestEvent from client: "
				+ request.getSender());
		InitClientEvent event = new InitClientEvent(null, request.getSender(),
				getSingleOrderDisplayFields(), getSingleOrderFieldDefs(),
				getSingleInstrumentDisplayFields(),
				getSingleInstrumentFieldDefs(),
				getMultiInstrumentDisplayFields(),
				getMultiInstrumentFieldDefs(), defaultStartEndTime);

		eventManager.sendRemoteEvent(event);
	}

	public List<String> getSingleOrderDisplayFields() throws StrategyException {
		List<String> result = globalStrategySettings
				.getSingleOrderCommonDisplayFields();
		if (null == result) {
			throw new StrategyException("SingleOrderDisplayFields is null");
		}
		return result;
	}

	public List<String> getSingleInstrumentDisplayFields()
			throws StrategyException {
		List<String> result = globalStrategySettings
				.getSingleInstrumentCommonDisplayFields();
		if (null == result) {
			throw new StrategyException("SingleInstrumentDisplayFields is null");
		}
		return result;
	}

	public List<String> getMultiInstrumentDisplayFields()
			throws StrategyException {
		List<String> result = globalStrategySettings
				.getMultiInstrumentCommonDisplayFields();
		if (null == result) {
			throw new StrategyException("MultiInstrumentDisplayFields is null");
		}
		return result;
	}

	private Map<String, Map<String, FieldDef>> getSingleOrderFieldDefs()
			throws StrategyException {
		List<IStrategy> templates = strategyFactory.getAllStrategyTemplates();
		Map<String, Map<String, FieldDef>> result = new HashMap<String, Map<String, FieldDef>>();
		for (IStrategy template : templates) {
			if (template instanceof SingleOrderStrategy) {
				Map<String, FieldDef> fieldDefs = template
						.getCombinedFieldDefs();
				result.put(template.getStrategyName(), fieldDefs);
			}
		}
		return result;
	}

	private Map<String, Map<String, FieldDef>> getSingleInstrumentFieldDefs()
			throws StrategyException {
		List<IStrategy> templates = strategyFactory.getAllStrategyTemplates();
		Map<String, Map<String, FieldDef>> result = new HashMap<String, Map<String, FieldDef>>();
		for (IStrategy template : templates) {
			if (template instanceof SingleInstrumentStrategy) {
				Map<String, FieldDef> fieldDefs = template
						.getCombinedFieldDefs();
				result.put(template.getStrategyName(), fieldDefs);
			}
		}
		return result;
	}

	synchronized public Map<String, MultiInstrumentStrategyDisplayConfig> getMultiInstrumentFieldDefs()
			throws StrategyException {
		List<IStrategy> templates = strategyFactory.getAllStrategyTemplates();
		Map<String, MultiInstrumentStrategyDisplayConfig> result = new HashMap<String, MultiInstrumentStrategyDisplayConfig>();
		for (IStrategy template : templates) {
			if (template instanceof MultiInstrumentStrategy) {
				MultiInstrumentStrategy ms = (MultiInstrumentStrategy) template;
				Map<String, FieldDef> fieldDefs = ms.getCombinedFieldDefs();
				List<String> instrumentDisplayFields = ms
						.getInstrumentDisplayFields();
				Map<String, FieldDef> instrumentLevelFieldDefs = ms
						.getCombinedInstrumentFieldDefs();
				MultiInstrumentStrategyDisplayConfig config = new MultiInstrumentStrategyDisplayConfig(
						ms.getStrategyName(), fieldDefs,
						instrumentDisplayFields, instrumentLevelFieldDefs);
				result.put(ms.getStrategyName(), config);
			}
		}
		return result;
	}

	public void processMarketSessionEvent(MarketSessionEvent event) {
		log.info("Received MarketSessionEvent: " + event);
		if (event.getSession().equals(MarketSessionType.PRECLOSE)) {
			if (this.cancelAllOrdersAtClose) {
				for (ParentOrder order : orders.values()) {
					if (!order.getOrdStatus().isCompleted()) {
						cancelOrder(order);
					}
				}
			}
			if (this.closeAllPositionsAtClose) {
				List<Account> accounts = accountKeeper.getAllAccounts();
				for (Account account : accounts) {
					List<OpenPosition> list = positionKeeper
							.getOverallPosition(account);
					for (OpenPosition position : list) {
						closePosition(position);
					}
				}
			}
		}
	}

	/**
	 *
	 * Based on processMarketSessionEvent
	 * Compare the "key" value of MarketSessionData in incoming IndexSessionEvent
	 * with the "key" of the orders/positions to determine if cancel/close or not
	 *
	 * @param event
	 */
	public void processIndexSessionEvent(IndexSessionEvent event) {
		log.info("Received IndexSessionEvent: " + event);

		if (event == null || !event.isOk()) {
			log.warn("Received IndexSessionEvent is " + (event == null ? "null" : "not OK"));
			return;
		}

		Map<String, MarketSessionData> sessionDataMap = event.getDataMap();

		for (Entry<String, MarketSessionData> sessionDataMapEntry : sessionDataMap.entrySet()) {
			String entryKey = sessionDataMapEntry.getKey();
			MarketSessionData entrySessionData = sessionDataMapEntry.getValue();

			if (entrySessionData.getSessionType().equals(MarketSessionType.PRECLOSE)) {
				if (this.cancelAllOrdersAtClose) {
					for (ParentOrder order : orders.values()) {
						String orderKey = order.getSymbol();
						RefData orderRefData = refDataManager.getRefData(orderKey);
						String orderIdxSessionType = orderRefData.getIndexSessionType();
						if (orderIdxSessionType.equals(IndexSessionType.SETTLEMENT.toString())) {
							orderKey = orderRefData.getSymbol();
						} else if (orderIdxSessionType.equals(IndexSessionType.SPOT.toString())) {
							orderKey = orderRefData.getCategory();
						} else if (orderIdxSessionType.equals(IndexSessionType.EXCHANGE.toString())) {
							orderKey = orderRefData.getExchange();
						}

						if (orderKey.equals(entryKey) && !order.getOrdStatus().isCompleted()) {
							cancelOrder(order);
						}
					}
				}

				if (this.closeAllPositionsAtClose) {
					List<Account> accounts = accountKeeper.getAllAccounts();
					for (Account account : accounts) {
						List<OpenPosition> list = positionKeeper
								.getOverallPosition(account);
						for (OpenPosition position : list) {
							String positionKey = position.getSymbol();
							RefData positionRefData = refDataManager.getRefData(positionKey);
							String positionIdxSessionType = positionRefData.getIndexSessionType();
							if (positionIdxSessionType.equals(IndexSessionType.SETTLEMENT.toString())) {
								positionKey = positionRefData.getSymbol();
							} else if (positionIdxSessionType.equals(IndexSessionType.SPOT.toString())) {
								positionKey = positionRefData.getCategory();
							} else if (positionIdxSessionType.equals(IndexSessionType.EXCHANGE.toString())) {
								positionKey = positionRefData.getExchange();
							}

							if (positionKey.equals(entryKey)) {
								closePosition(position);
							}
						}
					}
				}
			}
		}
	}

	private void cancelOrder(ParentOrder order) {
		log.debug("Market close cancel: " + order);
		String source = order.get(String.class,
				OrderField.SOURCE.value());
		String txId = order.get(String.class,
				OrderField.CLORDERID.value());
		CancelStrategyOrderEvent cancel = new CancelStrategyOrderEvent(
				order.getId(), order.getSender(), txId, source,
				null, false);
		eventManager.sendEvent(cancel);
	}

	private void closePosition(OpenPosition position) {
		log.info("Day end position close: "
				+ position.getAccount() + ", "
				+ position.getAvailableQty() + ", "
				+ position.getSymbol() + ", "
				+ position.getAcPnL());
		ClosePositionRequestEvent closeEvent = new ClosePositionRequestEvent(
				position.getAccount(), null,
				position.getAccount(), position.getSymbol(),
				0.0, OrderReason.DayEnd, IdGenerator
						.getInstance().getNextID(),true);

		eventManager.sendEvent(closeEvent);
	}

	public void processCancelPendingOrderEvent(CancelPendingOrderEvent event) {
		List<Account> accounts = accountKeeper.getAllAccounts();
		for (Account account : accounts) {
			TradingUtil.cancelAllOrders(account, positionKeeper, eventManager,
					OrderReason.CompanyStopLoss, riskOrderController);
		}

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, 7); // find the next same weekday.

		String[] times = cancelPendingOrderTime.split(":");
		int hr = Integer.parseInt(times[0]);
		int min = Integer.parseInt(times[1]);
		int sec = Integer.parseInt(times[2]);
		cal.set(Calendar.HOUR_OF_DAY, hr);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, sec);

		scheduleManager.scheduleTimerEvent(cal.getTime(), eventProcessor,
				cancelPendingOrderEvent);
		log.info("Schedule cancel pending order event at {}", cal.getTime());
	}

	public void processGlobalSettingRequestEvent(GlobalSettingRequestEvent event){
		
		GlobalSetting setting = new GlobalSetting();
		
		setting.setAccount(Default.getAccount());
		setting.setAccountCash(Default.getAccountCash());
		setting.setAccountPrefix(Default.getAccountPrefix());
		setting.setCommission(Default.getCommission());
		setting.setCommissionMin(Default.getCommissionMin());
		setting.setCompanyStopLossValue(Default.getCompanyStopLossValue());
		setting.setCreditPartial(Default.getCreditPartial());
		setting.setCurrency(Default.getCurrency());
		setting.setFreezePercent(Default.getFreezePercent());
		setting.setFreezeValue(Default.getFreezeValue());
		setting.setLiveTrading(Default.isLiveTrading());
		setting.setLiveTradingType(Default.getLiveTradingType());
		setting.setMarginCall(Default.getMarginCall());
		setting.setMarginCut(Default.getMarginCut());
		setting.setMarginTimes(Default.getMarginTimes());
		setting.setMarket(Default.getMarket());
		setting.setOrderQuantity(Default.getOrderQuantity());
		setting.setPositionStopLoss(Default.getPositionStopLoss());
		setting.setSettlementDays(Default.getSettlementDays());
		setting.setStopLossPercent(Default.getStopLossPercent());
		setting.setTerminatePecent(Default.getTerminatePecent());
		setting.setTerminateValue(Default.getTerminateValue());
		setting.setTimeZone(Default.getTimeZone());
		setting.setUser(Default.getUser());
		setting.setUserLiveTrading(Default.isUserLiveTrading());
		setting.setAppSetting(Default.getAppSetting());
			
		GlobalSettingReplyEvent reply = 
				new GlobalSettingReplyEvent(event.getKey(),event.getSender(),setting);
		try {
			eventManager.sendRemoteEvent(reply);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public void processTickTableRequestEvent(TickTableRequestEvent event) {
		try {
			String symbol = event.getSymbol();
			Map <AbstractTickTable,List<RefData>> map= tickTableManager.buildTickTableSymbolMap(symbol);
			if (null == map) {
				log.info("no tick table map data:{}",symbol);
				return;
			}

			eventManager.sendLocalOrRemoteEvent(
					new TickTableReplyEvent(event.getKey(),
							event.getSender(),map));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void injectStrategies(List<DataObject> list) {
		// create running strategies and assign to containers
		for (DataObject obj : list) {
			try {
				StrategyState state = obj.get(StrategyState.class,
						OrderField.STATE.value());
				if (state == null || state.equals(StrategyState.Terminated)) {
					continue;
				}

				String strategyName = obj.get(String.class,
						OrderField.STRATEGY.value());
				IStrategy strategy;
				strategy = strategyFactory.createStrategy(strategyName,
						new Object[] { refDataManager, tickTableManager, obj });
				IStrategyContainer container = getLeastLoadContainer();
				log.debug("strategy " + strategy.getId()
						+ " assigned to container " + container.getId());
				if (strategy instanceof SingleOrderStrategy) {
					ParentOrder parentOrder = ((SingleOrderStrategy) strategy)
							.getParentOrder();
					orders.put(parentOrder.getId(), parentOrder.getAccount(),
							parentOrder);
				}
				AddStrategyEvent addStrategyEvent = new AddStrategyEvent(
						container.getId(), strategy, autoStartStrategy);

				eventManager.sendEvent(addStrategyEvent);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public void init() throws Exception {
		strategyFactory.init();

		for (int i = 0; i < noOfContainers; i++) {
			IStrategyContainer container = (IStrategyContainer) applicationContext
					.getBean("strategyContainer");
			container.init();
			containers.add(container);
		}

		// subscribe to events
		eventProcessor.setHandler(this);
		eventProcessor.init();
		if (eventProcessor.getThread() != null) {
			eventProcessor.getThread().setName("BusinessManager");
		}

		eventMultiProcessor.setHandler(this);
		eventMultiProcessor.init();
		eventMultiProcessor.setName("BusinessTP");

		scheduleManager.scheduleRepeatTimerEvent(closePositionCheckInterval,
				eventProcessor, closePositionCheckEvent);

		if (weekDay != null && cancelPendingOrderTime != null) {
			Calendar cal = Calendar.getInstance();
			while (cal.get(Calendar.DAY_OF_WEEK) != weekDay.getDay()) {
				cal.add(Calendar.DAY_OF_YEAR, 1);
			}

			String[] times = cancelPendingOrderTime.split(":");
			int hr = Integer.parseInt(times[0]);
			int min = Integer.parseInt(times[1]);
			int sec = Integer.parseInt(times[2]);
			cal.set(Calendar.HOUR_OF_DAY, hr);
			cal.set(Calendar.MINUTE, min);
			cal.set(Calendar.SECOND, sec);

			if (TimeUtil.getTimePass(Clock.getInstance().now(), cal.getTime()) >= 0) {
				cal.add(Calendar.DAY_OF_YEAR, 7);
			}

			scheduleManager.scheduleTimerEvent(cal.getTime(), eventProcessor,
					cancelPendingOrderEvent);
			log.info("Schedule cancel pending order event at {}", cal.getTime());
		}
	}

	public void uninit() {
		scheduleManager.uninit();
		eventProcessor.uninit();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	public boolean isSync() {
		return eventProcessor.isSync();
	}

	public void setSync(boolean sync) {
		eventProcessor.setSync(sync);
	}

	public long getClosePositionCheckInterval() {
		return closePositionCheckInterval;
	}

	public void setClosePositionCheckInterval(long closePositionCheckInterval) {
		this.closePositionCheckInterval = closePositionCheckInterval;
	}

	public boolean isCancelAllOrdersAtClose() {
		return cancelAllOrdersAtClose;
	}

	public void setCancelAllOrdersAtClose(boolean cancelAllOrdersAtClose) {
		this.cancelAllOrdersAtClose = cancelAllOrdersAtClose;
	}

	public void setCloseAllPositionsAtClose(boolean closeAllPositionsAtClose) {
		this.closeAllPositionsAtClose = closeAllPositionsAtClose;
	}

	public void setWeekDay(WeekDay weekDay) {
		this.weekDay = weekDay;
	}

	public void setCancelPendingOrderTime(String cancelPendingOrderTime) {
		this.cancelPendingOrderTime = cancelPendingOrderTime;
	}

	public String getCancelPendingOrderTime() {
		return cancelPendingOrderTime;
	}
}
