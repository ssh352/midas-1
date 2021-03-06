package com.cyanspring.info.cdp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.event.info.PriceHighLowType;
import com.cyanspring.common.info.IRefSymbolInfo;
import com.cyanspring.common.marketdata.HistoricalPrice;
import com.cyanspring.common.marketdata.PriceHighLow;
import com.cyanspring.common.marketdata.Quote;
import com.cyanspring.common.marketdata.SymbolInfo;
import com.cyanspring.common.marketsession.MarketSessionType;
import com.cyanspring.common.staticdata.RefDataBitUtil;
import com.cyanspring.common.util.PriceUtils;
import com.cyanspring.info.util.IPriceSetter;
import com.cyanspring.info.util.InfoUtils;

public class SymbolData implements Comparable<SymbolData>
{
	private static final Logger log = LoggerFactory
			.getLogger(SymbolData.class);
//	private static final String insertPrice = "insert into %s (TRADEDATE,KEYTIME,DATATIME,SYMBOL,OPEN_PRICE,CLOSE_PRICE,HIGH_PRICE,LOW_PRICE,VOLUME,TOTALVOLUME,TURNOVER) " + 
//            "values ('%s','%s','%s','%s',%.5f,%.5f,%.5f,%.5f,%d,%.0f,%.5f) ON DUPLICATE KEY " + 
//            "Update TRADEDATE='%s',DATATIME='%s',OPEN_PRICE=%.5f,CLOSE_PRICE=%.5f,HIGH_PRICE=%.5f,LOW_PRICE=%.5f,VOLUME=%d,TOTALVOLUME=%.0f,TURNOVER=%.5f;";
	private static final String insertHead = "insert into %s (TRADEDATE,KEYTIME,DATATIME,SYMBOL,OPEN_PRICE,CLOSE_PRICE,HIGH_PRICE,LOW_PRICE,VOLUME,TOTALVOLUME,TURNOVER) VALUES ";
	private static final String insertValues = "('%s','%s','%s','%s',%.5f,%.5f,%.5f,%.5f,%d,%.0f,%.5f) ";
	private static final String insertTail = "ON DUPLICATE KEY UPDATE TRADEDATE=Values(TRADEDATE),DATATIME=values(DATATIME),OPEN_PRICE=Values(OPEN_PRICE),CLOSE_PRICE=Values(CLOSE_PRICE),HIGH_PRICE=Values(HIGH_PRICE),LOW_PRICE=Values(LOW_PRICE),VOLUME=Values(VOLUME),TOTALVOLUME=Values(TOTALVOLUME),TURNOVER=Values(TURNOVER);";
	private static final String DateFormat = "yyyy-MM-dd";
	private static final String DateTimeFormat = "yyyy-MM-dd HH:mm:ss";
	private Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")) ;
	
	private static IPriceSetter setter;
	private CentralDbProcessor centralDB = null;
	private boolean isUpdating = false;
	private boolean writeMin = false;
	private String strSymbol = null;
	private String market = null;
	private double d52WHigh = 0;
	private double d52WLow = 0; 
	private double dCurHigh = 0;
	private double dCurLow = 0;
	private double dOpen = 0;
	private double dClose = 0;
	private double dCurVolume = 0;
	private double dCurTotalVolume = 0;
	private double dCurTurnover = 0;
	private TreeMap<Date, HistoricalPrice> priceData = new TreeMap<Date, HistoricalPrice>() ;
//	private HashMap<String, List<HistoricalPrice>> mapHistorical = new HashMap<String, List<HistoricalPrice>>();
	private LinkedBlockingQueue<Quote> quoteTmp = new LinkedBlockingQueue<Quote>() ;

	private String sessionIndex;
	private MarketSessionType sessionType = null ;
	private Date sessionEnd;
	private String tradedate ;
	
