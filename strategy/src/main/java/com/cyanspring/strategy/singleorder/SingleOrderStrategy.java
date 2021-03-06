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
package com.cyanspring.strategy.singleorder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.Clock;
import com.cyanspring.common.business.ChildOrder;
import com.cyanspring.common.business.FieldDef;
import com.cyanspring.common.business.OrderField;
import com.cyanspring.common.business.ParentOrder;
import com.cyanspring.common.data.AlertType;
import com.cyanspring.common.data.DataObject;
import com.cyanspring.common.event.AsyncEvent;
import com.cyanspring.common.event.AsyncTimerEvent;
import com.cyanspring.common.event.RemoteAsyncEvent;
import com.cyanspring.common.event.alert.ClearSingleAlertEvent;
import com.cyanspring.common.event.marketdata.QuoteEvent;
import com.cyanspring.common.event.marketdata.QuoteSubEvent;
import com.cyanspring.common.event.marketdata.TradeEvent;
import com.cyanspring.common.event.marketsession.MarketSessionEvent;
import com.cyanspring.common.event.order.AmendParentOrderReplyEvent;
import com.cyanspring.common.event.order.AmendStrategyOrderEvent;
import com.cyanspring.common.event.order.CancelParentOrderReplyEvent;
import com.cyanspring.common.event.order.CancelStrategyOrderEvent;
import com.cyanspring.common.event.order.ManualActionReplyEvent;
import com.cyanspring.common.event.order.ManualAmendChildOrderEvent;
import com.cyanspring.common.event.order.ManualCancelChildOrderEvent;
import com.cyanspring.common.event.order.ManualNewChildOrderEvent;
import com.cyanspring.common.event.order.UpdateChildOrderEvent;
import com.cyanspring.common.event.order.UpdateParentOrderEvent;
import com.cyanspring.common.event.strategy.ExecuteEvent;
import com.cyanspring.common.event.strategy.ExecutionInstructionResponseEvent;
import com.cyanspring.common.event.strategy.PauseStrategyEvent;
import com.cyanspring.common.event.strategy.SingleOrderStrategyFieldDefUpdateEvent;
import com.cyanspring.common.event.strategy.StartStrategyEvent;
import com.cyanspring.common.event.strategy.StopStrategyEvent;
import com.cyanspring.common.event.strategy.StrategyEndTimerEvent;
import com.cyanspring.common.event.strategy.StrategyStartTimerEvent;
import com.cyanspring.common.marketdata.Quote;
import com.cyanspring.common.marketdata.Trade;
import com.cyanspring.common.marketsession.MarketSessionType;
import com.cyanspring.common.message.ErrorMessage;
import com.cyanspring.common.message.MessageLookup;
import com.cyanspring.common.staticdata.IRefDataManager;
import com.cyanspring.common.staticdata.ITickTable;
import com.cyanspring.common.staticdata.RefData;
import com.cyanspring.common.staticdata.TickTableManager;
import com.cyanspring.common.strategy.ExecuteTiming;
import com.cyanspring.common.strategy.ExecutionInstruction;
import com.cyanspring.common.strategy.PriceInstruction;
import com.cyanspring.common.strategy.StrategyException;
import com.cyanspring.common.type.ExchangeOrderType;
import com.cyanspring.common.type.ExecType;
import com.cyanspring.common.type.OrdStatus;
import com.cyanspring.common.type.OrderAction;
import com.cyanspring.common.type.OrderSide;
import com.cyanspring.common.type.OrderType;
import com.cyanspring.common.type.StrategyState;
import com.cyanspring.common.type.TimeInForce;
import com.cyanspring.common.util.IdGenerator;
import com.cyanspring.common.util.OrderUtils;
import com.cyanspring.common.util.PriceUtils;
import com.cyanspring.strategy.MarketStatistic;
import com.cyanspring.strategy.Strategy;
import com.cyanspring.strategy.utils.QuoteUtil;

/**
 * @author Dennis Chen
 *
 */
public abstract class SingleOrderStrategy extends Strategy {
	private static final Logger log = LoggerFactory
			.getLogger(SingleOrderStrategy.class);
	protected ParentOrder parentOrder;
	protected IQuantityAnalyzer quantityAnalyzer;
	protected IPriceAnalyzer priceAnalyzer;
	protected Quote quote;
	protected Quote adjQuote;
	protected MarketStatistic marketStatistic = new MarketStatistic();
	protected RefData refData;
	protected ITickTable tickTable;
	protected boolean rejectOnReject;
	protected boolean cancelOnCancel;
	private int maxCancelRetry = 0;
	private int cancelRetry = 0;
	
	private static List<FieldDef> commonFieldDefs;
	
	@Override
	protected Logger getLog() {
		return log;
	}
	
