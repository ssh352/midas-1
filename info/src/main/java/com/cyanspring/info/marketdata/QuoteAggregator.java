package com.cyanspring.info.marketdata;

import java.util.concurrent.ConcurrentHashMap;

import com.cyanspring.common.marketdata.Quote;

public class QuoteAggregator implements IQuoteAggregator {
	
	
	ConcurrentHashMap<String, AggrQuote> table = new ConcurrentHashMap<String, AggrQuote>(); 
	
	AggrQuote getQuote(String symbol) {
		if (table.containsKey(symbol)) {
			return table.get(symbol);		
		}
		else {
			AggrQuote aggrQuote = new AggrQuote(symbol);
			table.put(symbol,aggrQuote);
			return aggrQuote;
		}
	}	
	
	public void reset(String symbol) {
		AggrQuote aggrQuote = getQuote(symbol);
		if (null != aggrQuote) {
			aggrQuote.reset();
		}
	}
	
	public Quote update(String symbol, Quote quote, int sourceId) {
		AggrQuote aggrQuote = getQuote(symbol);
		return aggrQuote.update(quote, sourceId);
		
	}
}