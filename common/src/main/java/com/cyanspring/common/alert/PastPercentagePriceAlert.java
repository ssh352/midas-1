package com.cyanspring.common.alert;

public class PastPercentagePriceAlert extends BasePriceAlert{
	private String id;
	private String userId;
	private String symbol;
	private String dateTime ;
	private String content ;
	private double percentage ;	
	
	public PastPercentagePriceAlert(String userId, double percentage, String symbol, String strdateTime, String content, String commodity) {
		super(userId, percentage, symbol, strdateTime, content, commodity);
	}
	
	public PastPercentagePriceAlert()
	{
		super() ;
	}
	
	@Override
	protected String fieldtoString()
	{
		return new String("userId='" + userId + "',symbol='" + symbol + "',percentage=" + String.valueOf(percentage) +
				",datetime='" + dateTime + "'");
	}
}
