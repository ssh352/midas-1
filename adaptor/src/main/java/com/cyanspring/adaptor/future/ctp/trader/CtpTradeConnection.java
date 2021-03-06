package com.cyanspring.adaptor.future.ctp.trader;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cyanspring.adaptor.future.ctp.trader.client.CtpPosition;
import com.cyanspring.adaptor.future.ctp.trader.client.CtpTraderProxy;
import com.cyanspring.adaptor.future.ctp.trader.client.ILtsTraderListener;
import com.cyanspring.adaptor.future.ctp.trader.client.TraderHelper;
import com.cyanspring.adaptor.future.ctp.trader.generated.CThostFtdcInvestorPositionField;
import com.cyanspring.adaptor.future.ctp.trader.generated.CThostFtdcOrderField;
import com.cyanspring.adaptor.future.ctp.trader.generated.CThostFtdcTradeField;
import com.cyanspring.adaptor.future.ctp.trader.generated.TraderLibrary;
import com.cyanspring.common.business.ChildOrder;
import com.cyanspring.common.business.Execution;
import com.cyanspring.common.business.ISymbolConverter;
import com.cyanspring.common.business.OrderException;
import com.cyanspring.common.business.OrderField;
import com.cyanspring.common.downstream.DownStreamException;
import com.cyanspring.common.downstream.IDownStreamConnection;
import com.cyanspring.common.downstream.IDownStreamListener;
import com.cyanspring.common.downstream.IDownStreamSender;
import com.cyanspring.common.event.AsyncEvent;
import com.cyanspring.common.event.AsyncTimerEvent;
import com.cyanspring.common.event.IAsyncEventListener;
import com.cyanspring.common.event.ScheduleManager;
import com.cyanspring.common.type.ExecType;
import com.cyanspring.common.type.OrdStatus;
import com.cyanspring.common.util.DailyKeyCounter;
import com.cyanspring.common.util.IdGenerator;
import com.cyanspring.common.util.PriceUtils;
import com.cyanspring.common.util.TimeThrottler;
import com.cyanspring.common.util.TimeUtil;

public class CtpTradeConnection implements IDownStreamConnection, ILtsTraderListener, IAsyncEventListener {

	private static final Logger log = LoggerFactory
			.getLogger(CtpTradeConnection.class);

	private String url;
	private String conLog;
	private String id;
	private String broker;
	private int maxOrderCount;
	private int maxOpenPosition;
	private boolean state;
	private IDownStreamListener listener;
	private DownStreamSender downStreamSender = new DownStreamSender();
	private ISymbolConverter symbolConverter;

	private Map<String, String> exchangeSerial2Serial = new ConcurrentHashMap<String, String>();
	private Map<String, ChildOrder> serialToOrder = new ConcurrentHashMap<String, ChildOrder>();
	private Map<String, Double> tradePendings = new ConcurrentHashMap<String, Double>();
	private CtpPositionRecord positionRecord;
	private ScheduleManager scheduleManager = new ScheduleManager();
	private AsyncTimerEvent queryPositionEvent = new AsyncTimerEvent();
	private long queryPositionInterval = 60000;
	private long timerInterval = 3000;
	private TimeThrottler throttler;
	private boolean positionQueried;
	private DailyKeyCounter placeOrderCount;
    private DailyKeyCounter cancelOrderCount;

	// client to delegate ctp
	private CtpTraderProxy proxy;

	public CtpTradeConnection(String id, String url, String broker, String conLog,
			String user, String password, ISymbolConverter symbolConverter, int maxOrderCount, int maxOpenPosition) {
		this.id = id;
		this.url = url;
		this.broker = broker;
		this.conLog = conLog;
		this.symbolConverter = symbolConverter;
		this.maxOrderCount = maxOrderCount;
		this.maxOpenPosition = maxOpenPosition;
		this.positionRecord = new CtpPositionRecord(0, maxOpenPosition);
		proxy = new CtpTraderProxy(id, url, broker, conLog, user, password, symbolConverter);
	}

	@Override
	public void init() throws Exception {
		placeOrderCount = new DailyKeyCounter(maxOrderCount);
        cancelOrderCount = new DailyKeyCounter(maxOrderCount);
		registerListeners();
		positionRecord.clear();
		throttler = new TimeThrottler(queryPositionInterval);
		scheduleManager.scheduleRepeatTimerEvent(timerInterval, this, queryPositionEvent);
		proxy.init();
	}