	public void setParentOrder(ParentOrder parentOrder) {
		this.parentOrder = parentOrder;
	}

	@Override
	public StrategyState getState(){
		return parentOrder.getState();
	}
	
	@Override
	public String getAccount() {
		return parentOrder.getAccount();
	}
	
	@Override
	public DataObject getDataObject() {
		return parentOrder;
	}
	
	@Override
	public void init() throws StrategyException {
		logDebug("Initialize strategy: " + getStrategyName() + ", " + this.getId() + ", " + parentOrder.getSymbol() + ", " + parentOrder.getQuantity());
		super.init();
		container.subscribe(PauseStrategyEvent.class, parentOrder.getId(), this);
		container.subscribe(StopStrategyEvent.class, parentOrder.getId(), this);
		container.subscribe(StartStrategyEvent.class, parentOrder.getId(), this);
		container.subscribe(ManualCancelChildOrderEvent.class, parentOrder.getId(), this);
		container.subscribe(ManualNewChildOrderEvent.class, parentOrder.getId(), this);
		container.subscribe(ManualAmendChildOrderEvent.class, parentOrder.getId(), this);
		container.subscribe(UpdateChildOrderEvent.class, parentOrder.getId(), this);
		container.subscribe(AmendStrategyOrderEvent.class, parentOrder.getId(), this);
		container.subscribe(CancelStrategyOrderEvent.class, parentOrder.getId(), this);
		container.subscribe(ExecuteEvent.class, parentOrder.getId(), this);
		container.subscribe(ClearSingleAlertEvent.class, parentOrder.getId(), this);
		container.subscribe(MarketSessionEvent.class, null, this);
		container.subscribe(ExecutionInstructionResponseEvent.class, parentOrder.getId(), this);
		
		if(isQuoteRequired()) {
			container.subscribe(QuoteEvent.class, parentOrder.getSymbol(), this);
			container.subscribe(QuoteEvent.class, parentOrder.getId(), this);
			QuoteSubEvent quoteSubEvent = new QuoteSubEvent(parentOrder.getId(), null, parentOrder.getSymbol());
			container.sendEvent(quoteSubEvent);
		}
		
		if(isTimerEventRequired())
			container.scheduleRepeatTimerEvent(getTimerInterval(), this, timerEvent);
		
		setStartEndTimer();
	}
	
	@Override
	public void uninit() {
		super.uninit();
		container.unsubscribe(PauseStrategyEvent.class, parentOrder.getId(), this);
		container.unsubscribe(StopStrategyEvent.class, parentOrder.getId(), this);
		container.unsubscribe(StartStrategyEvent.class, parentOrder.getId(), this);
		container.unsubscribe(ManualCancelChildOrderEvent.class, parentOrder.getId(), this);
		container.unsubscribe(ManualNewChildOrderEvent.class, parentOrder.getId(), this);
		container.unsubscribe(ManualAmendChildOrderEvent.class, parentOrder.getId(), this);
		container.unsubscribe(UpdateChildOrderEvent.class, parentOrder.getId(), this);
		container.unsubscribe(AmendStrategyOrderEvent.class, parentOrder.getId(), this);
		container.unsubscribe(CancelStrategyOrderEvent.class, parentOrder.getId(), this);
		container.unsubscribe(ExecuteEvent.class, parentOrder.getId(), this);
		container.unsubscribe(MarketSessionEvent.class, null, this);
		container.unsubscribe(ClearSingleAlertEvent.class, parentOrder.getId(), this);
		container.unsubscribe(ExecutionInstructionResponseEvent.class, parentOrder.getId(), this);
		
		if(isQuoteRequired()) {
			container.unsubscribe(QuoteEvent.class, parentOrder.getSymbol(), this);
			container.unsubscribe(QuoteEvent.class, parentOrder.getId(), this);
		}
		
		if(isTimerEventRequired())
			container.cancelTimerEvent(timerEvent);
		
		cancelStartEndTimer();
	}

	
	/* 
	 * Expected parameters:
	 * 		RefDataManager
	 * 		TickTableManager
	 * 		ParentOrder
	 */
	@Override
	public void create(Object... objects) throws StrategyException {
		IRefDataManager refDataManager = (IRefDataManager)objects[0];
		TickTableManager tickTableManager = (TickTableManager)objects[1];
		
		ParentOrder order = (ParentOrder)objects[2];
		setParentOrder(order);

		RefData refData = refDataManager.getRefData(order.getSymbol());
		if(null == refData)
			throw new StrategyException("Can't find symbol in refdata: " + order.getSymbol());
		setRefData(refData);

		ITickTable tickTable = tickTableManager.getTickTable(refData);
		if(null == tickTable)
			throw new StrategyException("Can't find tick table for: " + order.getSymbol());
		setTickTable(tickTable);
	}
	
