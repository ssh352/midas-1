package com.cyanspring.adaptor.ib;

import com.cyanspring.common.business.ChildOrder;
import com.cyanspring.common.business.OrderException;
import com.cyanspring.common.business.OrderField;
import com.cyanspring.common.business.RefDataField;
import com.cyanspring.common.downstream.DownStreamException;
import com.cyanspring.common.downstream.IDownStreamConnection;
import com.cyanspring.common.downstream.IDownStreamListener;
import com.cyanspring.common.downstream.IDownStreamSender;
import com.cyanspring.common.event.AsyncEvent;
import com.cyanspring.common.event.IAsyncEventListener;
import com.cyanspring.common.event.IRemoteEventManager;
import com.cyanspring.common.event.refdata.RefDataEvent;
import com.cyanspring.common.event.refdata.RefDataRequestEvent;
import com.cyanspring.common.marketdata.*;
import com.cyanspring.common.staticdata.RefData;
import com.cyanspring.common.stream.IStreamAdaptor;
import com.cyanspring.common.type.*;
import com.cyanspring.common.util.DualMap;
import com.cyanspring.common.util.IdGenerator;
import com.cyanspring.common.util.PriceUtils;
import com.cyanspring.common.util.TimeUtil;
import com.ib.client.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IbAdaptor implements EWrapper, IMarketDataAdaptor,
        IStreamAdaptor<IDownStreamConnection>, IAsyncEventListener {

    private static final Logger log = LoggerFactory.getLogger(IbAdaptor.class);
    private static final Logger newsLog = LoggerFactory
            .getLogger(IbAdaptor.class.getName() + ".NewsLog");

    @Autowired
    private IRemoteEventManager eventManager;

    // connection parameters
    private String host;
    private int port = 7496;
    private int clientId = 100;
    // ids
    protected AtomicInteger nextOrderId = new AtomicInteger();

    // market data parameters
    private boolean reqMarketDepth = true;
    private String genericTickTags = "100,101,104,105,106,107,165,221,225,233,236,258,293,294,295,318";
    private int depthRows = 20;
    private boolean logMarketData = true;
    private List<IMarketDataStateListener> marketDataStateListeners = new ArrayList<IMarketDataStateListener>();

    protected EClientSocket clientSocket;
    private Map<String, List<IMarketDataListener>> subs = Collections
            .synchronizedMap(new HashMap<String, List<IMarketDataListener>>());

    private ConcurrentHashMap<String, RefData> refDataBySymbolMap = new ConcurrentHashMap<String, RefData>();

    // Down Stream
    private DownStreamConnection downStreamConnection = new DownStreamConnection();
    private DownStreamSender downStreamSender = new DownStreamSender();
    private IDownStreamListener downStreamListener;
    private DualMap<Integer, String> idToChildId = new DualMap<Integer, String>();
    private Map<Integer, ChildOrder> idToOrder = Collections
            .synchronizedMap(new HashMap<Integer, ChildOrder>());
    private boolean qtyHasChanged;
    private boolean priceHasChanged;
    private volatile boolean isConnected = false;
    private String id = "IB";
    private boolean initialised;
    private boolean newsIsSubCurrentDay = false;
    private volatile boolean reqDataReceived = false;
    private volatile boolean marketSubscribed = false;
    private volatile boolean prevStatus = false;
    private String refDataEventKey;

    // caching
    DualMap<String, Integer> symbolToId = new DualMap<String, Integer>();
    Map<String, Quote> quotes = Collections
            .synchronizedMap(new HashMap<String, Quote>());

    // gc thread
    Thread gcThread;

    // cancel open orders
    private boolean cancelOpenOrders = false;
    private Set<Integer> openOrderIds = new LinkedHashSet<Integer>();
    private boolean cancellingOpenOrders;
    private long lastTimeSend;
    private long checkLastTimeInterval = 0;
    private boolean reconnectOn = true;

    public IbAdaptor() {
        clientSocket = new EClientSocket(this);
    }

    @Override
    synchronized public void init() throws Exception {
        if (!initialised) {
            refDataEventKey = IdGenerator.getInstance().getNextID();
            log.debug(id + " event key = " + refDataEventKey);
            eventManager.subscribe(RefDataEvent.class, refDataEventKey, this);
            gcThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (clientSocket.isConnected() == false) {
                            isConnected =false;
                            notifyAdaptorState(false);
                            marketSubscribed = false;
                            if (reqDataReceived){
                                ConnectToIBGateway();
                            }else{
                                requestRequireData();
                            }
                        } else {
                            if (marketSubscribed && checkLastTimeInterval != 0
                                    && ((System.currentTimeMillis() - lastTimeSend) > checkLastTimeInterval)) {
                                if (reconnectOn) {
                                    log.warn("IB Gateway too long not tick,reconnect!" + id);
                                    clientSocket.eDisconnect();
                                    isConnected = false;
                                    notifyAdaptorState(false);
                                }else{
                                    log.error("IB Gateway too long not tick,reconnect!" + id);
                                }
                                //wait disconnect
                                try {
                                    TimeUnit.SECONDS.sleep(10);
                                } catch (InterruptedException e) {}
                                continue;
                            }
                        }
                        gcChildOrders();
                        try {
                            TimeUnit.SECONDS.sleep(10);
                        } catch (InterruptedException e) {
                        }
                    }
                }

            });
            gcThread.setName("IbInitThread-" + id);
            gcThread.start();

            if (cancelOpenOrders) {
                cancellingOpenOrders = true;
                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        log.info(id + " Requesting open orders");

                        while(!clientSocket.isConnected()) {
                        	try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
							}
                        }
                        clientSocket.reqOpenOrders();
                    }

                });
                thread.start();
            }

            initialised = true;
        }
    }

    private void requestRequireData(){
        RefDataRequestEvent event = new RefDataRequestEvent(refDataEventKey, null);
        eventManager.sendEvent(event);
    }

    private void ConnectToIBGateway() {
        if (clientSocket.isConnected()) {
			return;
		}
        log.info("Attempting to establish connection to IB TWS/Gateway..." + id);
        clear();
        clientSocket.eDisconnect();
        clientSocket.eConnect(host, port, clientId);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        }
        if (clientSocket.isConnected()){
            lastTimeSend = System.currentTimeMillis();
            log.info(id + " connected");
        }
    }

    @Override
    synchronized public void uninit() {
        log.info("Ib Adapter uninit begin");
        eventManager.unsubscribe(RefDataEvent.class, refDataEventKey, this);
        gcThread.interrupt();
        clientSocket.eDisconnect();
        prevStatus = false;
        isConnected = false;
        log.info("Ib Adapter uninit end");
    }

    private void gcChildOrders() {
        synchronized (idToChildId) {
            ArrayList<Integer> toBeRemoved = new ArrayList<Integer>();
            for (Entry<Integer, ChildOrder> entry : idToOrder.entrySet()) {
                ChildOrder order = entry.getValue();
                if (order.getOrdStatus().isCompleted()
                        && TimeUtil.getTimePass(order.getModified()) > 20000) {
					toBeRemoved.add(entry.getKey());
				}
            }
            for (Integer id : toBeRemoved) {
                idToChildId.remove(id);
                ChildOrder order = idToOrder.remove(id);
                log.debug("GC: " + order);
            }
        }
    }

    private void notifyAdaptorState(boolean on){
        log.info(id + " notifyState, prev=" + prevStatus +",now=" + on);
        if (prevStatus == on && !prevStatus){
            return;
        }else{
            prevStatus = on;
        }
        if (marketDataStateListeners != null && marketDataStateListeners.size() > 0){
            log.info(id + " sendState to marketDataListener");
            for (IMarketDataStateListener listener : marketDataStateListeners) {
                listener.onState(on, this);
            }
        }
        if (downStreamListener != null){
            log.info(id + " sendState to downStreamListener");
            downStreamListener.onState(on);
        }
    }

    // //////////////////////////////////////////////
    // Begin: implementation of IMarketDataAdaptor
    // //////////////////////////////////////////////

    @Override
    public boolean getState() {
        if (!clientSocket.isConnected()) {
			return false;
		}
        return isConnected;
    }

    @Override
    public void subscribeMarketDataState(IMarketDataStateListener listener) {
        if (!marketDataStateListeners.contains(listener)) {
            marketDataStateListeners.add(listener);
            listener.onState(getState(), this);
        }
    }

    @Override
    public void unsubscribeMarketDataState(IMarketDataStateListener listener) {
        marketDataStateListeners.remove(listener);
    }

    @Override
    public void subscribeMarketData(String instrument,
                                    IMarketDataListener listener) throws MarketDataException {
        log.info("subscribeMarketData: " + instrument);
        RefData refData = refDataBySymbolMap.get(instrument);
        if (refData == null) {
            throw new MarketDataException("Symbol " + instrument
                    + " is not found in reference data");
        }
        Contract contract = refData.get(Contract.class,
                RefDataField.CONTRACT.value());
        if (null == contract) {
            throw new MarketDataException("Symbol " + instrument
                    + " contract is not found in reference data");
        }

        List<IMarketDataListener> list = subs.get(instrument);
        if (list == null) {
            list = Collections
                    .synchronizedList(new ArrayList<IMarketDataListener>());
            subs.put(instrument, list);
        }

        if (!list.contains(listener)) {
            list.add(listener);
        }

        if (symbolToId.containsKey(refData.getSymbol())) {
            return;
        }

        Integer reqId = nextOrderId.getAndIncrement();
        symbolToId.put(instrument, reqId);
        subscribeMarketData(refData, instrument, reqId, contract,
                genericTickTags);
    }

    void subscribeMarketData(RefData refData, String instrument, int reqId,
                             Contract contract, String genericTickTags) {
        clientSocket.reqMktData(reqId, contract, genericTickTags, false);
        log.info("subscribeMarketData: " + instrument + "," + reqId);
        if (reqMarketDepth) {
            clientSocket.reqMktDepth(reqId, contract, depthRows);
            log.info("subscribeMarketDepth: " + refData.getSymbol() + ","
                    + reqId);
        }
    }

    @Override
    public void unsubscribeMarketData(String instrument,
                                      IMarketDataListener listener) {
        List<IMarketDataListener> list = subs.get(instrument);
        if (list != null) {
            list.remove(listener);
            if (list.size() == 0) { // no more listeners on this stock, cancel
                // subscription
                if (!symbolToId.containsKey(instrument)) {
					return;
				}
                Integer reqId = symbolToId.get(instrument);
                clientSocket.cancelMktData(reqId);
                if (reqMarketDepth) {
                    clientSocket.cancelMktDepth(reqId);
                }
            }
        }
    }

    @Override
    public void subscribeMultiMarketData(List<String> subscribeList, IMarketDataListener listener) throws MarketDataException {
        for (String symbol: subscribeList){
            subscribeMarketData(symbol, listener);
        }
        if (subscribeList != null && subscribeList.size() > 0){
            marketSubscribed = true;
        }
    }

    @Override
    public void unsubscribeMultiMarketData(List<String> unSubscribeList, IMarketDataListener listener) {
        for (String symbol: unSubscribeList){
            unsubscribeMarketData(symbol, listener);
        }
    }

    @Override
    public void onEvent(AsyncEvent event) {
        if (!reqDataReceived) {
            if (event instanceof RefDataEvent) {
                log.debug("Ib Adapter Receive RefDataEvent - " + id + "-" + event.getKey());
                RefDataEvent refDataEvent = (RefDataEvent) event;
                if (refDataEvent != null && refDataEvent.isOk() && refDataEvent.getRefDataList().size() > 0) {
                    for (RefData refData : refDataEvent.getRefDataList()) {
                        refDataBySymbolMap.put(refData.getSymbol(), refData);
                    }
                    reqDataReceived = true;
                }else{
                    log.warn(id + "-RefDataEvent is empty or null!");
                }
            }
        }
    }

    // //////////////////////////////////////////////
    // End: implementation of IMarketDataAdaptor
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // Begin: implementation of IStreamAdaptor
    // //////////////////////////////////////////////

    class DownStreamSender implements IDownStreamSender {

        String getSide(OrderSide side) throws DownStreamException {
            if (side.equals(OrderSide.Buy)) {
				return "BUY";
			} else if (side.equals(OrderSide.Sell)) {
				return "SELL";
			} else if (side.equals(OrderSide.SS)) {
				return "SSHORT";
			} else {
				throw new DownStreamException(side + " is not supported");
			}
        }

        String getType(ExchangeOrderType type) throws DownStreamException {
            if (type.equals(ExchangeOrderType.LIMIT)) {
				return "LMT";
			} else if (type.equals(ExchangeOrderType.MARKET)) {
				return "MKT";
			} else {
				throw new DownStreamException(type + " is not supported");
			}
        }

        @Override
        public boolean getState() {
            if (!clientSocket.isConnected()) {
				return false;
			}
            return isConnected;
        }

        @Override
        synchronized public void newOrder(ChildOrder order) throws DownStreamException {
            log.debug("Sending new order: " + order);
            RefData refData = refDataBySymbolMap.get(order.getSymbol());
            Contract contract = refData.get(Contract.class,
                    RefDataField.CONTRACT.value());
            if (refData == null || null == contract) {
                throw new DownStreamException("Symbol " + order.getSymbol()
                        + " or contract is not found in reference data");
            }
            Order ibOrder = new Order();
            ibOrder.m_action = getSide(order.getSide());
            ibOrder.m_totalQuantity = (int) order.getQuantity();
            ibOrder.m_orderType = getType(order.getType());
            ibOrder.m_lmtPrice = order.getPrice();
            int orderId = nextOrderId.getAndIncrement();
			synchronized (idToChildId) {
                idToChildId.put(orderId, order.getId());
                idToOrder.put(orderId, order);
            }
            order.setOrdStatus(OrdStatus.PENDING_NEW);
            order.put(OrderField.CLORDERID.value(), "" + orderId);
            clientSocket.placeOrder(orderId, contract, ibOrder);
            log.debug("New order sent: " + orderId);
        }

        @Override
        public void amendOrder(ChildOrder order, Map<String, Object> fields)
                throws DownStreamException {
            log.debug("Amending order: " + order);
            RefData refData = refDataBySymbolMap.get(order.getSymbol());
            Contract contract = refData.get(Contract.class,
                    RefDataField.CONTRACT.value());
            if (refData == null || null == contract) {
                throw new DownStreamException("Symbol " + order.getSymbol()
                        + " is not found in reference data");
            }

            Integer orderId;
            ChildOrder myOrder;
            synchronized (idToChildId) {
                orderId = idToChildId.getKeyByValue(order.getId());
                myOrder = idToOrder.get(orderId);
            }

            if (null == orderId) {
                order.setOrdStatus(autoStatus(order));
                downStreamListener.onOrder(ExecType.REJECTED, order, null,
                        "Unable to find this child order in adaptor");
                return;
            }

            // try to find the original child order, we do not want
            // to use the copy sent in
            if (null == myOrder || !order.getId().equals(myOrder.getId())) {
                throw new DownStreamException(
                        "Unable to find this child order with matching id in adaptor: "
                                + myOrder);
            }

            log.debug("Local order: " + order);
            if (myOrder.getOrdStatus().isCompleted()) {
                downStreamListener.onOrder(ExecType.REJECTED, myOrder, null,
                        "Order in completed state");
                return;
            }

            // if (pendingAmends.containsKey(orderId)) {
            // downStreamListener.onOrder(ExecType.REJECTED, myOrder, null,
            // "Order has pending amend record");
            // return;
            // }

            Order ibOrder = new Order();
            ibOrder.m_action = getSide(myOrder.getSide());
            ibOrder.m_orderType = getType(myOrder.getType());

            Double qtyChange = (Double) fields.get(OrderField.QUANTITY.value());
            if (qtyChange != null) {
				ibOrder.m_totalQuantity = qtyChange.intValue();
			} else {
				ibOrder.m_totalQuantity = (int) myOrder.getQuantity();
			}

            Double priceChange = (Double) fields.get(OrderField.PRICE.value());

            if (priceChange != null) {
				ibOrder.m_lmtPrice = priceChange;
			} else {
				ibOrder.m_lmtPrice = myOrder.getPrice();
			}
            myOrder.setOrdStatus(OrdStatus.PENDING_REPLACE);
            // pendingAmends.put(orderId, fields);
            clientSocket.placeOrder(orderId, contract, ibOrder);
        }

        @Override
        public void cancelOrder(ChildOrder order) throws DownStreamException {
            log.debug("Canceling order: " + order);
            Integer orderId;
            ChildOrder myOrder;
            synchronized (idToChildId) {
                orderId = idToChildId.getKeyByValue(order.getId());
                myOrder = idToOrder.get(orderId);
            }

            if (null == orderId) {
                order.setOrdStatus(autoStatus(order));
                downStreamListener.onOrder(ExecType.REJECTED, order, null,
                        "Unable to find this child order in adaptor");
                return;
            }

            // try to find the original child order, we do not want
            // to use the copy sent in
            if (null == myOrder || !order.getId().equals(myOrder.getId())) {
                throw new DownStreamException(
                        "Unable to find this child order with matching id in adaptor: "
                                + myOrder);
            }
            log.debug("Local order: " + order);
            if (myOrder.getOrdStatus().isCompleted()) {
                downStreamListener.onOrder(ExecType.REJECTED, myOrder, null,
                        "Order in completed state");
                return;
            }

            myOrder.setOrdStatus(OrdStatus.PENDING_CANCEL);
            clientSocket.cancelOrder(orderId);
        }

    }

    class DownStreamConnection implements IDownStreamConnection {
        DownStreamSender downStreamSender = new DownStreamSender();

        @Override
        public void init() {
        }

        @Override
        public void uninit() {
        }

        @Override
        public String getId() {
            return IbAdaptor.this.getId();
        }

        @Override
        public boolean getState() {
            if (!clientSocket.isConnected()) {
				return false;
			}
            return isConnected;
        }

        @Override
        public IDownStreamSender setListener(IDownStreamListener listener) {
            IbAdaptor.this.downStreamListener = listener;
            if (null != listener) {
				downStreamListener.onState(getState());
			}
            return IbAdaptor.this.downStreamSender;
        }

    }

    @Override
    public List<IDownStreamConnection> getConnections() {
        ArrayList<IDownStreamConnection> result = new ArrayList<IDownStreamConnection>();
        result.add(downStreamConnection);
        return result;
    }

    // //////////////////////////////////////////////
    // End: implementation of IStreamAdaptor
    // //////////////////////////////////////////////

    // ////////////////////////////////////
    // Begin: implementation of EWrapper
    // ////////////////////////////////////

    @Override
    public void error(Exception e) {
        log.error("IB-ERR: " + e.getMessage(), e);

    }

    @Override
    public void error(String str) {
        log.error("IB-ERR: " + EWrapperMsgGenerator.error(str));

    }

    @Override
    synchronized public void error(int id, int errorCode, String errorMsg) {
        log.info("IB-ERR: "
                + EWrapperMsgGenerator.error(id, errorCode, errorMsg));

        ChildOrder order = idToOrder.get(id);
        if (null != order) {
        	if (errorCode == 202) {
        		if (!order.getOrdStatus().isCompleted()) {
	        		order.setOrdStatus(OrdStatus.CANCELED);
	                downStreamListener.onOrder(ExecType.CANCELED, order, null,
	                        "IB error: " + errorCode + ", " + errorMsg);
        		} else {
        			log.debug("Ignore error code: " + order);
        		}
        	} else if ((order.getOrdStatus().equals(OrdStatus.PENDING_NEW) ||
        		order.getOrdStatus().equals(OrdStatus.PENDING_REPLACE))&&
                (errorCode == 399 && errorMsg.contains("Warning"))) {
                ExecType execType = ExecType.pendingToReady(order.getOrdStatus());
                order.setOrdStatus(autoStatus(order));
                downStreamListener.onOrder(execType, order, null, "");
            } else {
                order.setOrdStatus(OrdStatus.REJECTED);
                downStreamListener.onOrder(ExecType.REJECTED, order, null,
                    "IB error: " + errorCode + ", " + errorMsg);
            }
        }
        if (id == -1) {
            if (errorCode == 1100) {
                // Connectivity between IB and TWS has been lost.
            } else if (errorCode == 1101) {
                // Connectivity between IB and TWS has been restoreddata lost.*
                // *Market and account data subscription requests must be
                // resubmitted
            } else if (errorCode == 1102) {
                // Connectivity between IB and TWS has been restoreddata
                // maintained.
            } else if (errorCode == 1300) {
                // TWS socket port has been reset and this connection is being
                // dropped. Please reconnect on the new port - <port_num>
            }
        }
    }

    @Override
    public void connectionClosed() {
        log.info("IB connection closed");
        clear();
        isConnected = false;
        notifyAdaptorState(isConnected);
    }

    private void clear() {
        quotes.clear();
        symbolToId.clear();
    }

    synchronized private void publishTrade(Trade trade) {
        List<IMarketDataListener> list = subs.get(trade.getSymbol());
        if (null != list) {
			for (IMarketDataListener listener : list) {
				listener.onTrade(trade);
			}
		}
    }

    synchronized private void publishQuote(Quote quote) {
//        if (!checkQuote(quote)) return;
        lastTimeSend = System.currentTimeMillis();
        quote = (Quote) quote.clone();
        quote.sourceId = 1;
        quote.setTimeStamp(new Date());
        List<IMarketDataListener> list = subs.get(quote.getSymbol());
        if (null != list)
		 {
			for (IMarketDataListener listener : list)
			 {
				listener.onQuote(new InnerQuote(QuoteSource.IB, quote)); // use in MDM proceessInnerQuoteEvent , IB Adapter sourceid=1
			}
		}

    }

    private boolean checkQuote(Quote quote){
        boolean isCheck = true;
        if (PriceUtils.EqualLessThan(quote.getBid(),0) || PriceUtils.EqualLessThan(quote.getAsk(),0)){
            isCheck = false;
        }
        return isCheck;
    }

    /**
     * IB tickPrice field ref : https://www.interactivebrokers.com/en/software/api/apiguide/tables/tick_types.htm
     */
    @Override
    public void tickPrice(int tickerId, int field, double price,
                          int canAutoExecute) {
        if (logMarketData) {
			log.info(EWrapperMsgGenerator.tickPrice(tickerId, field, price,
                    canAutoExecute));
		}
        String symbol = symbolToId.getKeyByValue(tickerId);
        if (null == symbol) {
            log.error("tickPrice: " + "can't find id in symbolToId map: "
                    + tickerId);
            return;
        }
        Quote quote = quotes.get(symbol);
        if (null == quote) {
            quote = new Quote(symbol, new LinkedList<QtyPrice>(),
                    new LinkedList<QtyPrice>());
            quotes.put(symbol, quote);
        }
        switch (field) {
            case 1:
                quote.setBid(price);
                // publishQuote(quote);
                break;
            case 2:
                quote.setAsk(price);
                // publishQuote(quote);
                break;

            case 4:
                quote.setLast(price);
                // publishQuote(quote);
                break;
            case 6:
                quote.setHigh(price);

                if (quote.getOpen() == 0) {
                    if (PriceUtils.Equal(quote.getHigh(), quote.getLow())) {
                        quote.setOpen(price);
                        log.debug("Symbol=" + quote.getSymbol() + ",OpenPrice=" + price);
                    }
                }

                publishQuote(quote);
                break;

            case 7:
                quote.setLow(price);
                if (quote.getOpen() == 0) {
                    if (PriceUtils.Equal(quote.getHigh(), quote.getLow())) {
                        quote.setOpen(price);
                        log.debug("Symbol=" + quote.getSymbol() + ",OpenPrice=" + price);
                    }
                }
                publishQuote(quote);
                break;

            case 9:
                if (price != 0) {
                    quote.setClose(price);
                }
                if (quote.getBid() == -1 && quote.getAsk() == -1) {
                    quote.setOpen(0);
                    log.debug("Symbol=" + quote.getSymbol() + ",OpenPrice=" + price + " Trade Close");
                }

                publishQuote(quote);
                break;

            case 14:
                quote.setOpen(price);
                publishQuote(quote);
                break;

            default:
                if (logMarketData) {
					log.trace("tickPrice: " + symbol + " undefined field type: "
                            + field + ", value: " + price);
				}
        }
    }

    @Override
    public void tickSize(int tickerId, int field, int size) {
        if (logMarketData) {
			log.info(EWrapperMsgGenerator.tickSize(tickerId, field, size));
		}
        String symbol = symbolToId.getKeyByValue(tickerId);
        if (null == symbol) {
            log.error("tickSize: " + "can't find id in symbolToId map: "
                    + tickerId);
            return;
        }
        Quote quote = quotes.get(symbol);
        if (null == quote) {
            quote = new Quote(symbol, new LinkedList<QtyPrice>(),
                    new LinkedList<QtyPrice>());
            quotes.put(symbol, quote);
        }
        switch (field) {
            case 0:
                quote.setBidVol(size);
                publishQuote(quote);
                break;
            case 3:
                quote.setAskVol(size);
                publishQuote(quote);
                break;
            case 5:
                quote.setLastVol(size);
                // publishQuote(quote);
                break;
            case 8:
                Trade trade = new Trade();
                trade.setSymbol(quote.getSymbol());
                trade.setPrice(quote.getLast());
                trade.setQuantity(quote.getLastVol());
                trade.setId(IdGenerator.getInstance().getNextID() + "T");
                publishTrade(trade);

                quote.setTotalVolume(size);
                publishQuote(quote);
                break;
            default:
                if (logMarketData) {
					log.trace("tickSize: " + symbol + " undefined field type: " + field
                            + ", value: " + size);
				}
        }
    }

    @Override
    public void updateMktDepth(int tickerId, int position, int operation,
                               int side, double price, int size) {
        log.debug(EWrapperMsgGenerator.updateMktDepth(tickerId, position,
                operation, side, price, size));

        String symbol = symbolToId.getKeyByValue(tickerId);
        if (null == symbol) {
            log.error("updateMktDepth: " + "can't find id in symbolToId map: "
                    + tickerId);
            return;
        }
        Quote quote = quotes.get(symbol);
        if (null == quote) {
            quote = new Quote(symbol, new LinkedList<QtyPrice>(),
                    new LinkedList<QtyPrice>());
            quotes.put(symbol, quote);
        }
        List<QtyPrice> list = side == 0 ? quote.getAsks() : quote.getBids();
        switch (operation) {
            case 0:
                if (position < list.size()) {
                    list.add(position, new QtyPrice(size, price));
                } else {
                    int inc = list.size() - position;
                    for (int i = 0; i < inc; i++) {
						list.add(new QtyPrice(0, 0));
					}
                    list.add(new QtyPrice(size, price));
                }
                publishQuote(quote);
                break;
            case 1:
                if (position < list.size()) {
                    list.set(position, new QtyPrice(size, price));
                } else {
                    log.warn("updateMktDepth: update row not found " + position);
                }
                publishQuote(quote);
                break;
            case 2:
                if (position < list.size()) {
                    list.remove(position);
                } else {
                    // just to remove the last item at the list to get around a IB
                    // bug
                    if (list.size() > 0) {
						list.remove(list.size() - 1);
					}
                    log.debug("updateMktDepth: delete row not found " + position);
                }
                publishQuote(quote);
                break;
            default:
                log.warn("updateMktDepth: unknown operation " + operation);
        }
    }

    @Override
    public void tickOptionComputation(int tickerId, int field,
                                      double impliedVol, double delta, double optPrice,
                                      double pvDividend, double gamma, double vega, double theta,
                                      double undPrice) {
        if (logMarketData) {
			log.info(EWrapperMsgGenerator.tickOptionComputation(tickerId,
                    field, impliedVol, delta, optPrice, pvDividend, gamma,
                    vega, theta, undPrice));
		}

    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {
        if (logMarketData) {
			log.info(EWrapperMsgGenerator
                    .tickGeneric(tickerId, tickType, value));
		}
    }

    @Override
    public void tickString(int tickerId, int tickType, String value) {
        if (logMarketData) {
			log.info(EWrapperMsgGenerator.tickString(tickerId, tickType, value));
		}
    }

    @Override
    public void tickEFP(int tickerId, int tickType, double basisPoints,
                        String formattedBasisPoints, double impliedFuture, int holdDays,
                        String futureExpiry, double dividendImpact, double dividendsToExpiry) {
        if (logMarketData) {
			log.info(EWrapperMsgGenerator.tickEFP(tickerId, tickType,
                    basisPoints, formattedBasisPoints, impliedFuture, holdDays,
                    futureExpiry, dividendImpact, dividendsToExpiry));
		}
    }

    @Override
    public void tickSnapshotEnd(int reqId) {
        if (logMarketData) {
			log.info(EWrapperMsgGenerator.tickSnapshotEnd(reqId));
		}
    }

    public OrdStatus autoStatus(ChildOrder order) {
        if (PriceUtils.isZero(order.getQuantity())) {
            return OrdStatus.REJECTED;
        }

        if (PriceUtils.EqualLessThan(order.getQuantity(), order.getCumQty())) {
			return OrdStatus.FILLED;
		}

        if (PriceUtils.EqualGreaterThan(0, order.getCumQty())) {
			return OrdStatus.NEW;
		}

        return OrdStatus.PARTIALLY_FILLED;
    }

    @Override
    synchronized public void orderStatus(int orderId, String status,
                                         int filled, int remaining, double avgFillPrice, int permId,
                                         int parentId, double lastFillPrice, int clientId, String whyHeld) {
        log.info(EWrapperMsgGenerator.orderStatus(orderId, status, filled,
                remaining, avgFillPrice, permId, parentId, lastFillPrice,
                clientId, whyHeld));

        ChildOrder order = idToOrder.get(orderId);
        if (null == order) {
            log.warn("orderStatus: unable to find this order in cache: "
                    + EWrapperMsgGenerator.orderStatus(orderId, status, filled,
                    remaining, avgFillPrice, permId, parentId,
                    lastFillPrice, clientId, whyHeld));
            return;
        }

        if (status.equals("PendingSubmit")) {
            // we dont want to know it is pending submit
            return;
        }

        ExecType execType = ExecType.RESTATED;
        com.cyanspring.common.business.Execution execution = null;
        int oldFilled = (int) order.getCumQty();

        order.setCumQty(filled);
        order.setAvgPx(avgFillPrice);
        order.touch();

        if (oldFilled != filled) {
            int orderQty = (int) order.getQuantity();
            if (order.getOrdStatus().isPending()) {
                execType = ExecType.pendingToReady(order.getOrdStatus());
                if (filled == orderQty) {
                    order.setOrdStatus(OrdStatus.FILLED);
                } else {
                    order.setOrdStatus(OrdStatus.PARTIALLY_FILLED);
                }
            } else if (filled == orderQty) {
                execType = ExecType.FILLED;
                order.setOrdStatus(OrdStatus.FILLED);
            } else {
                execType = ExecType.PARTIALLY_FILLED;
                order.setOrdStatus(OrdStatus.PARTIALLY_FILLED);
            }

            // add defensive logic for a deadly IB bug where lastFillPrice some times
            // can be 0 when there is a fill
            if (PriceUtils.isZero(lastFillPrice)) {
				lastFillPrice = avgFillPrice;
			}

            Date tradeDate = TimeUtil.getOnlyDate(Calendar.getInstance().getTime());

            try {
				execution = new com.cyanspring.common.business.Execution(
				        order.getSymbol(), order.getSide(), filled - oldFilled,
				        lastFillPrice, order.getId(), order.getParentOrderId(),
				        order.getStrategyId(), IdGenerator.getInstance()
				        .getNextID() + "E", order.getUser(),
				        order.getAccount(), order.getRoute(), tradeDate);
			} catch (OrderException e) {
				log.error(e.getMessage(), e);
				return;
			}
            downStreamListener.onOrder(execType, order, execution, "");
        }

        if (priceHasChanged || qtyHasChanged) {
            order.setOrdStatus(autoStatus(order));
            downStreamListener.onOrder(ExecType.REPLACE, order, null, "");
        }

        if ((status.equals("Cancelled") || status.equals("ApiCancelled")) &&
        	(!order.getOrdStatus().isCompleted() || oldFilled != filled)) {
            execType = ExecType.CANCELED;
            order.setOrdStatus(OrdStatus.CANCELED);
            downStreamListener.onOrder(execType, order, null, "");
            return;
        } else if (status.equals("Submitted")) {
            if (order.getOrdStatus().isPending() && order.getOrdStatus() != OrdStatus.PENDING_CANCEL) {
                execType = ExecType.pendingChangeToReady(order.getOrdStatus());
                order.setOrdStatus(OrdStatus.pendingToReady(order
                        .getOrdStatus()));
                downStreamListener.onOrder(execType, order, null, "");
                return;
            }
        }

        log.debug("Not sending update");

    }

    @Override
    synchronized public void openOrder(int orderId, Contract contract,
                                       Order order, OrderState orderState) {
        log.info(EWrapperMsgGenerator.openOrder(orderId, contract, order,
                orderState));
        if (this.cancellingOpenOrders) {
            log.info("Add open order to be cancelled: " + orderId);
            openOrderIds.add(orderId);
            return;
        }

        ChildOrder child = idToOrder.get(orderId);
        if (null == child) {
            log.warn("orderStatus: unable to find this order in cache: "
                    + EWrapperMsgGenerator.openOrder(orderId, contract, order,
                    orderState));
            return;
        }

        // assuming IB will reject through the error method, we don't need this
        // logic for now
        // Map<String, Object> fields = pendingAmends.get(orderId);
        // if (null != fields) {
        // Double qtyChange = (Double)fields.get(OrderField.QUANTITY.value());
        // boolean qtyChangeFailed = (qtyChange != null) &&
        // (order.m_totalQuantity != qtyChange.intValue());
        // Double priceChange = (Double)fields.get(OrderField.PRICE.value());
        // boolean priceChangeFailed = (priceChange != null) &&
        // (order.m_lmtPrice != priceChange.intValue());
        // if (qtyChangeFailed || priceChangeFailed)
        // downStreamListener.onOrder(ExecType.REJECTED, child, null,
        // "Qty/Price change failed: " + qtyChangeFailed + ", " +
        // priceChangeFailed);
        // else
        // downStreamListener.onOrder(ExecType.REPLACE, child, null,
        // "Qty/Price change failed: " + qtyChangeFailed + ", " +
        // priceChangeFailed);
        // pendingAmends.remove(orderId);
        // }

        qtyHasChanged = child.getQuantity() != order.m_totalQuantity;
        priceHasChanged = !PriceUtils.Equal(child.getPrice(), order.m_lmtPrice);

        child.setQuantity(order.m_totalQuantity);
        child.setPrice(order.m_lmtPrice);
    }

    @Override
    synchronized public void openOrderEnd() {
        log.debug(EWrapperMsgGenerator.openOrderEnd());
        for (Integer orderId : openOrderIds) {
            log.info("Cancelling open order: " + orderId);
            this.clientSocket.cancelOrder(orderId);
        }
        openOrderIds.clear();
        cancellingOpenOrders = false;
    }

    @Override
    public void updateAccountValue(String key, String value, String currency,
                                   String accountName) {
        log.debug(EWrapperMsgGenerator.updateAccountValue(key, value, currency,
                accountName));

    }

    @Override
    public void updatePortfolio(Contract contract, int position,
                                double marketPrice, double marketValue, double averageCost,
                                double unrealizedPNL, double realizedPNL, String accountName) {
        log.debug(EWrapperMsgGenerator.updatePortfolio(contract, position,
                marketPrice, marketValue, averageCost, unrealizedPNL,
                realizedPNL, accountName));
    }

    @Override
    public void updateAccountTime(String timeStamp) {
        log.debug(EWrapperMsgGenerator.updateAccountTime(timeStamp));
    }

    @Override
    public void accountDownloadEnd(String accountName) {
        log.debug(EWrapperMsgGenerator.accountDownloadEnd(accountName));
    }

    @Override
    public void nextValidId(int orderId) {
        log.info(id + " nextValidId: " + orderId);

        if (orderId > nextOrderId.get()) {
            nextOrderId.set(orderId);
        } else {
            log.debug(id + " use ourself " + nextOrderId.get());
        }

        isConnected = true;
        notifyAdaptorState(isConnected);

        //request ib News
        newsLog.info("IB Request News");
        clientSocket.reqNewsBulletins(newsIsSubCurrentDay);
    }

    @Override
    public void contractDetails(int reqId, ContractDetails contractDetails) {
        log.debug(EWrapperMsgGenerator.contractDetails(reqId, contractDetails));
    }

    @Override
    public void bondContractDetails(int reqId, ContractDetails contractDetails) {
        log.debug(EWrapperMsgGenerator.bondContractDetails(reqId,
                contractDetails));
    }

    @Override
    public void contractDetailsEnd(int reqId) {
        log.debug(EWrapperMsgGenerator.contractDetailsEnd(reqId));
    }

    @Override
    public void execDetails(int reqId, Contract contract, Execution execution) {
        log.debug(EWrapperMsgGenerator.execDetails(reqId, contract, execution));
    }

    @Override
    public void execDetailsEnd(int reqId) {
        log.debug(EWrapperMsgGenerator.execDetailsEnd(reqId));
    }

    @Override
    public void updateMktDepthL2(int tickerId, int position,
                                 String marketMaker, int operation, int side, double price, int size) {
        log.debug(EWrapperMsgGenerator.updateMktDepthL2(tickerId, position,
                marketMaker, operation, side, price, size));

    }

    @Override
    public void updateNewsBulletin(int msgId, int msgType, String message,
                                   String origExchange) {
        newsLog.info(EWrapperMsgGenerator.updateNewsBulletin(msgId, msgType,
                message, origExchange));
    }

    @Override
    public void managedAccounts(String accountsList) {
        log.debug(EWrapperMsgGenerator.managedAccounts(accountsList));
    }

    @Override
    public void receiveFA(int faDataType, String xml) {
        log.debug(EWrapperMsgGenerator.receiveFA(faDataType, xml));
    }

    @Override
    public void historicalData(int reqId, String date, double open,
                               double high, double low, double close, int volume, int count,
                               double WAP, boolean hasGaps) {
        log.debug(EWrapperMsgGenerator.historicalData(reqId, date, open, high,
                low, close, volume, count, WAP, hasGaps));
    }

    @Override
    public void scannerParameters(String xml) {
        log.debug(EWrapperMsgGenerator.scannerParameters(xml));
    }

    @Override
    public void scannerData(int reqId, int rank,
                            ContractDetails contractDetails, String distance, String benchmark,
                            String projection, String legsStr) {
        log.debug(EWrapperMsgGenerator.scannerData(reqId, rank,
                contractDetails, distance, benchmark, projection, legsStr));
    }

    @Override
    public void scannerDataEnd(int reqId) {
        log.debug(EWrapperMsgGenerator.scannerDataEnd(reqId));
    }

    @Override
    public void realtimeBar(int reqId, long time, double open, double high,
                            double low, double close, long volume, double wap, int count) {
        log.debug(EWrapperMsgGenerator.realtimeBar(reqId, time, open, high,
                low, close, volume, wap, count));
    }

    @Override
    public void currentTime(long time) {
        log.debug(EWrapperMsgGenerator.currentTime(time));
    }

    @Override
    public void fundamentalData(int reqId, String data) {
        log.debug(EWrapperMsgGenerator.fundamentalData(reqId, data));
    }

    @Override
    public void deltaNeutralValidation(int reqId, UnderComp underComp) {
        log.debug(EWrapperMsgGenerator.deltaNeutralValidation(reqId, underComp));
    }

    @Override
    public void marketDataType(int reqId, int marketDataType) {
        log.debug(EWrapperMsgGenerator.marketDataType(reqId, marketDataType));
    }

    // ////////////////////////////////////
    // End: implementation of EWrapper
    // ////////////////////////////////////

    @Override
    public void clean() {

    }

    // ////////////////////
    // getters and setters
    // ////////////////////

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public boolean isReqMarketDepth() {
        return reqMarketDepth;
    }

    public void setReqMarketDepth(boolean reqMarketDepth) {
        this.reqMarketDepth = reqMarketDepth;
    }

    public String getGenericTickTags() {
        return genericTickTags;
    }

    public void setGenericTickTags(String genericTickTags) {
        this.genericTickTags = genericTickTags;
    }

    public int getDepthRows() {
        return depthRows;
    }

    public void setDepthRows(int depthRows) {
        this.depthRows = depthRows;
    }

    public boolean isLogMarketData() {
        return logMarketData;
    }

    public void setLogMarketData(boolean logMarketData) {
        this.logMarketData = logMarketData;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isCancelOpenOrders() {
        return cancelOpenOrders;
    }

    public void setCancelOpenOrders(boolean cancelOpenOrders) {
        this.cancelOpenOrders = cancelOpenOrders;
    }

    public void setNewsIsSubCurrentDay(boolean newsIsSubCurrentDay) {
        this.newsIsSubCurrentDay = newsIsSubCurrentDay;
    }

    public void setCheckLastTimeInterval(long checkLastTimeInterval) {
        this.checkLastTimeInterval = checkLastTimeInterval;
    }

    public void setReconnectOn(boolean reconnectOn) {
        this.reconnectOn = reconnectOn;
    }
}