	public SymbolData(String strSymbol, String market, CentralDbProcessor centralDB)
	{
		log.info("Create SymbolData " + strSymbol + " Market=" + market);
		isUpdating = true ;
		this.setStrSymbol(strSymbol) ;
		this.centralDB = centralDB ;
		this.market = market;
		resetStatement();
	}
	public void resetStatement()
	{
		isUpdating = true ;
		writeMin = false;
		d52WHigh = 0;
		d52WLow = 0; 
		dCurHigh = 0;
		dCurLow = 0;
		dOpen = 0;
		dClose = 0;
		dCurVolume = 0;
		dCurTotalVolume = 0;
		dCurTurnover = 0;
		readFromTick() ;
//		get52WHighLow() ;
		isUpdating = false ;
	}
	public SymbolData(String symbol) {
		this.setStrSymbol(symbol) ;
	}
	public void resetPriceData()
	{
//		int tickCount = centralDB.getTickCount() ;
		synchronized(priceData)
		{
			priceData.clear();
		}
	}
	public void resetMapHistorical() 
	{
		synchronized(getMapHistorical())
		{
			getMapHistorical().clear();
		}
	}
	public void parseQuote(Quote quote)
	{
		try 
		{
			quoteTmp.put(quote) ;
		} 
		catch (InterruptedException e) 
		{
			log.error(e.toString(), e);
		}
		if (isUpdating)
		{
			return;
		}
		else
		{
			Quote q;
//			String commodity = centralDB.getRefSymbolInfo().getbySymbol(strSymbol).getCommodity();
			long instrumentType = centralDB.getRefSymbolInfo().getbySymbol(strSymbol).getInstrumentType();
			while ((q = quoteTmp.poll()) != null)
			{
				if (instrumentType == 0)
					continue;
//				if ((commodity.equals("FC") || commodity.equals("FI")) &&
				if ((RefDataBitUtil.isFutures(instrumentType) || RefDataBitUtil.isIndex(instrumentType))
						&& PriceUtils.isZero(q.getTotalVolume()))
				{
					continue;
				}
				else if (RefDataBitUtil.isStock(instrumentType) &&
						(PriceUtils.isZero(q.getTotalVolume()) || PriceUtils.isZero(q.getTurnover())))
				{
					continue;
				}
				setPrice(q, instrumentType) ;
				dCurVolume = q.getTotalVolume() ;
			}
		}
	}
	public boolean setPrice(Quote quote, long instrumentType)
	{
		boolean changed;
		synchronized(priceData)
		{
			cal.setTime(quote.getTimeStamp()) ;
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			HistoricalPrice price = priceData.get(cal.getTime()) ;
			if (price == null)
			{
				price = new HistoricalPrice(strSymbol, getTradedate(), cal.getTime());
				changed = setter.setPrice(price, quote, dCurVolume, instrumentType);
				if (changed)
					priceData.put(cal.getTime(), price);
			}
			else
			{
				changed = setter.setPrice(price, quote, dCurVolume, instrumentType);
			}
		}
		if (changed && writeMin) //writeToMin() ; 
		{
			centralDB.getChartCacheProcessor().put(this);
			writeMin = false;
		}
		setter.setDataPrice(this, quote, instrumentType);
		return changed;
	}
	
	@SuppressWarnings("unchecked")
	public void readFromTick()
	{
		resetPriceData();
		String tradedate = getTradedate();
		if (tradedate == null)
		{
        	log.debug(String.format("Read: TradeDate null"));
			return;
		}
		String strFile = String.format("./DAT/%s/%s", 
				getTradedate(), getStrSymbol()) ;
		File fileMins = new File(strFile + ".1M");
		File fileCache = new File(strFile + ".Ch");
		if (fileMins.exists() == false)
		{
        	log.debug(String.format("Read: %s not exist", strFile));
			return;
		}
	    try
        {
	        FileInputStream fis = new FileInputStream(fileMins);
            FSTObjectInput in = new FSTObjectInput(fis);
            priceData = (TreeMap<Date, HistoricalPrice>) in.readObject(TreeMap.class);
            in.close();
            fis.close();
			
			if (fileCache.exists())
			{
				Scanner data = new Scanner(fileCache);
				data.useDelimiter(";");
				String str = data.next();
				dOpen = Double.parseDouble(str);
				str = data.next();
				dCurHigh = Double.parseDouble(str);
				str = data.next();
				dCurLow = Double.parseDouble(str);
				str = data.next();
				dClose = Double.parseDouble(str);
				str = data.next();
				d52WHigh = Double.parseDouble(str);
				str = data.next();
				d52WLow = Double.parseDouble(str);
				str = data.next();
				dCurVolume = Double.parseDouble(str);
				data.close();
			}
        	log.debug(String.format("Read: S:%s;O:%f;H:%f;L:%f;C:%f;52H:%f;52L:%f;V:%d", 
        			strSymbol, dOpen, dCurHigh, dCurLow, dClose, d52WHigh, d52WLow, (long)dCurVolume));
        }
        catch (Exception e)
        {
        	log.error(e.getMessage(), e);
        }
	}
	
	public void writeToMin()
	{
		writeMin = false;
		if (getTradedate() == null)
		{
			return;
		}
		String strFile = String.format("./DAT/%s/%s", 
				getTradedate(), getStrSymbol()) ;
		File fileMins = new File(strFile + ".1M") ;
        fileMins.getParentFile().mkdirs();
        try
        {
        	FileWriter fwrite = new FileWriter(strFile + ".Ch");
        	log.debug(String.format("Cache: S:%s;O:%f;H:%f;L:%f;C:%f;52H:%f;52L:%f;V:%d", 
        			strSymbol, dOpen, dCurHigh, dCurLow, dClose, d52WHigh, d52WLow, (long)dCurVolume));
        	fwrite.write(String.format("%f;%f;%f;%f;%f;%f;%d", 
        			dOpen, dCurHigh, dCurLow, dClose, d52WHigh, d52WLow, (long)dCurVolume));
        	fwrite.flush();
        	fwrite.close();
        	synchronized(priceData)
    		{
	            FileOutputStream fos = new FileOutputStream(fileMins, false);
	            FSTObjectOutput out = new FSTObjectOutput(fos);
	            out.writeObject( priceData, TreeMap.class );
	            // DON'T out.close() when using factory method;
	            out.flush();
	            out.close();
	            fos.close();
    		}
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
        }
	}
	