	@Override
	public RemoteAsyncEvent createConfigUpdateEvent() throws StrategyException {
			return new SingleOrderStrategyFieldDefUpdateEvent(null, null, getStrategyName(), getCombinedFieldDefs());
	}
	
	
	@Override
	protected void updateStrategyState(StrategyState state) {
		parentOrder.setState(state);
		String txId = parentOrder.get(String.class, OrderField.CLORDERID.value());
		UpdateParentOrderEvent updateEvent = 
			new UpdateParentOrderEvent(parentOrder.getId(), ExecType.RESTATED, txId, parentOrder, null);
		container.sendEvent(updateEvent);
		
	}
	
	@Override
	protected void error(String info) {
		marketStatistic.reset();
		super.error(info);
	}

	
	@Override
	public void stop() {
		super.stop();
		marketStatistic.reset();
	}
	
	@Override
	public void terminate() {
		super.terminate();
		marketStatistic.reset();
	}

	private boolean checkCompletion() {
		if(PriceUtils.EqualGreaterThan(parentOrder.getCumQty(), parentOrder.getQuantity())) {
			logInfo("Strategy completed");
			if(!parentOrder.getState().equals(StrategyState.Terminated))
				terminate();
			return true;
		}
		return false;
	}
	
	public void setAlert(AlertType alertType, String msg) {
		parentOrder.put(OrderField.ALERT_TYPE.value(), alertType);
		parentOrder.put(OrderField.ALERT_MSG.value(), msg);
		UpdateParentOrderEvent updateEvent = 
			new UpdateParentOrderEvent(parentOrder.getId(), ExecType.RESTATED, IdGenerator.getInstance().getNextID(), parentOrder, null);
		container.sendEvent(updateEvent);
	}
	
	public void clearAlert() {
		parentOrder.remove(OrderField.ALERT_TYPE.value());
		parentOrder.remove(OrderField.ALERT_MSG.value());
		UpdateParentOrderEvent updateEvent = 
			new UpdateParentOrderEvent(parentOrder.getId(), ExecType.RESTATED, IdGenerator.getInstance().getNextID(), parentOrder, null);
		container.sendEvent(updateEvent);
	}

	@Override
	public ChildOrder createChildOrder(String parentId, String symbol, OrderSide side, double quantity, double price, ExchangeOrderType type) {
		//ignore id, symbol & side specification for single order strategy. They should be the same as parent orders.
		ChildOrder order = parentOrder.createChild(quantity, price, type);
		if(this.isSimMarketOrder() && this.parentOrder.getOrderType().equals(OrderType.Market)) {
			log.debug("Setting order IOC: " + order.getId());
			order.put(OrderField.TIF.value(), TimeInForce.IMMEDIATE_OR_CANCEL);
		}
		
		return order;
	}

	public double getQtyInMarket() {
		double result = 0;
		for(ChildOrder order: getOpenChildOrdersByParent(parentOrder.getId())) {
			if(order.getOrdStatus().isReady())
				result += order.getQuantity() - order.getCumQty();
		}
		return result;
	}

	protected PriceInstruction analyze() {
		return null;
	}
	
