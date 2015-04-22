/**
 * ****************************************************************************
 * Copyright (c) 2011-2012 Cyan Spring Limited
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms specified by license file attached.
 * <p/>
 * Software distributed under the License is released on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * ****************************************************************************
 */
package com.cyanspring.adaptor;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cyanspring.common.event.marketdata.*;
import com.cyanspring.common.marketdata.*;
import com.cyanspring.id.Library.Util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.cyanspring.common.Clock;
import com.cyanspring.common.IPlugin;
import com.cyanspring.common.data.DataObject;
import com.cyanspring.common.event.AsyncTimerEvent;
import com.cyanspring.common.event.IAsyncEventManager;
import com.cyanspring.common.event.IRemoteEventManager;
import com.cyanspring.common.event.ScheduleManager;
import com.cyanspring.common.event.marketsession.MarketSessionEvent;
import com.cyanspring.common.event.marketsession.MarketSessionRequestEvent;
import com.cyanspring.common.event.marketsession.TradeDateEvent;
import com.cyanspring.common.marketsession.MarketSessionType;
import com.cyanspring.common.server.event.MarketDataReadyEvent;
import com.cyanspring.common.util.PriceUtils;
import com.cyanspring.common.util.TimeUtil;
import com.cyanspring.event.AsyncEventProcessor;

