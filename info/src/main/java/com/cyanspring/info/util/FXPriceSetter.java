package com.cyanspring.info.util;

import java.util.Calendar;
import java.util.TimeZone;

import com.cyanspring.common.marketdata.HistoricalPrice;
import com.cyanspring.common.marketdata.Quote;
import com.cyanspring.common.util.PriceUtils;
import com.cyanspring.info.cdp.SymbolData;

public class FXPriceSetter implements IPriceSetter
{
	@Override
	public boolean setPrice(HistoricalPrice price, Quote quote, double LastVolume, long instrumentType) 
	{
		if (PriceUtils.EqualLessThan(quote.getBid(), 0) || PriceUtils.EqualLessThan(quote.getAsk(), 0))
		{
			return false;
		}
		double dPrice = (quote.getBid() + quote.getAsk()) / 2;
		boolean changed = price.setPrice(dPrice);
		price.setDatatime(quote.getTimeStamp()) ;
		return changed;
	}

	@Override
	public boolean setDataPrice(SymbolData symboldata, Quote quote, long instrumentType) 
	{
		if (PriceUtils.EqualLessThan(quote.getBid(), 0) || PriceUtils.EqualLessThan(quote.getAsk(), 0))
		{
			return false;
		}
		double dPrice = (quote.getBid() + quote.getAsk()) / 2;
		if (PriceUtils.LessThan(symboldata.getD52WHigh(), dPrice))
		{
			symboldata.setD52WHigh(dPrice) ;
		}
		if (PriceUtils.isZero(symboldata.getD52WLow()) 
				|| PriceUtils.GreaterThan(symboldata.getD52WLow(), dPrice))
		{
			symboldata.setD52WLow(dPrice);
		}
		if (PriceUtils.LessThan(symboldata.getdCurHigh(), dPrice))
		{
			symboldata.setdCurHigh(dPrice);
		}
		if (PriceUtils.isZero(symboldata.getdCurLow()) 
				|| PriceUtils.GreaterThan(symboldata.getdCurLow(), dPrice))
		{
			symboldata.setdCurLow(dPrice);
		}
		if (PriceUtils.isZero(symboldata.getdOpen()))
		{
			symboldata.setdOpen(dPrice);
		}
//		if(quote.getOpen() > 0 && PriceUtils.Equal(symboldata.getdOpen(),quote.getOpen()) == false) 
//		{
//			symboldata.setdOpen(quote.getOpen());
//		} // Ib & Id may have different first price, use first recieved price as open
		if (!PriceUtils.Equal(symboldata.getdClose(), dPrice))
		{
			symboldata.setdClose(dPrice);
		}
		return true;
	}
}