	@Override
	public void execute(ExecuteTiming timing) {
		updateExecuteTiming(timing);
		
		if(pendingExecInstrEvent != null) {
			timeOutCheck();
			return;
		}
		
		if(checkCompletion())
			return;
		
		if(parentOrder.getState().equals(StrategyState.Terminated) || 
		   parentOrder.getState().equals(StrategyState.Error) ||
		   parentOrder.getState().equals(StrategyState.Stopped))
							return;
						
		if(checkStartEndTime())
			return;
		
		if(parentOrder.getState().equals(StrategyState.Stopping)) {
			if(childOrderCount() == 0)
				updateStrategyState(StrategyState.Stopped);
			else
				withDrawAllChildOrders();
			return;
		}

		if(parentOrder.getState().equals(StrategyState.Paused) && pendingAckHandler == null)
			return;
			
		if(!preCheck())
			return;
		
		if(isQuoteRequired()) {
			if(quote == null || quote.isStale()) {
				logDebug("not doing anything since we haven't recieved any quote yet");
				return;
			}
			
			Set<ChildOrder> children = getSortedOpenChildOrdersBySymbol(quote.getSymbol());
			if(isCheckAdjQuote())
				adjQuote = OrderUtils.calAdjustedQuote(quote, children);
			else 
				adjQuote = quote;
			
			if(adjQuote == null) {
				logDebug("Our orders are still not yet reflected in market image");
				return;
			} 
			
			if(isValidateQuote() && !QuoteUtil.validateQuote(adjQuote)){
				return;
			} 
		}
		setLastExecuted(Clock.getInstance().now());
		setExecuteTiming(ExecuteTiming.NORMAL);
		
		//QPE framework
		PriceInstruction pi = null;
		QuantityInstruction qi = null;
		if(null != quantityAnalyzer && null != priceAnalyzer) {
			qi = quantityAnalyzer.analyze(this);
			pi = priceAnalyzer.analyze(qi, this);
		} else {
			pi = this.analyze();
		}
		List<ExecutionInstruction> eis = executionAnalyzer.analyze(pi, this);
		
		if(pendingAckHandler != null)
			eis = pendingAckHandler.preHandle(eis);
		
		if(eis != null && eis.size()>0) { // to reduce logging, only log when it comes up some changes
			Collection<ChildOrder> children = this.getChildOrders();
			logInfo("Parent order: " + "Qty: " + parentOrder.getQuantity() + ", Cum: " + parentOrder.getCumQty());
			logInfo("Open orders: " + children);
			logInfo("Quote: " + quote);
			logInfo("AdjQuote: " + adjQuote);
			if(null != qi)
				logInfo("QuantityInstruction: " + qi);
			logInfo("PriceInstruction: " + pi);
			logInfo("ExecutionInstruction: " + eis);
		}
//		else {
//			Collection<ChildOrder> children = this.getChildOrders();
//			logDebug("Parent order: " + "Qty: " + parentOrder.getQuantity() + ", Cum: " + parentOrder.getCumQty());
//			logDebug("Open orders: " + children);
//			logDebug("Quote: " + quote);
//			logDebug("AdjQuote: " + adjQuote);
//			if(null != qi)
//				logDebug("QuantityInstruction: " + qi);
//			logDebug("PriceInstruction: " + pi);
//			logDebug("ExecutionInstruction: " + eis);
//		}

		executeInstructions(eis);
	}

	
	
	private boolean checkManualOverFill(double qty, String sender) {
		double inMarketQty = getQtyInMarket() + qty;
		if(PriceUtils.GreaterThan(inMarketQty, parentOrder.getRemainingQty())) {
			String message = MessageLookup.buildEventMessage(ErrorMessage.OVER_FILLED, "This would have overfilled parent order");

			container.sendRemoteEvent(
					new ManualActionReplyEvent(null, sender, false, message));
			return false;
		}
		return true;
	}
	
	private boolean checkManualOrderPrice(double p1, double p2, String sender) {
		if(!OrderUtils.isBetterPrice(parentOrder.getSide(), p1, p2)){
			String message = MessageLookup.buildEventMessage(ErrorMessage.PRICE_NOT_PERMITTED, "Price is not permitted by parentOrder");

			container.sendRemoteEvent(
					new ManualActionReplyEvent(null, sender, false, message));
			return false;
		}
		return true;
	}
	
	protected void processManualCancelChildOrderEvent(ManualCancelChildOrderEvent event) {
		if(!checkPendingAck(event.getKey(), event.getSender())) {
			return;
		}
		
		List<ExecutionInstruction> ei = new ArrayList<ExecutionInstruction>();
		for(String childOrderId: event.getChildOrderIds()) {
			ChildOrder order = getChildOrder(childOrderId);
			if(order == null) {
				String message = MessageLookup.buildEventMessage(ErrorMessage.NO_ORDER_IN_ACTIVE_CHILD_ORDER, "cant find order in active child orders");

				container.sendRemoteEvent(
						new ManualActionReplyEvent(null, event.getSender(), false, message));
				return;
			}
			ei.add(new ExecutionInstruction(OrderAction.CANCEL, order, null));
		}
		pendingAckHandler = new ManualPendingAckHandler(event.getKey(), event.getSender());
		executeInstructions(ei);
	}
	
	protected void processManualNewChildOrderEvent(ManualNewChildOrderEvent event) {
		if(!checkPendingAck(null, event.getSender())) {
			return;
		}
		
		if(!checkManualOverFill(event.getQuantity(), event.getSender()))
			return;
		
		if(!checkManualOrderPrice(event.getPrice(), parentOrder.getPrice(), event.getSender()))
			return;
		
		List<ExecutionInstruction> ei = new ArrayList<ExecutionInstruction>();
		ChildOrder order = parentOrder.createChild(event.getQuantity(), event.getPrice(), event.getOrderType());
		ei.add(new ExecutionInstruction(OrderAction.NEW, order, null));
		pendingAckHandler = new ManualPendingAckHandler(event.getKey(), event.getSender());
		executeInstructions(ei);
	}