public class MarketDataReceiver implements IPlugin, IMarketDataListener,
        IMarketDataStateListener {
    private static final Logger log = LoggerFactory
            .getLogger(MarketDataReceiver.class);
    private static final Logger quoteLog = LoggerFactory
            .getLogger(MarketDataReceiver.class.getName() + ".QuoteLog");

    protected HashMap<String, Quote> quotes = new HashMap<String, Quote>();
    protected HashMap<String, DataObject> quoteExtends = new HashMap<String, DataObject>();
    protected Map<String, Quote> lastTradeDateQuotes = new HashMap<String, Quote>();
    protected Map<String, DataObject> lastTradeDateQuoteExtends = new HashMap<String, DataObject>();

    @Autowired
    protected IRemoteEventManager eventManager;

    protected ScheduleManager scheduleManager = new ScheduleManager();
    private PriceSessionQuoteChecker quoteChecker = new PriceSessionQuoteChecker();

    protected AsyncTimerEvent timerEvent = new AsyncTimerEvent();
    protected long quoteThrottle = 100; // 0 = no throttle
    protected long timerInterval = 300;
    protected Map<String, QuoteEvent> quotesToBeSent = new HashMap<String, QuoteEvent>();
    protected List<List<String>> preSubscriptionList = new ArrayList<List<String>>();
    protected List<IMarketDataAdaptor> adaptors = new ArrayList<IMarketDataAdaptor>();
    protected String tradeDate;

    private long lastQuoteSaveInterval = 20000;
    private boolean staleQuotesSent;
    private Date initTime = Clock.getInstance().now();
    private Map<MarketSessionType, Long> sessionMonitor;
    private Date chkDate;
    private long chkTime;
    private boolean quotePriceWarningIsOpen = false;
    private boolean quoteExtendEventIsSend = true;
    private int quotePriceWarningPercent = 99;
    private boolean quoteLogIsOpen = false;
    private int quoteExtendSegmentSize = 300;
    private IQuoteAggregator aggregator;
    boolean state = false;
    boolean isUninit = false;

    private AsyncEventProcessor eventProcessor = new AsyncEventProcessor() {

        @Override
        public void subscribeToEvents() {
            subscribeToEvent(TradeDateEvent.class, null);
            subscribeToEvent(MarketSessionEvent.class, null);
            subscribeToEvent(QuoteReplyEvent.class, null);

            for (Class clz : subscuribeEvent()) {
                subscribeToEvent(clz, null);
            }
        }

        @Override
        public IAsyncEventManager getEventManager() {
            return eventManager;
        }
    };

    protected List<Class> subscuribeEvent() {
        return new ArrayList<Class>();
    }

    public void processQuoteReplyEvent(QuoteReplyEvent event) {
        for (Entry<String, Quote> entry : event.getQuotes().entrySet()) {

        }

        for (Entry<String, DataObject> entry : event.getQuoteExts().entrySet()) {

        }
    }

    public void processMarketSessionEvent(MarketSessionEvent event) throws Exception {
        if (null != quoteChecker)
            quoteChecker.setSession(event.getSession());
        chkTime = sessionMonitor.get(event.getSession());
        log.info("Get MarketSessionEvent: " + event.getSession()
                + ", map size: " + sessionMonitor.size() + ", checkTime: "
                + chkTime);

        if (aggregator != null) {
            aggregator.onMarketSession(event.getSession());
        }

        for (IMarketDataAdaptor adapter : adaptors) {
            String adapterName = adapter.getClass().getSimpleName();
            if (adapterName.equals("WindFutureDataAdaptor")) {
                ((com.cyanspring.adaptor.future.wind.WindFutureDataAdaptor) adapter).processMarketSession(event);
                if (MarketSessionType.PREOPEN == event.getSession()) {
                    log.debug("Process Wind Future PREOPEN resubscribe");
                    ((com.cyanspring.adaptor.future.wind.WindFutureDataAdaptor) adapter).clearSubscribeMarketData();
                    preSubscribe();
                }
            }
        }

    }

    public boolean processTradeDateEvent(TradeDateEvent event) {
        String newTradeDate = event.getTradeDate();
        if (tradeDate == null || !newTradeDate.equals(tradeDate)) {
            tradeDate = newTradeDate;
            try {
                List<Quote> lst = new ArrayList<Quote>(lastTradeDateQuotes.values());
                log.info("LastTradeDatesQuotes: " + lst + ", tradeDate:" + tradeDate);
                eventManager.sendRemoteEvent(new LastTradeDateQuotesEvent(null, null, tradeDate, lst));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    protected void sendQuoteEvent(QuoteEvent event) {
        try {
            eventManager.sendEvent(event);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void clearAndSendQuoteEvent(QuoteEvent event) {
        event.getQuote().setTimeSent(Clock.getInstance().now());
        quotesToBeSent.remove(event.getQuote().getSymbol()); // clear anything
        // in queue
        // because we
        // are sending
        // it now
        if (null != aggregator) {
            aggregator.reset(event.getQuote().getSymbol());
        }
        sendQuoteEvent(event);
    }

    private void logStaleInfo(Quote prev, Quote quote, boolean stale) {
        log.info("Quote stale: " + quote.getSymbol() + ", " + stale
                + ", Prev: " + prev + ", New: " + quote);
    }

    // Check Quote Value
    public boolean checkQuote(Quote prev, Quote quote) {
        boolean IsCorrectQuote = true;
        if (prev != null) {
            if (quote.getClose() <= 0) {
                quote.setClose(prev.getClose());
            }
            if (quote.getOpen() <= 0) {
                quote.setOpen(prev.getOpen());
            }
            if (quote.getHigh() <= 0) {
                quote.setHigh(prev.getHigh());
            }
            if (quote.getLow() <= 0) {
                quote.setLow(prev.getLow());
            }
            if (quote.getBid() <= 0) {
                quote.setBid(prev.getBid());
            }
            if (quote.getAsk() <= 0) {
                quote.setAsk(prev.getAsk());
            }
        }

        if (isQuotePriceWarningIsOpen()) {
            if (quote.getClose() > 0 && getQuotePriceWarningPercent() > 0
                    && getQuotePriceWarningPercent() < 100) {
                double preCloseAddWarningPrice = quote.getClose()
                        * (1.0 + getQuotePriceWarningPercent() / 100.0);
                double preCloseSubtractWarningPrice = quote.getClose()
                        * (1.0 - getQuotePriceWarningPercent() / 100.0);
                if (quote.getAsk() > 0
                        && (PriceUtils.GreaterThan(quote.getAsk(),
                        preCloseAddWarningPrice) || PriceUtils
                        .LessThan(quote.getAsk(),
                                preCloseSubtractWarningPrice))) {
                    IsCorrectQuote = false;
                }
                if (quote.getBid() > 0
                        && (PriceUtils.GreaterThan(quote.getBid(),
                        preCloseAddWarningPrice) || PriceUtils
                        .LessThan(quote.getBid(),
                                preCloseSubtractWarningPrice))) {
                    IsCorrectQuote = false;
                }
                if (quote.getHigh() > 0
                        && (PriceUtils.GreaterThan(quote.getHigh(),
                        preCloseAddWarningPrice) || PriceUtils
                        .LessThan(quote.getHigh(),
                                preCloseSubtractWarningPrice))) {
                    IsCorrectQuote = false;
                }
                if (quote.getLow() > 0
                        && (PriceUtils.GreaterThan(quote.getLow(),
                        preCloseAddWarningPrice) || PriceUtils
                        .LessThan(quote.getLow(),
                                preCloseSubtractWarningPrice))) {
                    IsCorrectQuote = false;
                }
                if (quote.getOpen() > 0
                        && (PriceUtils.GreaterThan(quote.getOpen(),
                        preCloseAddWarningPrice) || PriceUtils
                        .LessThan(quote.getOpen(),
                                preCloseSubtractWarningPrice))) {
                    IsCorrectQuote = false;
                }
            }
        }

        return IsCorrectQuote;
    }

    public void processInnerQuoteEvent(InnerQuoteEvent inEvent) {
        Quote quote = inEvent.getQuote();
        Quote prev = quotes.get(quote.getSymbol());

        //Calculate Future Quote last Volume
        if (inEvent.getSourceId() > 100) {
            if (prev != null && DateUtil.formatDate(prev.getTimeStamp(), "yyyy-MM-dd").equals(tradeDate)) {
                quote.setLastVol(quote.getTotalVolume() - prev.getTotalVolume());
            } else {
                quote.setLastVol(quote.getTotalVolume());
            }
        }

        if (isQuoteLogIsOpen()) {
            quoteLog.info("Quote Receive : " + "Sc="
                            + inEvent.getSourceId() + ",Symbol=" + quote.getSymbol()
                            + ",A=" + quote.getAsk() + ",B=" + quote.getBid()
                            + ",C=" + quote.getClose() + ",O=" + quote.getOpen()
                            + ",H=" + quote.getHigh() + ",L=" + quote.getLow()
                            + ",Last=" + quote.getLast()
                            + ",Stale=" + quote.isStale() + ",ts="
                            + quote.getTimeStamp().toString() + ",wPcnt="
                            + getQuotePriceWarningPercent()
                            + ",lsV=" + quote.getLastVol() + ",tV=" + quote.getTotalVolume()
            );
        }

        if (!checkQuote(prev, quote) && inEvent.getSourceId() <= 100) {
            quoteLog.warn("Quote BBBBB! : " + "Sc=" + inEvent.getSourceId()
                            + ",Symbol=" + quote.getSymbol() + ",A=" + quote.getAsk()
                            + ",B=" + quote.getBid() + ",C=" + quote.getClose()
                            + ",O=" + quote.getOpen() + ",H=" + quote.getHigh()
                            + ",L=" + quote.getLow() + ",Last=" + quote.getLast()
                            + ",Stale=" + quote.isStale()
                            + ",ts=" + quote.getTimeStamp().toString()
                            + ",wPcnt=" + getQuotePriceWarningPercent() + ",lsV=" + quote.getLastVol() + ",tV=" + quote.getTotalVolume()
            );
            return;
        }

        if (null == prev) {
            logStaleInfo(prev, quote, quote.isStale());
            quotes.put(quote.getSymbol(), quote);
            clearAndSendQuoteEvent(inEvent.getQuoteEvent());
            return;
        } else if (null != quoteChecker
                && !quoteChecker.checkWithSession(quote)) {
            // if wind Adapter Quote always send,if other Adapter Quote prev not
            // stale to send
            if (inEvent.getSourceId() > 100) {
                // Stale continue send Quote
                quotes.put(quote.getSymbol(), quote);
                clearAndSendQuoteEvent(new QuoteEvent(inEvent.getKey(), null,
                        quote));
            } else {
                boolean prevStale = prev.isStale();
                logStaleInfo(prev, quote, true);
                prev.setStale(true); // just set the existing stale
                if (!prevStale) {
                    // Stale send prev Quote
                    clearAndSendQuoteEvent(new QuoteEvent(inEvent.getKey(),
                            null, prev));
                }
            }
            return;
        } else {
            quotes.put(quote.getSymbol(), quote);
            if (prev.isStale() != quote.isStale()) {
                logStaleInfo(prev, quote, quote.isStale());
            }
        }

        String symbol = inEvent.getQuote().getSymbol();

        if (null != aggregator) {
            quote = aggregator.update(symbol, inEvent.getQuote(),
                    inEvent.getSourceId());
        }

        QuoteEvent event = new QuoteEvent(inEvent.getKey(), null, quote);

        if (eventProcessor.isSync()) {
            sendQuoteEvent(event);
            return;
        }

        // queue up quotes
        if (null != prev && quoteThrottle != 0
                && TimeUtil.getTimePass(prev.getTimeSent()) < quoteThrottle) {
            quote.setTimeSent(prev.getTimeSent()); // important record the last
            // time sent of this quote
            quotesToBeSent.put(quote.getSymbol(), event);
            return;
        }

        // send the quote now
        clearAndSendQuoteEvent(event);
    }

    public void processAsyncTimerEvent(AsyncTimerEvent event) {
        // flush out all quotes throttled
        for (Entry<String, QuoteEvent> entry : quotesToBeSent.entrySet()) {
            sendQuoteEvent(entry.getValue());
            // log.debug("Sending throttle quote: " +
            // entry.getValue().getQuote());
        }
        quotesToBeSent.clear();
        broadCastStaleQuotes();
    }

    public void processTradeEvent(TradeEvent event) {
        eventManager.sendEvent(event);
    }

    public MarketDataReceiver(List<IMarketDataAdaptor> adaptors) {
        this.adaptors = adaptors;
    }

    @Override
    public void init() throws Exception {
        log.info("initialising");
        // subscribe to events
        eventProcessor.setHandler(this);
        eventProcessor.init();
        if (eventProcessor.getThread() != null)
            eventProcessor.getThread().setName("MarketDataManager");

        // requestMarketSession
        eventManager.sendEvent(new MarketSessionRequestEvent(null, null, true));

        chkDate = Clock.getInstance().now();
        for (IMarketDataAdaptor adaptor : adaptors) {
            log.debug("IMarketDataAdaptor=" + adaptor.getClass()
                    + " SubMarketDataState");
            adaptor.subscribeMarketDataState(this);
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (final IMarketDataAdaptor adaptor : adaptors) {
                    try {
                        adaptor.init();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }

        });

        thread.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                uninit();
            }
        });

        boolean curState = false;
        for (IMarketDataAdaptor adaptor : adaptors) {
            log.debug(adaptor.getClass() + ", State=" + adaptor.getState());
            if (adaptor.getState())
                curState = true;
        }

        if (curState) {
            log.debug("presubscribe quotes...");
            preSubscribe();
        }
        setState(curState);

        if (!eventProcessor.isSync())
            scheduleManager.scheduleRepeatTimerEvent(timerInterval,
                    eventProcessor, timerEvent);
    }

    private void broadCastStaleQuotes() {
        if (staleQuotesSent)
            return;

        if (TimeUtil.getTimePass(initTime) < lastQuoteSaveInterval)
            return;

        staleQuotesSent = true;
        for (Quote quote : quotes.values()) {
            if (quote.isStale())
                this.clearAndSendQuoteEvent(new QuoteEvent(quote.getSymbol(),
                        null, quote));
        }
    }

    public void reset() {
        quotes.clear();
        quoteExtends.clear();
    }

    @Override
    public void uninit() {
        if (isUninit)
            return;

        isUninit = true;

        log.info("uninitialising");
        if (!eventProcessor.isSync())
            scheduleManager.cancelTimerEvent(timerEvent);

        for (IMarketDataAdaptor adaptor : adaptors) {
            adaptor.uninit();
        }

        eventProcessor.uninit();
    }

    @Override
    public void onQuote(InnerQuote innerQuote) {
        if (TimeUtil.getTimePass(chkDate) > chkTime && chkTime != 0) {
            log.warn("Quotes receive time large than excepted.");
        }

        chkDate = Clock.getInstance().now();
        InnerQuoteEvent event = new InnerQuoteEvent(innerQuote.getSymbol(), null,
                innerQuote.getQuote(), innerQuote.getSourceId());
        eventProcessor.onEvent(event);
    }

    @Override
    public void onQuoteExt(DataObject quoteExt, int sourceId) {

        if (quoteExt != null && isQuoteExtendEventIsSend()) {

            StringBuffer sbQuoteExtendLog = new StringBuffer();
            for (String key : quoteExt.getFields().keySet()) {
                sbQuoteExtendLog.append("," + key + "=" + quoteExt.getFields().get(key));
            }
            quoteLog.info("QuoteExtend Receive : " + "Source=" + sourceId + sbQuoteExtendLog.toString());

            String symbol = quoteExt.get(String.class, QuoteExtDataField.SYMBOL.value());
            quoteExt.put(QuoteExtDataField.TIMESENT.value(), Clock.getInstance().now());
            quoteExtends.put(symbol, quoteExt);
            QuoteExtEvent event = new QuoteExtEvent(quoteExt.get(String.class,
                    QuoteExtDataField.SYMBOL.value()), null, quoteExt, sourceId);
            try {
                eventManager.sendGlobalEvent(event);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    @Override
    public void onTrade(Trade trade) {
        TradeEvent event = new TradeEvent(trade.getSymbol(), null, trade);
        eventProcessor.onEvent(event);
    }

    @Override
    public void onState(boolean on) {
        if (on) {
            log.info("MarketData feed is up");
            setState(true);
            eventManager.sendEvent(new MarketDataReadyEvent(null, true));
            preSubscribe();
        } else {
            for (IMarketDataAdaptor adaptor : adaptors) {
                if (adaptor.getState()) {
                    return;
                }
            }
            log.warn("MarketData feed is down");
            setState(false);
            eventManager.sendEvent(new MarketDataReadyEvent(null, false));
        }
    }

    private void preSubscribe() {
        if (null == preSubscriptionList)
            return;

        log.debug("Market data presubscribe: " + preSubscriptionList);
        try {
            for (int i = 0; i < preSubscriptionList.size(); i++) {
                List<String> preList = preSubscriptionList.get(i);
                IMarketDataAdaptor adaptor = adaptors.get(i);
                log.debug("Market data presubscribe adapter begin : Adapter=" + adaptor.getClass().getSimpleName() + ",State="
                        + adaptor.getState());
                if (!adaptor.getState())
                    continue;

                for (String symbol : preList) {
                    adaptor.subscribeMarketData(symbol, this);
                }
            }
        } catch (MarketDataException e) {
            log.error(e.getMessage(), e);
        }
    }

    public int getQuoteExtendSegmentSize() {
        return quoteExtendSegmentSize;
    }

    public void setQuoteExtendSegmentSize(int quoteExtendSegmentSize) {
        this.quoteExtendSegmentSize = quoteExtendSegmentSize;
    }

    public boolean isQuoteExtendEventIsSend() {
        return quoteExtendEventIsSend;
    }

    public void setQuoteExtendEventIsSend(boolean quoteExtendEventIsSend) {
        this.quoteExtendEventIsSend = quoteExtendEventIsSend;
    }

    public boolean isQuoteLogIsOpen() {
        return quoteLogIsOpen;
    }

    public void setQuoteLogIsOpen(boolean quoteLogIsOpen) {
        this.quoteLogIsOpen = quoteLogIsOpen;
    }

    public boolean isQuotePriceWarningIsOpen() {
        return quotePriceWarningIsOpen;
    }

    public int getQuotePriceWarningPercent() {
        return quotePriceWarningPercent;
    }

    public void setQuotePriceWarningIsOpen(boolean quotePriceWarningIsOpen) {
        this.quotePriceWarningIsOpen = quotePriceWarningIsOpen;
    }

    public void setQuotePriceWarningPercent(int quotePriceWarningPercent) {
        this.quotePriceWarningPercent = quotePriceWarningPercent;
    }

    public IQuoteAggregator getAggregator() {
        return aggregator;
    }

    public void setAggregator(IQuoteAggregator aggregator) {
        this.aggregator = aggregator;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public boolean isSync() {
        return eventProcessor.isSync();
    }

    public void setSync(boolean sync) {
        eventProcessor.setSync(sync);
    }

    public long getQuoteThrottle() {
        return quoteThrottle;
    }

    public void setQuoteThrottle(long quoteThrottle) {
        this.quoteThrottle = quoteThrottle;
    }

    public List<List<String>> getPreSubscriptionList() {
        return preSubscriptionList;
    }

    public void setPreSubscriptionList(List<List<String>> preSubscriptionList) {
        this.preSubscriptionList = preSubscriptionList;
    }

    public IQuoteChecker getQuoteChecker() {
        return quoteChecker;
    }

    public void setQuoteChecker(IQuoteChecker quoteChecker) {
        this.quoteChecker = (PriceSessionQuoteChecker) quoteChecker;
    }

    public void setSessionMonitor(Map<MarketSessionType, Long> sessionMonitor) {
        this.sessionMonitor = sessionMonitor;
    }

    public long getTimerInterval() {
        return timerInterval;
    }

    public void setTimerInterval(long timerInterval) {
        this.timerInterval = timerInterval;
    }

    public DataObject getQuoteExtendBySymbol(String symbol) {
        return quoteExtends.get(symbol);
    }

    public HashMap<String, DataObject> getQuoteExtends() {
        return quoteExtends;
    }
}