	public void insertSQLDate(String strType)
	{
    	log.debug(strSymbol + "Processing type \"" + strType + "\" chart");
    	if (market == null || PriceUtils.isZero(dOpen))
    	{
			log.warn(strSymbol + " get Open price ZERO");
    		return;
    	}
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")) ;
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		Date currentDate = cal.getTime();
		if (strType.equals("W"))
		{
			cal.add(Calendar.DAY_OF_WEEK, (-1) * (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY));
//			cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		}
		else if (strType.equals("M"))
		{
			cal.set(Calendar.DAY_OF_MONTH, 1);
		}
		Date keyDate = cal.getTime();
    	String prefix = (market.equals("FX")) ? "0040" : market;
		String strTable = String.format("%s_%s", prefix, strType) ;
		String sqlcmd = "" ;
		String tradeDate = getTradedate() ;
		HistoricalPrice lastPrice = null;
		String strSymbol = getRefSymbol();
		if (strType.equals("W") || strType.equals("M"))
		{
			if (getMapHistorical().get(strType) == null)
			{
//				lastPrice = centralDB.getDbhnd().getLastValue(market, strType, getStrSymbol(), false) ;
			}
			else
			{
				if (getMapHistorical().get(strType).size() > 0)
					lastPrice = getMapHistorical().get(strType).get(getMapHistorical().get(strType).size()-1);
			}
			SimpleDateFormat sdf = new SimpleDateFormat(DateFormat);
			Calendar cal_ = Calendar.getInstance() ;
			List<HistoricalPrice> listBase = null;
			HistoricalPrice curPrice = new HistoricalPrice(getTradedate(), 
					keyDate, 
					currentDate, 
					getStrSymbol(), 
					getdOpen(), 
					getdCurHigh(), 
					getdCurLow(), 
					getdClose(), 
					(long)dCurVolume) ;
			curPrice.setTotalVolume(getdCurTotalVolume());
			curPrice.setTurnover(getdCurTurnover());
			
			if (lastPrice != null && lastPrice.getKeytime() != null)
			{
				cal_.setTime(lastPrice.getKeytime());
				if (strType.equals("W"))
				{
					if (InfoUtils.getWeek(sdf.format(cal_.getTime())) == InfoUtils.getWeek(sdf.format(cal.getTime())))
						listBase = centralDB.getDbhnd().getPeriodValue(market, "W", getStrSymbol(), lastPrice.getKeytime());
				}
				else
				{
					if ((cal_.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
							&& cal_.get(Calendar.YEAR) == cal.get(Calendar.YEAR)))
						listBase = centralDB.getDbhnd().getPeriodValue(market, "M", getStrSymbol(), lastPrice.getKeytime());
				}
				HistoricalPrice newPrice = null;
				if (listBase != null)
				{
					double amountVolume = 0, amountTurnover = 0;
					newPrice = new HistoricalPrice(lastPrice.getSymbol(), lastPrice.getTradedate(), lastPrice.getKeytime());
					for (HistoricalPrice price : listBase)
					{
						newPrice.update(price);
						amountVolume += price.getTotalVolume();
						amountTurnover += price.getTurnover();
					}
					newPrice.update(curPrice);
					amountVolume += curPrice.getTotalVolume();
					amountTurnover += curPrice.getTurnover();
					newPrice.setTotalVolume(amountVolume);
					newPrice.setTurnover(amountTurnover);
				}
				else 
				{
					newPrice = (HistoricalPrice) curPrice.clone();
				}
				lastPrice = (HistoricalPrice) newPrice.clone();
			}
			else
			{
				lastPrice = centralDB.getDbhnd().getLastValue(market, strType, getStrSymbol(), false) ;
				if (lastPrice == null)
					lastPrice = (HistoricalPrice) curPrice.clone();
				else
				{
					cal_.setTime(lastPrice.getKeytime());
					if (InfoUtils.getWeek(sdf.format(cal_.getTime())) == InfoUtils.getWeek(sdf.format(cal.getTime()))
							|| (cal_.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
									&& cal_.get(Calendar.YEAR) == cal.get(Calendar.YEAR)))
						lastPrice.update(curPrice);
					else
						lastPrice = (HistoricalPrice) curPrice.clone();
				}
			}
		}
		else
		{
			lastPrice = new HistoricalPrice(getTradedate(),
											keyDate,
											currentDate,
											getStrSymbol(),
											getdOpen(),
											getdCurHigh(),
											getdCurLow(),
											getdClose(),
											(long)dCurVolume);
			lastPrice.setTotalVolume(getdCurTotalVolume());
			lastPrice.setTurnover(getdCurTurnover());
		}
		SimpleDateFormat sdf = new SimpleDateFormat(DateTimeFormat);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		sqlcmd = String.format(insertHead, strTable);
		sqlcmd += String.format(insertValues, 
				tradeDate, sdf.format(lastPrice.getKeytime()), sdf.format(lastPrice.getDatatime()), 
				getStrSymbol(), lastPrice.getOpen(), lastPrice.getClose(), 
				lastPrice.getHigh(), lastPrice.getLow(), lastPrice.getVolume(), 
				lastPrice.getTotalVolume(), lastPrice.getTurnover()) ;
		sqlcmd += insertTail;
		centralDB.getDbhnd().updateSQL(sqlcmd);
		if (strSymbol.equals(getStrSymbol()) == false)
		{
			sqlcmd = String.format(insertHead, strTable);
			sqlcmd += String.format(insertValues, 
					tradeDate, sdf.format(lastPrice.getKeytime()), sdf.format(lastPrice.getDatatime()), 
					strSymbol, lastPrice.getOpen(), lastPrice.getClose(), 
					lastPrice.getHigh(), lastPrice.getLow(), lastPrice.getVolume(),  
					lastPrice.getTotalVolume(), lastPrice.getTurnover());
			sqlcmd += insertTail;
			centralDB.getDbhnd().updateSQL(sqlcmd);
		}
		logHistoricalPrice(lastPrice);
	}
	
	public PriceHighLow getPriceHighLow(PriceHighLowType type)
	{
		PriceHighLow phl = new PriceHighLow() ;
		if (type == PriceHighLowType.DAY)
		{
			phl.setHigh(this.getdCurHigh());
			phl.setLow(this.getdCurLow());
			phl.setSymbol(this.getStrSymbol());
			phl.setType(type) ;
		}
		else if (type == PriceHighLowType.W52)
		{
			if (getMapHistorical().get("W") == null
					|| PriceUtils.isZero(this.getD52WHigh())
					|| PriceUtils.isZero(this.getD52WLow()))
				get52WHighLow();
			phl.setHigh(this.getD52WHigh());
			phl.setLow(this.getD52WLow());
			phl.setSymbol(this.getStrSymbol());
			phl.setType(type) ;
		}
		return phl ;
	}
	
	public void get52WHighLow()
	{
		log.debug(strSymbol + " get52WHighLow()");
    	if (market == null)
    	{
    		return;
    	}
    	ArrayList<String> symbolarr = new ArrayList<String>();
    	symbolarr.add(strSymbol);
    	ArrayList<SymbolInfo> symbolinfos = (ArrayList<SymbolInfo>) centralDB.getRefSymbolInfo().getBySymbolStrings(symbolarr);
    	if (symbolinfos.size() < 1)
    	{
    		return;
    	}
    	String symbol;
    	if (symbolinfos.get(0).hasRefSymbol() == false)
    	{
    		symbol = symbolinfos.get(0).getCode(); 
    	}
    	else
    	{
    		String[] splits = symbolinfos.get(0).getCode().split("\\.");
    		symbol = symbolinfos.get(0).getHint();
    		if (splits.length > 1)
    			symbol  += ("." + splits[1]);
    	}
		centralDB.getDbhnd().get52WHighLow(this, market, symbol);
		log.debug(strSymbol + " get52WHighLow() end" + 
				String.format(" H:%.5f L:%.5f", getD52WHigh(), getD52WLow()));
		return ;
	}
	
	public void retrieveChartPrice()
	{
		log.debug("Retrieve chart data [" + strSymbol + "]");
		getMapHistorical().clear();
		getAllChartPrice();
		log.debug("Retrieve chart data [" + strSymbol + "] finish");
	}
	
	public void getAllChartPrice()
	{
		getChartPrice("1");
		getChartPrice("R");
		getChartPrice("A");
		getChartPrice("Q");
		getChartPrice("H");
		getChartPrice("6");
		getChartPrice("T");
		getChartPrice("D");
		getChartPrice("W");
		getChartPrice("M");
	}
	
	public void checkAllChartPrice()
	{
		log.info("Check chart data [" + strSymbol + "]");
		String[] types = {"1","R","A","Q","H","6","T","D","W","M"};
		for (String type : types)
		{
			if (getMapHistorical().get(type) == null)
			{
				getChartPrice(type);
			}
		}
		log.info("Check chart data [" + strSymbol + "] finish");
	}
	
	public void getChartPrice(String strType)
	{
		synchronized(getMapHistorical())
		{
			if (getMapHistorical().get(strType) != null)
			{
				return;
			}
			String strSymbol = getRefSymbol();
			log.debug(String.format("Retrieve chart data [%s,%s,%s,%d]", market, strSymbol, strType, centralDB.getHistoricalDataCount().get(strType)));
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, (-1) * (centralDB.getHistoricalDataPeriod().get(strType) + 30));
			SimpleDateFormat sdf = new SimpleDateFormat(DateFormat);
			List<HistoricalPrice> historical = centralDB.getDbhnd().getCountsValue(
					market, strType, strSymbol, centralDB.getHistoricalDataCount().get(strType), sdf.format(cal.getTime()), true);
			if (historical != null)
			{
				log.debug(String.format("Retrieve chart data [%s,%s,%s,%d] get %d", 
						market, strSymbol, strType, centralDB.getHistoricalDataCount().get(strType), historical.size()));
//				if(historical.size() < centralDB.getHistoricalDataCount().get(strType)) 
//				{
//					cal.add(Calendar.DATE, -30);
//					historical = centralDB.getDbhnd().getCountsValue(
//							market, strType, strSymbol, centralDB.getHistoricalDataCount().get(strType), sdf.format(cal.getTime()), true);
//					log.debug(String.format("Retrieve chart data [%s,%s,%s,%d] lack of data, query again get %d", 
//							market, strSymbol, strType, centralDB.getHistoricalDataCount().get(strType), historical.size()));					
//				}
				getMapHistorical().put(strType,  historical);
			}
		}
	}
	