	protected void processManualAmendChildOrderEvent(
			ManualAmendChildOrderEvent event) {
		if(!checkPendingAck(event.getKey(), event.getSender())) {
			return;
		}

		List<ExecutionInstruction> ei = new ArrayList<ExecutionInstruction>();
		ChildOrder order = getChildOrder(event.getChildOrderId());
		if(order == null) {
			String message = MessageLookup.buildEventMessage(ErrorMessage.NO_ORDER_IN_ACTIVE_CHILD_ORDER, "cant find order in active child orders");

			container.sendRemoteEvent(
					new ManualActionReplyEvent(null, event.getSender(), false, message));
			return;
		}
		
		if(PriceUtils.LessThan(event.getQuantity(), order.getCumQty())) {
			String message = MessageLookup.buildEventMessage(ErrorMessage.CUM_QTY_GREATER_THAN_INTENTED_QTY, "CumQty is greater than intended quantity");

			container.sendRemoteEvent(
					new ManualActionReplyEvent(null, event.getSender(), false,message));
			return;
		}

		if(!checkManualOverFill(event.getQuantity()-order.getQuantity(), event.getSender()))
			return;

		if(!checkManualOrderPrice(event.getPrice(), parentOrder.getPrice(), event.getSender()))
			return;
		

		Map<String, Object> changeFields = new HashMap<String, Object>();
		changeFields.put(OrderField.QUANTITY.value(), event.getQuantity());
		changeFields.put(OrderField.PRICE.value(), event.getPrice());
		ei.add(new ExecutionInstruction(OrderAction.AMEND, order, changeFields));
		pendingAckHandler = new ManualPendingAckHandler(event.getKey(), event.getSender());
		executeInstructions(ei);
	}

	protected void preProcessUpdateChildOrderEvent(UpdateChildOrderEvent event) {
		super.preProcessUpdateChildOrderEvent(event);
		
		if(event.getExecution() != null) {
			setLastExecutionTime(Clock.getInstance().now());
			parentOrder.processExecution(event.getExecution());
			marketStatistic.executionUpdate(event.getExecution());
			ExecType execType = ExecType.PARTIALLY_FILLED;
			if(PriceUtils.EqualGreaterThan(parentOrder.getCumQty(), parentOrder.getQuantity()))
				execType = ExecType.FILLED;
		
			String txId = parentOrder.get(String.class, OrderField.CLORDERID.value());
			UpdateParentOrderEvent updateEvent = 
				new UpdateParentOrderEvent(parentOrder.getId(), execType, txId, parentOrder, null);
			
			container.sendEvent(updateEvent);
		}
	}
	
	@Override
	protected void postProcessUpdateChildOrderEvent(UpdateChildOrderEvent event) {
		super.postProcessUpdateChildOrderEvent(event);
		ChildOrder order = event.getOrder();
		if(order.getOrdStatus().equals(OrdStatus.CANCELED) && order.isUnsolicited() && cancelOnCancel) {
			log.debug("postProcessUpdateChildOrderEvent: " + parentOrder.getOrderType() + "," + cancelRetry + "," + maxCancelRetry);
			if(!parentOrder.getOrderType().equals(OrderType.Market) || !this.isSimMarketOrder() || cancelRetry >= maxCancelRetry) {
				log.debug("Received unsolicited cancel on child order, cancelling parent order: " + parentOrder);
				parentOrder.setOrdStatus(OrdStatus.CANCELED);
				parentOrder.touch();
				terminate();
				return;
			} else {
				cancelRetry++;
			}
			return;
		} else if(!parentOrder.getOrdStatus().isCompleted() && 
				order.getOrdStatus().equals(OrdStatus.REJECTED) && 
				rejectOnReject) {
			parentOrder.setOrdStatus(OrdStatus.REJECTED);
			parentOrder.touch();
			terminate();
			return;
		}
		
		executeWithTiming(ExecuteTiming.ASAP, event.getClass());
	}

	private boolean startEndTimeChanged(AmendStrategyOrderEvent event) {
		if(event.getFields().containsKey(OrderField.START_TIME.value()) ||
				event.getFields().containsKey(OrderField.START_TIME.value()))
			return true;
		return false;
	}
	
	class AmendPendingHandler extends PendingAckHandler {

		AmendStrategyOrderEvent event;
		Map<String, Object> originalFields;
		public AmendPendingHandler(AmendStrategyOrderEvent event, Map<String, Object> originalFields) {
			this.event = event;
			this.originalFields = originalFields;
		}
		@Override
		public void postHandle(boolean success, String info) {
			if(!success) { // roll back
				parentOrder.update(originalFields);
			} else {
				amendParentOrder(event);
				UpdateParentOrderEvent update = new UpdateParentOrderEvent(parentOrder.getId(), ExecType.REPLACE, event.getTxId(), parentOrder, info);
				container.sendEvent(update);
			}
			AmendParentOrderReplyEvent reply = new AmendParentOrderReplyEvent(
					event.getSourceId(), event.getReceiver(), success, info, event.getTxId(), parentOrder);
			
			logDebug("AmendParentOrderReplyEvent sent: " + reply.getTxId() + ", " + reply.isOk());
			container.sendLocalOrRemoteEvent(reply);
			
			container.sendEvent(new ExecuteEvent(parentOrder.getId(), ExecuteTiming.NOW));
		}
		