	private void registerListeners() throws DownStreamException {
	}

	@Override
	public void uninit() {
		scheduleManager.cancelTimerEvent(queryPositionEvent);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean getState() {
		return proxy.getState();
	}

	private String convertDownSymbol(String symbol) {
		if (null != symbolConverter) {
			return symbolConverter.convertDown(symbol);
		}
		return symbol;
	}

	private String convertUpSymbol(String symbol) {
		if (null != symbolConverter) {
			return symbolConverter.convertUp(symbol);
		}
		return symbol;
	}

	class DownStreamSender implements IDownStreamSender {
		@Override
		synchronized public void newOrder(ChildOrder order) throws DownStreamException {
			if (!placeOrderCount.check(order.getSymbol())) {
				order = order.clone();
				order.setOrdStatus(OrdStatus.REJECTED);
				String msg = "Max new order count reached: " + CtpTradeConnection.this.maxOrderCount;
				log.error(msg);
				CtpTradeConnection.this.listener.onOrder(ExecType.REJECTED, order, null, msg);
				return;
			}

			String ordRef = proxy.getClOrderId();
			serialToOrder.put(ordRef, order);
			order.setClOrderId(ordRef);
			String symbol = convertDownSymbol(order.getSymbol());
			byte flag = positionRecord.holdQuantity(symbol, order.getSide().isBuy(), order.getQuantity());
			if (TraderLibrary.THOST_FTDC_OF_Open == flag &&
					!positionRecord.checkMaxOpenPosition(symbol)) {
				order = order.clone();
				order.setOrdStatus(OrdStatus.REJECTED);
				String msg = "Max open position reach: " + CtpTradeConnection.this.maxOpenPosition;
				log.error(msg);
				CtpTradeConnection.this.listener.onOrder(ExecType.REJECTED, order, null, msg);
				return;
			}

			order.put(OrderField.FLAG.value(), flag);
			proxy.newOrder(ordRef, order);
			log.info("Send Order: " + order.getId() + ", " + ordRef + ", " + TraderHelper.readCombOffsetFlag(flag));
		}

		@Override
		public void amendOrder(ChildOrder order, Map<String, Object> fields)
				throws DownStreamException {
			throw new DownStreamException("Amend order not support");
		}

		@Override
		public void cancelOrder(ChildOrder order) throws DownStreamException {
            if (!cancelOrderCount.check(order.getSymbol())) {
                order = order.clone();
                order.setOrdStatus(OrdStatus.REJECTED);
                String msg = "Max cancel order count reach: " + CtpTradeConnection.this.maxOrderCount;
                log.error(msg);
                CtpTradeConnection.this.listener.onOrder(ExecType.REJECTED, order, null, msg);
                return;
            }
			proxy.cancelOrder(order);
			log.info("Cancel Order: " + order.getClOrderId());
		}

		@Override
		public boolean getState() {
			return CtpTradeConnection.this.getState();
		}

	}

	@Override
	public IDownStreamSender setListener(IDownStreamListener listener)
			throws DownStreamException {

		if (listener != null && this.listener != null) {
			throw new DownStreamException("Support only one listener");
		}
		// lintner chain
		this.listener = listener;
		proxy.addTradeListener(this);
		return this.downStreamSender;
	}


	private long genSerialId() {
		AtomicLong id = new AtomicLong();
		return id.getAndIncrement();
	}

	public void onError(String message) {
		log.error("Response Error:" + message);
	}

	@Override
	public void onConnectReady(boolean isReady) {
		// set proxy ready to use
		// notify upStream
		proxy.setReady(isReady);
		this.listener.onState(isReady);
		//TODO: output details
		log.info("Position Record: " + positionRecord);
	}

	/**
	 * Notify Order Status except TradeStatus
	 */
	@Override
	public void onOrder(CThostFtdcOrderField update) {
		log.debug("onOrder: " + update);
		String clOrderId = TraderHelper.genClOrderId(update.FrontID(), update.SessionID(), update.OrderRef().getCString());
		byte statusCode = update.OrderStatus();
		int volumeTraded = update.VolumeTraded();
		String msg = TraderHelper.toGBKString(update.StatusMsg().getBytes());
		OrdStatus status = TraderHelper.convert2OrdStatus(statusCode);
		if (null == status) {
			log.debug("skipping update for status: " + statusCode);
			return;
		}
		ExecType execType = TraderHelper.OrdStatus2ExecType(status);
		log.info("onOrder: " + clOrderId + " Type: " + status + " Volume: " + volumeTraded + " Message: " + msg );

		ChildOrder order = serialToOrder.get(clOrderId);
		if ( null == order ) {
			log.info("Order not found: " + clOrderId);
			return;
		}

		// store exchange serial ID
		String exchangeId = update.ExchangeID().getCString();
		String orderSysId =  update.OrderSysID().getCString();
		exchangeId = exchangeId.trim();
		orderSysId = orderSysId.trim();
		if ( !isEmptyString(exchangeId) && !isEmptyString(orderSysId) ) {
			String exchangeOrderId = TraderHelper.genExchangeOrderId(exchangeId, orderSysId);
			exchangeSerial2Serial.put(exchangeOrderId, clOrderId);
			log.info("Store exchange order id:" + exchangeOrderId);
		}

		OrdStatus oldStatus = order.getOrdStatus();
		order.setOrdStatus(status);
		Double tradeRecord = tradePendings.get(clOrderId);
		if (null != tradeRecord) {
			if (PriceUtils.GreaterThan(volumeTraded, tradeRecord)) {
				log.debug("Trade record total update: " + tradeRecord + " -> " + volumeTraded);
				tradePendings.put(clOrderId, new Double(volumeTraded)); // leave trade to do the update
			}
		} else {
			if (PriceUtils.GreaterThan(volumeTraded, order.getCumQty())) {
				log.debug("Trade record added: " + volumeTraded);
				tradePendings.put(clOrderId, new Double(volumeTraded)); // leave trade to do the update
			} else if (status != OrdStatus.PARTIALLY_FILLED &&
					oldStatus == status) {
				log.debug("Skipping update since ordStatus doesn't change: " + status);
				return;
			} else {
				this.listener.onOrder(execType, order.clone(), null, msg);
				// return remaining position holding
				returnRemainingPosition(order, update.CombOffsetFlag().getByte());
			}
		}
	}

	private boolean isEmptyString(String str) {
		return str == null ? true : "".equals(str);
	}

	private void returnRemainingPosition(ChildOrder order, byte flag) {
		if (order.getOrdStatus() == OrdStatus.CANCELED || order.getOrdStatus() == OrdStatus.REJECTED) {
			double remaining = order.getQuantity() - order.getCumQty();
			if (!PriceUtils.isZero(remaining)) {
				positionRecord.releaseQuantity(convertDownSymbol(order.getSymbol()),
					order.getSide().isBuy(),
					flag, remaining);
			}
		} else {
			log.debug("After response: " + positionRecord);
		}
	}

	/**
	 * +
	 * Notify Trade
	 */
	@Override
	public void onTrade(CThostFtdcTradeField trade) {
		log.info("Traded: " + trade);
		String exchangeId = trade.ExchangeID().getCString();
		String orderSysId =  trade.OrderSysID().getCString();
		exchangeId = exchangeId.trim();
		orderSysId = orderSysId.trim();
		String exchangeOrderId = TraderHelper.genExchangeOrderId(exchangeId, orderSysId);
		String clOrderId = exchangeSerial2Serial.get(exchangeOrderId);
		if ( null == clOrderId ) {
			log.info("Order not found： " + exchangeOrderId);
			return;
		}
		ChildOrder order = serialToOrder.get(clOrderId);
		if ( null == order ) {
			log.info("Order not found: " + clOrderId);
			return;
		}

		Double tradeRecord = tradePendings.get(clOrderId);
		if (null == tradeRecord) {
			log.error("Received trade without order update: " + clOrderId);
		}

		if (trade.Volume() == 0 || PriceUtils.isZero(trade.Price())) {
			log.error("volume or price is 0: " + clOrderId);
			return;
		}

		double avgPx = (order.getAvgPx() * order.getCumQty() + trade.Volume() * trade.Price()) / (order.getCumQty() + trade.Volume());
		double volume = order.getCumQty() + trade.Volume();
		order.setCumQty(volume);
		order.setAvgPx(avgPx);
		ExecType execType = ExecType.PARTIALLY_FILLED;
		if (order.getOrdStatus() == OrdStatus.CANCELED || order.getOrdStatus() == OrdStatus.REJECTED) {
			execType = TraderHelper.OrdStatus2ExecType(order.getOrdStatus());
		} else if (PriceUtils.EqualGreaterThan(order.getCumQty(), order.getQuantity())) {
			execType = ExecType.FILLED;
			order.setOrdStatus(OrdStatus.FILLED);
		} else {
			order.setOrdStatus(OrdStatus.PARTIALLY_FILLED);
		}

		Execution execution;

		Date tradeDate = TimeUtil.getOnlyDate(Calendar.getInstance().getTime());

		try {
			execution = new com.cyanspring.common.business.Execution(
			        order.getSymbol(), order.getSide(), trade.Volume(),
			        trade.Price(), order.getId(), order.getParentOrderId(),
			        order.getStrategyId(), IdGenerator.getInstance()
			        .getNextID() + "E", order.getUser(),
			        order.getAccount(), order.getRoute(), tradeDate);
		} catch (OrderException e) {
			log.error(e.getMessage(), e);
			return;
		}

		// update position holding
		positionRecord.onTradeUpdate(trade.InstrumentID().getCString(),
				trade.Direction() == TraderLibrary.THOST_FTDC_D_Buy,
				order.get(Byte.class, OrderField.FLAG.value()),
				trade.Volume());

		ChildOrder update = order.clone();

		if (null != tradeRecord) {
			if (PriceUtils.GreaterThan(tradeRecord, order.getCumQty())) { // not yet received all trades
				execType = ExecType.PARTIALLY_FILLED;
				update.setOrdStatus(OrdStatus.PARTIALLY_FILLED);
			} else {
				tradePendings.remove(clOrderId);
			}
		}

		returnRemainingPosition(update, update.get(Byte.class, OrderField.FLAG.value()));
		this.listener.onOrder(execType, update, execution, null);
	}

	@Override
	public void onCancel(String orderId, String msg) {
		log.info("Response cancel On Order:" + orderId + " " + msg);
		ChildOrder order = serialToOrder.get(orderId);
		if ( null == order ) {
			log.info("Order not found: " + orderId);
			return;
		}
		order.setOrdStatus(OrdStatus.CANCELED);

		// return position holding
		returnRemainingPosition(order, order.get(Byte.class, OrderField.FLAG.value()));

		this.listener.onOrder(ExecType.CANCELED, order.clone(), null, msg);
	}

	@Override
	public void onError(String orderId, String msg) {
		log.info("Response Error On Order:" + orderId + " " + msg);
		ChildOrder order = serialToOrder.get(orderId);
		if ( null == order ) {
			log.info("Order not found: " + orderId);
			return;
		}
		order.setOrdStatus(OrdStatus.REJECTED);

		// return position holding
		returnRemainingPosition(order, order.get(Byte.class, OrderField.FLAG.value()));

		this.listener.onOrder(ExecType.REJECTED, order.clone(), null, msg);
	}

	@Override
	public void onQryPosition(CThostFtdcInvestorPositionField field,
			boolean isLast) {
		if ( field == null ) {
			log.info("CThostFtdcInvestorPositionField is null");
			positionRecord.clear();
			return;
		}
		String symbol = field.InstrumentID().getCString();
		double today = field.TodayPosition();
		double yesterday = field.YdPosition();
		if (field.YdPosition() > 0) {
			yesterday -= field.CloseVolume();
		}

		boolean isBuy = false;
		if (field.PosiDirection() == TraderLibrary.THOST_FTDC_PD_Long) {
			isBuy = true;
		}
		else if (field.PosiDirection() == TraderLibrary.THOST_FTDC_PD_Short) {
			isBuy = false;
		} else {
			log.error("This flag can't be handled: " + field.PosiDirection());
			return;
		}

		CtpPosition pos = new CtpPosition(symbol, isBuy, today, yesterday, field.OpenVolume());
		positionRecord.inject(pos, isLast);
	}

	@Override
	public void onEvent(AsyncEvent event) {
		if (event == queryPositionEvent) {
			if (getState() && (!positionQueried || throttler.check())) {
				positionQueried = true;
				proxy.doQryPosition();
			}
		}
	}

}