	public ArrayList<HistoricalPrice> getPriceList(String strType, 
												   ArrayList<HistoricalPrice> prices) throws ParseException
	{
		HistoricalPrice priceEmpty = null ;
		if (prices.isEmpty())
		{
			if (getMapHistorical().get(strType) == null)
			{
//				priceEmpty = centralDB.getDbhnd().getLastValue(market, strType, getStrSymbol(), false) ;
			}
			else
			{
				if (getMapHistorical().get(strType).size() > 0)
					priceEmpty = getMapHistorical().get(strType).get(getMapHistorical().get(strType).size()-1);
			}
			if (priceEmpty != null)
			{
				prices.add(priceEmpty) ;
			}
		}
		else
		{
			priceEmpty = prices.get(prices.size()-1);
		}
//		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")) ;
		Calendar pricetime = Calendar.getInstance(TimeZone.getTimeZone("GMT")) ;
		Calendar emptytime = Calendar.getInstance(TimeZone.getTimeZone("GMT")) ;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd") ;
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		int priceMin = 0;
		int emptyMin = 0;
		HistoricalPrice price;
		synchronized(priceData)
		{
			if (strType.equals("1"))
			{
				for(Map.Entry<Date, HistoricalPrice> entry : priceData.entrySet())
				{
					price = entry.getValue();
					if (priceEmpty == null)
					{
						priceEmpty = (HistoricalPrice)price.clone();
					}
					if (price.getDatatime() == null 
							|| price.getDatatime().getTime() < priceEmpty.getDatatime().getTime())
					{
						continue;
					}
					prices.add(price) ;
				}
			}
			else
			{
				for(Map.Entry<Date, HistoricalPrice> entry : priceData.entrySet())
				{
					price = entry.getValue();
					if (price.getDatatime() == null)
					{
						continue ;
					}
					if (priceEmpty == null)
					{
						priceEmpty = (HistoricalPrice)price.clone();
						continue;
					}
					if (priceEmpty.getDatatime() == null)
					{
						priceEmpty.update(price);
						continue;
					}
					if (price.getDatatime().getTime() < priceEmpty.getDatatime().getTime())
					{
						continue;
					}
					pricetime.setTime(price.getDatatime());
					emptytime.setTime(priceEmpty.getDatatime());
					priceMin = pricetime.get(Calendar.HOUR_OF_DAY)*60 + pricetime.get(Calendar.MINUTE);
					emptyMin = emptytime.get(Calendar.HOUR_OF_DAY)*60 + emptytime.get(Calendar.MINUTE);
					if (strType.equals("R") 
							&& (priceMin/5 != emptyMin/5))
					{
						priceEmpty = (HistoricalPrice)price.clone() ;
						prices.add(priceEmpty) ;
						cal.setTime(price.getDatatime());
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MINUTE, ((pricetime.get(Calendar.MINUTE) / 5) * 5)) ;
						priceEmpty.setKeytime(cal.getTime());
					}
					else if (strType.equals("A") 
							&& (priceMin/10 != emptyMin/10))
					{
						priceEmpty = (HistoricalPrice)price.clone() ;
						prices.add(priceEmpty) ;
						cal.setTime(price.getDatatime());
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MINUTE, ((pricetime.get(Calendar.MINUTE) / 10) * 10)) ;
						priceEmpty.setKeytime(cal.getTime());
					}
					else if (strType.equals("Q") 
							&& (priceMin/15 != emptyMin/15))
					{
						priceEmpty = (HistoricalPrice)price.clone() ;
						prices.add(priceEmpty) ;
						cal.setTime(price.getDatatime());
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MINUTE, ((pricetime.get(Calendar.MINUTE) / 15) * 15)) ;
						priceEmpty.setKeytime(cal.getTime());
					}
					else if (strType.equals("H") 
							&& (priceMin/30 != emptyMin/30))
					{
						priceEmpty = (HistoricalPrice)price.clone() ;
						prices.add(priceEmpty) ;
						cal.setTime(price.getDatatime());
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MINUTE, ((pricetime.get(Calendar.MINUTE) / 30) * 30)) ;
						priceEmpty.setKeytime(cal.getTime());
					}
					else if (strType.equals("6") 
							&& ((pricetime.get(Calendar.HOUR_OF_DAY) != emptytime.get(Calendar.HOUR_OF_DAY))
									|| (pricetime.get(Calendar.DATE) != emptytime.get(Calendar.DATE))))
					{
						priceEmpty = (HistoricalPrice)price.clone() ;
						prices.add(priceEmpty) ;
						cal.setTime(price.getDatatime());
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.HOUR_OF_DAY, pricetime.get(Calendar.HOUR_OF_DAY)) ;
						priceEmpty.setKeytime(cal.getTime());
					}
					else if (strType.equals("T") 
							&& ((pricetime.get(Calendar.HOUR_OF_DAY)/4 != emptytime.get(Calendar.HOUR_OF_DAY)/4)
									|| (pricetime.get(Calendar.DATE) != emptytime.get(Calendar.DATE))))
					{
						priceEmpty = (HistoricalPrice)price.clone() ;
						prices.add(priceEmpty) ;
						cal.setTime(price.getDatatime());
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.HOUR_OF_DAY, (pricetime.get(Calendar.HOUR_OF_DAY) / 4) * 4) ;
						priceEmpty.setKeytime(cal.getTime());
					}
					else
					{
						priceEmpty.update(price);
					}
				}
			}
		}
		return prices ;
	}
	
	public void getPriceDate(String strType, ArrayList<HistoricalPrice> pricelist)
	{
		if (PriceUtils.isZero(getdOpen()))
		{
			return;
		}
//		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")) ;
		Calendar pricetime = Calendar.getInstance(TimeZone.getTimeZone("GMT")) ;
		Calendar emptytime = Calendar.getInstance(TimeZone.getTimeZone("GMT")) ;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd") ;
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		emptytime.set(Calendar.HOUR_OF_DAY, 0);
		emptytime.set(Calendar.MINUTE, 0);
		emptytime.set(Calendar.SECOND, 0);
		Date date = emptytime.getTime() ;
		HistoricalPrice curPrice = new HistoricalPrice(getTradedate(), 
													   date, 
													   date, 
													   getStrSymbol(), 
													   getdOpen(), 
													   getdCurHigh(), 
													   getdCurLow(), 
													   getdClose(), 
													   (long)dCurVolume) ;
		curPrice.setTotalVolume(getdCurTotalVolume());
		curPrice.setTurnover(getdCurTurnover());
		HistoricalPrice lastPrice = null ;
		if (pricelist.size() > 0)
		{
			lastPrice = pricelist.get(pricelist.size()-1) ;
			pricetime.setTime(lastPrice.getKeytime());
		}
		if (strType.equals("D") && lastPrice != null)
		{
			if (sdf.format(emptytime.getTime()).equals(sdf.format(pricetime.getTime())))
			{
				if (PriceUtils.isZero(getdCurHigh()) && lastPrice.getHigh() < getdCurHigh())
				{
					lastPrice.setHigh(getdCurHigh());
				}
				if (PriceUtils.isZero(getdCurLow()) && lastPrice.getLow() > getdCurLow())
				{
					lastPrice.setLow(getdCurLow());
				}
				if (!PriceUtils.isZero(getdClose()))
				{
					lastPrice.setClose(getdClose());
				}
				lastPrice.setDatatime(date);
			}
			else
			{
				pricelist.add(curPrice) ;
			}
		}
		else if (strType.equals("W") && lastPrice != null)
		{
			if (InfoUtils.getWeek(sdf.format(emptytime.getTime())) == InfoUtils.getWeek(sdf.format(pricetime.getTime())))
			{
				pricelist.remove(lastPrice);
				emptytime.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
				List<HistoricalPrice> listWeek = centralDB.getDbhnd().getPeriodValue(market, "W", getStrSymbol(), lastPrice.getKeytime());
				HistoricalPrice newPrice = new HistoricalPrice(lastPrice.getSymbol(), lastPrice.getTradedate(), lastPrice.getKeytime());
				if (listWeek != null)
				{
					for (HistoricalPrice price : listWeek)
					{
						newPrice.update(price);
					}
				}
				newPrice.update(curPrice);
				pricelist.add(newPrice);
			}
			else
			{
				pricelist.add(curPrice) ;
			}
		}
		else if (strType.equals("M") && lastPrice != null)
		{
			if (emptytime.get(Calendar.MONTH) == pricetime.get(Calendar.MONTH) 
					&& emptytime.get(Calendar.YEAR) == pricetime.get(Calendar.YEAR))
			{
				pricelist.remove(lastPrice);
				emptytime.set(Calendar.DAY_OF_MONTH, 1);
				List<HistoricalPrice> listMon = centralDB.getDbhnd().getPeriodValue(market, "M", getStrSymbol(), lastPrice.getKeytime());
				HistoricalPrice newPrice = new HistoricalPrice(lastPrice.getSymbol(), lastPrice.getTradedate(), lastPrice.getKeytime());
				if (listMon != null)
				{
					for (HistoricalPrice price : listMon)
					{
						newPrice.update(price);
					}
				}
				newPrice.update(curPrice);
				pricelist.add(newPrice);
			}
			else
			{
				pricelist.add(curPrice) ;
			}
		}
		
	}
	
	public void insertSQLTick(String strType)
	{
    	log.debug(strSymbol + "Processing type \"" + strType + "\" chart");
    	if (market == null)
    	{
    		return;
    	}
		ArrayList<HistoricalPrice> prices = new ArrayList<HistoricalPrice>() ;
		try {
			prices = getPriceList(strType, prices);
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			return ;
		}
		if (prices.isEmpty())
		{
			return ;
		}
		String strSymbol = getRefSymbol();

    	String prefix = (market.equals("FX")) ? "0040" : market;
		String strTable = String.format("%s_%s", prefix, strType) ;
		String sqlcmd = "" ;//"START TRANSACTION;" ;
		String strKeyTime = "" ;
		SimpleDateFormat sdf = new SimpleDateFormat(DateTimeFormat) ;
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		sqlcmd = String.format(insertHead, strTable);
		int count = 0;
		for (HistoricalPrice price : prices)
		{
			if (price.getDatatime() == null)
			{
				continue;
			}
			if (count != 0)
			{
				sqlcmd += ",";
			}
			strKeyTime = sdf.format(price.getKeytime()) ;
			sqlcmd += String.format(insertValues, 
					price.getTradedate(), strKeyTime, sdf.format(price.getDatatime()), 
					getStrSymbol(), price.getOpen(), price.getClose(), 
					price.getHigh(), price.getLow(), price.getVolume(),  
					price.getTotalVolume(), price.getTurnover()) ;
			count++;
			if (strSymbol.equals(getStrSymbol()) == false)
			{
				if (count != 0)
				{
					sqlcmd += ",";
				}
				sqlcmd += String.format(insertValues, 
						price.getTradedate(), strKeyTime, sdf.format(price.getDatatime()), 
						strSymbol, price.getOpen(), price.getClose(), 
						price.getHigh(), price.getLow(), price.getVolume(),  
						price.getTotalVolume(), price.getTurnover()) ;
				count++;
			}
			logHistoricalPrice(price);
			if (count >= 50)
			{
				count = 0;
				sqlcmd += insertTail;
				centralDB.getDbhnd().addBatch(sqlcmd);
				sqlcmd = String.format(insertHead, strTable);
			}
		}
		if (count != 0)
		{
			sqlcmd += insertTail;
			centralDB.getDbhnd().addBatch(sqlcmd);
		}
		centralDB.getDbhnd().executeBatch();
		return ;
	}
	
	public void putInsert()
	{
		centralDB.getInsertProc().put(getStrSymbol());
	}
	
	public List<HistoricalPrice> getHistoricalPrice(String type, String symbol, int dataCount)
	{
    	if (market == null)
    	{
    		return null;
    	}
		SimpleDateFormat sdfprice = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") ;
		sdfprice.setTimeZone(TimeZone.getTimeZone("GMT"));
//		String strSymbol = getRefSymbol();
		
		ArrayList<HistoricalPrice> listPrice;
		if (getMapHistorical().get(type) == null)
		{
			getChartPrice(type);
		}
		int size = getMapHistorical().get(type).size();
		int limit = (dataCount > size) ? size : dataCount;
		listPrice = new ArrayList<HistoricalPrice>();
		listPrice.addAll(getMapHistorical().get(type).subList(size - limit, size));
		Collections.sort(listPrice);
		
		if (getSessionType() != MarketSessionType.CLOSE)
		{
			try {
				switch(type)
				{
				case "1":
				case "R":
				case "A":
				case "Q":
				case "H":
				case "6":
				case "T":
				{
					getPriceList(type, listPrice) ;
					break ;
				}
				case "D":
				case "W":
				case "M":
				{
					getPriceDate(type, listPrice) ;
					break;
				}
				default:
					return null ;
				}
			} catch (ParseException e) {
				log.error(e.getMessage(), e);
				return null ;
			}
		}
		Collections.sort(listPrice);
		return listPrice ;
	}
	
	public void logHistoricalPrice(HistoricalPrice hp)
	{
		//if (...)
		log.debug(String.format("%s|%s : %s open: %.5f, high: %.5f, low: %.5f, close:%.5f, volume: %d tvol: %.0f tovr: %.0f", 
				hp.getKeytime(), hp.getDatatime(), hp.getSymbol(), hp.getOpen(),
				hp.getHigh(), hp.getLow(), hp.getClose(), hp.getVolume(),
				hp.getTotalVolume(), hp.getTurnover()));
	}
	@Override
	public int compareTo(SymbolData o) {
		return this.getStrSymbol().compareTo(o.getStrSymbol());
	}
	public String getStrSymbol() {
		return strSymbol;
	}
	public void setStrSymbol(String strSymbol) {
		this.strSymbol = strSymbol;
	}
	public String getMarket() {
		return market;
	}
	public void setMarket(String market) {
		this.market = market;
	}
	public boolean isWriteMin() {
		return writeMin;
	}
	public void setWriteMin(boolean writeMin) {
		this.writeMin = writeMin;
	}
	public static IPriceSetter getSetter() {
		return setter;
	}
	public static void setSetter(IPriceSetter setter) {
		SymbolData.setter = setter;
	}
	public double getD52WHigh() {
		return d52WHigh;
	}
	public void setD52WHigh(double d52wHigh) {
		d52WHigh = d52wHigh;
	}
	public double getD52WLow() {
		return d52WLow;
	}
	public void setD52WLow(double d52wLow) {
		d52WLow = d52wLow;
	}
	public double getdCurHigh() {
		return dCurHigh;
	}
	public void setdCurHigh(double dCurHigh) {
		this.dCurHigh = dCurHigh;
	}
	public double getdCurLow() {
		return dCurLow;
	}
	public void setdCurLow(double dCurLow) {
		this.dCurLow = dCurLow;
	}
	public double getdOpen() {
		return dOpen;
	}
	public void setdOpen(double dOpen) {
		this.dOpen = dOpen;
	}
	public double getdClose() {
		return dClose;
	}
	public void setdClose(double dClose) {
		this.dClose = dClose;
	}
	public double getdCurTotalVolume() {
		return dCurTotalVolume;
	}
	public void setdCurTotalVolume(double dCurTotalVolume) {
		this.dCurTotalVolume = dCurTotalVolume;
	}
	public double getdCurTurnover() {
		return dCurTurnover;
	}
	public void setdCurTurnover(double dCurTurnover) {
		this.dCurTurnover = dCurTurnover;
	}
	public void clearMapHistorical()
	{
		getMapHistorical().clear();
	}
	public HashMap<String, List<HistoricalPrice>> getMapHistorical() {
//		return mapHistorical;
		String strSymbol = getRefSymbol();
		if (centralDB.getRetrieveMap().get(strSymbol) == null)
		{
			centralDB.getRetrieveMap().put(strSymbol, new  HashMap<String, List<HistoricalPrice>>());
		}
		return centralDB.getRetrieveMap().get(strSymbol);
	}
	public void setMapHistorical(HashMap<String, List<HistoricalPrice>> mapHistorical) 
	{
		String msg = strSymbol + " set cache map ";
		if (mapHistorical.get("1") != null)
			msg += "1:" + mapHistorical.get("1").size() + "\t";
		if (mapHistorical.get("R") != null)
			msg += "R:" + mapHistorical.get("R").size() + "\t";
		if (mapHistorical.get("A") != null)
			msg += "A:" + mapHistorical.get("A").size() + "\t";
		if (mapHistorical.get("Q") != null)
			msg += "Q:" + mapHistorical.get("Q").size() + "\t";
		if (mapHistorical.get("H") != null)
			msg += "H:" + mapHistorical.get("H").size() + "\t";
		if (mapHistorical.get("6") != null)
			msg += "6:" + mapHistorical.get("6").size() + "\t";
		if (mapHistorical.get("T") != null)
			msg += "T:" + mapHistorical.get("T").size() + "\t";
		if (mapHistorical.get("D") != null)
			msg += "D:" + mapHistorical.get("D").size() + "\t";
		if (mapHistorical.get("W") != null)
			msg += "W:" + mapHistorical.get("W").size() + "\t";
		if (mapHistorical.get("M") != null)
			msg += "M:" + mapHistorical.get("M").size();
		log.debug(msg);
//		synchronized (getMapHistorical())
//		{
//			this.mapHistorical = mapHistorical;
//		}
	}
	public void set52WHLByMapHistorical()
	{
		log.info(strSymbol + " set52WHLByMapHistorical() start");
		List<HistoricalPrice> listPrice = getMapHistorical().get("W");
		if (listPrice == null)
		{
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, (-1) * (centralDB.getHistoricalDataPeriod().get("W") + 30));
			SimpleDateFormat sdf = new SimpleDateFormat(DateFormat);
			listPrice = centralDB.getDbhnd().getCountsValue(
					market, "W", strSymbol, centralDB.getHistoricalDataCount().get("W"), sdf.format(cal.getTime()), true);
			if (listPrice == null)
				return;
		}
		if (listPrice.isEmpty())
		{
			log.info(strSymbol + " set52WHLByMapHistorical() stop, empty W chart");
			return;
		}
		Collections.sort(listPrice);
		int head = (listPrice.size() > 52) ? (listPrice.size() - 52) : 0;
		List<HistoricalPrice> listPrice52 = listPrice.subList(head, listPrice.size()-1);
		double dHigh = 0 ; 
		double dLow = 0 ;
		for (HistoricalPrice price : listPrice52)
		{
			dHigh = price.getHigh();
			dLow = price.getLow();
			if (getD52WHigh() < dHigh && PriceUtils.isZero(dHigh) == false)
			{
				setD52WHigh(dHigh) ;
			}
			if ((PriceUtils.isZero(getD52WLow()) || getD52WLow() > dLow) && PriceUtils.isZero(dLow) == false)
			{
				setD52WLow(dLow) ;
			}
		}
		log.info(strSymbol + " set52WHLByMapHistorical() end" + 
				String.format(" H:%.5f L:%.5f", getD52WHigh(), getD52WLow()));
	}
	public MarketSessionType getSessionType()
	{
		return sessionType;
	}
	public void setSessionType(MarketSessionType sessionType)
	{
		if (this.sessionType == null)
		{
			this.resetStatement();
		}
		else if (this.sessionType != sessionType)
		{
			if (sessionType == MarketSessionType.CLOSE)
			{
				putInsert();
			}
			if (this.sessionType == MarketSessionType.CLOSE)
			{
				this.resetStatement();
			}
		}
		this.sessionType = sessionType;
	}
	public Date getSessionEnd()
	{
		return sessionEnd;
	}
	public void setSessionEnd(Date sessionEnd)
	{
		this.sessionEnd = sessionEnd;
	}
	public String getTradedate()
	{
		return tradedate;
	}
	public void setTradedate(String tradedate)
	{
		this.tradedate = tradedate;
		readFromTick();
	}
	public String getSessionIndex()
	{
		return sessionIndex;
	}
	public void setSessionIndex(String sessionIndex)
	{
		this.sessionIndex = sessionIndex;
	}
	public String getRefSymbol()
	{
		IRefSymbolInfo refsymbol = centralDB.getRefSymbolInfo();
		SymbolInfo symbolinfo = refsymbol.get(refsymbol.at(new SymbolInfo(centralDB.getServerMarket(), getStrSymbol())));
		String strSymbol = null;
		if (symbolinfo != null)
		{
			if (symbolinfo.hasRefSymbol())
			{
				strSymbol = String.format("%s.%s", symbolinfo.getHint(), symbolinfo.getExchange());
			}
		}
		if (strSymbol == null)
		{
			strSymbol = getStrSymbol();
		}
		return strSymbol;
	}
	public void setFTurnover(Date timestamp, long fTurnover)
	{
		HistoricalPrice price = priceData.get(timestamp);
		if (price == null)
		{
			return;
		}
	}
}