		@Override
		public List<ExecutionInstruction> preHandle(List<ExecutionInstruction> eis) {
			// remove new order instructions to perform risk-free action before
			// acknowledge amendment
			List<ExecutionInstruction> toBeRemoved = new ArrayList<ExecutionInstruction>();
			for(ExecutionInstruction ei: eis) {
				if(ei.getAction().equals(OrderAction.NEW))
					toBeRemoved.add(ei);
			}
			
			for(ExecutionInstruction ei: toBeRemoved) {
				eis.remove(ei);
			}
			logDebug("AmendPendingHandler.preHandle: " + eis);
			return eis;
		}
		
	};
	
	protected AmendParentOrderReplyEvent getImmediateAmendReply(AmendStrategyOrderEvent event) {
		AmendParentOrderReplyEvent reply = null;
		if( pendingAckHandler != null || pendingExecInstrEvent != null) {
			String message = MessageLookup.buildEventMessage(ErrorMessage.ORDER_IS_PENDING, "Order is pending on instruction/amendment/cancellation");
			
			reply = new AmendParentOrderReplyEvent(
					event.getSourceId(), event.getReceiver(), false,message , event.getTxId(), parentOrder);
			return reply;
		} 
		
		Map<String, Object> fields = event.getFields();
		Double qty = (Double)fields.get(OrderField.QUANTITY.value());
		if(qty != null && PriceUtils.GreaterThan(parentOrder.getCumQty(), qty)) {
			String message = MessageLookup.buildEventMessage(ErrorMessage.OVER_FILLED,"This would cause overfilled");
			reply = new AmendParentOrderReplyEvent(
					event.getSourceId(), event.getReceiver(), false,message, event.getTxId(), parentOrder);
			return reply;
		}

		boolean delayAccept = false;
		Double price = (Double)fields.get(OrderField.PRICE.value());
		if(pendingExecInstrEvent != null) {
			delayAccept = true;
		} else if(childOrderCount() > 0) {
			if(price != null && !price.equals(parentOrder.getPrice())) {
				delayAccept = true;
			} else if (qty != null && PriceUtils.LessThan(qty, parentOrder.getQuantity())) {
				delayAccept = true;
			}
		}
		
		if(!delayAccept) {
			amendParentOrder(event);
			reply = new AmendParentOrderReplyEvent(
					event.getSourceId(), event.getReceiver(), true, "", event.getTxId(), parentOrder);
		}
		
		return reply;
	}
	
	private void amendParentOrder(AmendStrategyOrderEvent event) {
		parentOrder.update(event.getFields());
		String source = parentOrder.get(String.class, OrderField.SOURCE.value());
		// only update clOrdID when the amendment is from same source
		if(null != event.getTxId() && null != event.getSourceId() && event.getSourceId().equals(source))
			parentOrder.put(OrderField.CLORDERID.value(), event.getTxId());
		parentOrder.setOrdStatus(OrdStatus.REPLACED);
		parentOrder.touch();
		if(startEndTimeChanged(event))
			setStartEndTimer();
	}
	
	protected void processAmendStrategyOrderEvent(AmendStrategyOrderEvent event) {
		logDebug("processAmendStrategyOrderEvent received: " + event.getSourceId() + ", " + event.getFields());
		AmendParentOrderReplyEvent reply = getImmediateAmendReply(event);

		if(reply != null) {
			logDebug("AmendParentOrderReplyEvent sent: " + reply.getTxId() + ", " + reply.isOk());
			container.sendLocalOrRemoteEvent(reply);
			
			if(reply.isOk()) {
				UpdateParentOrderEvent update = new UpdateParentOrderEvent(parentOrder.getId(), ExecType.REPLACE, event.getTxId(), parentOrder, null);
				container.sendEvent(update);
			}
		} else {
			final Map<String, Object> originalFields = parentOrder.update(event.getFields());
			pendingAckHandler = new AmendPendingHandler(event, originalFields);
		}
		
		execute(ExecuteTiming.NOW);
	}
	
	class CancelPendingHandler extends PendingAckHandler {
		CancelStrategyOrderEvent event;
		public CancelPendingHandler(CancelStrategyOrderEvent event) {
			this.event = event;
		}

		@Override
		public void postHandle(boolean success, String info) {
			if(success){
				cancelParentOrder(event);
				if(null != event.getReason())
					parentOrder.setReason(event.getReason());
				UpdateParentOrderEvent update = new UpdateParentOrderEvent(parentOrder.getId(), ExecType.CANCELED, event.getTxId(), parentOrder, info);
				container.sendEvent(update);
			}
			CancelParentOrderReplyEvent reply = new CancelParentOrderReplyEvent(
					event.getSourceId(), event.getReceiver(), success, info, event.getTxId(), parentOrder);
			
			logDebug("CancelParentOrderReplyEvent sent: " + reply.getKey() + ", " +
					reply.getReceiver() + ", " +
					reply.getTxId() + ", " + reply.isOk());
			container.sendLocalOrRemoteEvent(reply);
		}

		@Override
		public List<ExecutionInstruction> preHandle(List<ExecutionInstruction> eis) {
			// remove all child orders
			PriceInstruction pi = new PriceInstruction();
			List<ExecutionInstruction> ei = executionAnalyzer.analyze(pi, SingleOrderStrategy.this);
			logInfo("CancelPendingHandler has changed executionInstruction: " + ei);
			return ei;
		}
		
	}
	
	private void cancelParentOrder(CancelStrategyOrderEvent event) {
		String source = parentOrder.get(String.class, OrderField.SOURCE.value());
		// only update clOrdID when the amendment is from same source
		if(null != event.getTxId() && null != event.getSourceId() && event.getSourceId().equals(source))
			parentOrder.put(OrderField.CLORDERID.value(), event.getTxId());
		parentOrder.setOrdStatus(OrdStatus.CANCELED);
		parentOrder.touch();
		terminate();
	}
	
	protected void processCancelStrategyOrderEvent(CancelStrategyOrderEvent event) {
		logDebug("processCancelStrategyOrderEvent received: " + event.getSourceId());
		CancelParentOrderReplyEvent reply = null;
		
		if(event.isForce() || pendingExecInstrEvent == null && childOrderCount() == 0) {
			cancelParentOrder(event);
			reply = new CancelParentOrderReplyEvent(
					event.getSourceId(), event.getSender(), true, null, event.getTxId(), parentOrder);
			container.sendLocalOrRemoteEvent(reply);
			if(null != event.getReason())
				parentOrder.setReason(event.getReason());
			container.sendEvent(new UpdateParentOrderEvent(parentOrder.getId(), ExecType.CANCELED, event.getTxId(), parentOrder, null));
			return;
		}
		
		if( pendingAckHandler != null || pendingExecInstrEvent != null) {
			String message = MessageLookup.buildEventMessage(ErrorMessage.ORDER_IS_PENDING, "Order has pending amendment/cancellation");

			reply = new CancelParentOrderReplyEvent(
					event.getSourceId(), event.getSender(), false, message, event.getTxId(), parentOrder);
			container.sendLocalOrRemoteEvent(reply);
			log.debug("Cancelling order with pending amendment/cancellation: " + event.getKey());
			return;
		} 
		
		pendingAckHandler = new CancelPendingHandler(event);
		execute(ExecuteTiming.NOW);
	}
	
	protected void processClearSingleAlertEvent(ClearSingleAlertEvent event) {
		clearAlert();
	}
	
	@Override
	public void onEvent(AsyncEvent event) {
		if (event instanceof AsyncTimerEvent) {
			processAsyncTimerEvent((AsyncTimerEvent)event);
		}else if (event instanceof ExecutionInstructionResponseEvent) {
			processExecutionInstructionResponseEvent((ExecutionInstructionResponseEvent)event);
		} else if (event instanceof QuoteEvent) {
			processQuoteEvent((QuoteEvent)event);
		} else if (event instanceof TradeEvent) {
			processTradeEvent((TradeEvent)event);
		} else if (event instanceof PauseStrategyEvent) {
			processPauseStrategyEvent((PauseStrategyEvent)event);
		} else if (event instanceof StopStrategyEvent) {
			processStopStrategyEvent((StopStrategyEvent)event);
		} else if (event instanceof StartStrategyEvent) {
			processStartStrategyEvent((StartStrategyEvent)event);
		} else if (event instanceof UpdateChildOrderEvent) {
			processUpdateChildOrderEvent((UpdateChildOrderEvent)event);
		} else if (event instanceof ManualCancelChildOrderEvent) {
			processManualCancelChildOrderEvent((ManualCancelChildOrderEvent)event);
		} else if (event instanceof ManualAmendChildOrderEvent) {
			processManualAmendChildOrderEvent((ManualAmendChildOrderEvent)event);
		} else if (event instanceof ManualNewChildOrderEvent) {
			processManualNewChildOrderEvent((ManualNewChildOrderEvent)event);
		} else if (event instanceof AmendStrategyOrderEvent) {
			processAmendStrategyOrderEvent((AmendStrategyOrderEvent)event);
		} else if (event instanceof CancelStrategyOrderEvent) {
			processCancelStrategyOrderEvent((CancelStrategyOrderEvent)event);
		} else if (event instanceof MarketSessionEvent) {
			processMarketSessionEvent((MarketSessionEvent)event);
		} else if (event instanceof ExecuteEvent) {
			processExecuteEvent((ExecuteEvent)event);
		} else if (event instanceof StrategyStartTimerEvent) {
			processStrategyStartTimerEvent((StrategyStartTimerEvent)event);
		} else if (event instanceof StrategyEndTimerEvent) {
			processStrategyEndTimerEvent((StrategyEndTimerEvent)event);
		} else if (event instanceof ClearSingleAlertEvent) {
			processClearSingleAlertEvent((ClearSingleAlertEvent)event);
		} else {
			log.warn("Unhandled event: " + event);
		}
	}

	protected void processPauseStrategyEvent(PauseStrategyEvent event) {
		log.debug("received PauseStrategyEvent");
		pause();
		execute(ExecuteTiming.NOW);
	}
	
	protected void processStopStrategyEvent(StopStrategyEvent event) {
		logDebug("received StopStrategyEvent");
		stop();
		execute(ExecuteTiming.NOW);
	}
	
	protected void processStartStrategyEvent(StartStrategyEvent event) {
		start();
		execute(ExecuteTiming.NOW);
	}
	
	protected void processQuoteEvent(QuoteEvent event) {
		logDebug("QuoteEvent: " + event.getQuote().toString());
		quote = event.getQuote();
		
		executeWithTiming(ExecuteTiming.ASAP, event.getClass());
	}

	protected void processTradeEvent(TradeEvent event) {
		logDebug("TradeEvent: " + event.getTrade().getQuantity() + ", " + event.getTrade().getPrice());
		Trade trade = event.getTrade();
		double limit = parentOrder.getPrice();
		if(getState().equals(StrategyState.Running) && OrderUtils.inLimit(trade.getPrice(), limit, parentOrder.getSide())) {
			marketStatistic.tradeUpdate(trade);
		}
	}
	
	///////////////////////
	//
	// getters and setters
	//
	///////////////////////
	public String getId() {
		return parentOrder.getId();
	}
	
	public ParentOrder getParentOrder() {
		return parentOrder;
	}

	public IQuantityAnalyzer getQuantityAnalyzer() {
		return quantityAnalyzer;
	}


	public void setQuantityAnalyzer(IQuantityAnalyzer quantityAnalyzer) {
		this.quantityAnalyzer = quantityAnalyzer;
	}


	public IPriceAnalyzer getPriceAnalyzer() {
		return priceAnalyzer;
	}


	public void setPriceAnalyzer(IPriceAnalyzer priceAnalyzer) {
		this.priceAnalyzer = priceAnalyzer;
	}


	public Quote getQuote() {
		return quote;
	}


	public RefData getRefData() {
		return refData;
	}


	public void setRefData(RefData refData) {
		this.refData = refData;
	}

	public MarketStatistic getMarketStatistic() {
		return marketStatistic;
	}

	public ITickTable getTickTable() {
		return tickTable;
	}

	public void setTickTable(ITickTable tickTable) {
		this.tickTable = tickTable;
	}

	public Quote getAdjQuote() {
		return adjQuote;
	}

	public MarketSessionType getMarketSession() {
		return marketSession;
	}

	@Override
	public Date getStartTime(){
		return parentOrder.getStartTime();
	}
	@Override
	public Date getEndTime() {
		return parentOrder.getEndTime();
	}
	
	@Override
	public List<FieldDef> getCommonFieldDefs() {
		return commonFieldDefs;
	}

	public static void setCommonFieldDefs(List<FieldDef> commonFieldDefs) {
		SingleOrderStrategy.commonFieldDefs = commonFieldDefs;
	}
	
	public boolean isRejectOnReject() {
		return rejectOnReject;
	}

	public void setRejectOnReject(boolean rejectOnReject) {
		this.rejectOnReject = rejectOnReject;
	}

	public boolean isCancelOnCancel() {
		return cancelOnCancel;
	}

	public void setCancelOnCancel(boolean cancelOnCancel) {
		this.cancelOnCancel = cancelOnCancel;
	}

	public int getMaxCancelRetry() {
		return maxCancelRetry;
	}

	public void setMaxCancelRetry(int maxCancelRetry) {
		this.maxCancelRetry = maxCancelRetry;
	}	
	
}
