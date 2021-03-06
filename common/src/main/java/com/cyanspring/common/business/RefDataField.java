package com.cyanspring.common.business;

import java.util.HashMap;

public enum RefDataField {
	SYMBOL("Symbol"),
	EXCHANGE("Exchange"),
	MARKET("Market"),
	CURRENCY("Currency"),
	FX_CURRENCY("FX Currency"),
	LOT_SIZE("Lot size"),
	OPEN("Open"),
	CLOSE("Close"),
	HIGH("High"),
	LOW("Low"),
	CONTRACT("Contract"),
	EN_DISPLAYNAME("ENName"),
	TW_DISPLAYNAME("TWName"),
	CN_DISPLAYNAME("CNName"),
	REF_SYMBOL("Ref symbol"),  // 相關商品. ex : 股票選擇權裡面的股票
	CATEGORY("Category"),      // 商品分類 . ex IF
	TICK_TABLE("Tick table"),  // tick table ref
	SETTLEMENT_DATE("Settlement date"),  // 到期日
	PRICE_PER_UNIT("Price per unit"),    // 報價單位
	EN_TRADING_UNIT("ENTrading unit"),   // 交易單位
	TW_TRADING_UNIT("TWTrading unit"),   // 交易單位
	CN_TRADING_UNIT("CNTrading unit"),   // 交易單位
	MARGIN_RATE("Margin Rate"),          // 保證金 %%
	COMMISSION_FEE("Commission fee"),    // 百分比手續費
	MINIMAL_COMMISSION_FEE("Minimal CF"),// 最低收續費
	LOT_COMMISSION_FEE("Lot CF"),        // 單手手續費
	//MAXIMUM_LOT("Maximum lot"),     // 最大下單手數
	LIMIT_MAXIMUM_LOT("Limit maximum lot"),  // 現價單最大數量
	MARKET_MAXIMUM_LOT("Market maximum lot"), // 市價單最大數量
	MAXIMUM_HOLD("Maximum hold"),	// 最大持倉量
	PRICE_LIMIT("Price limit"),     // 帳跌停板幅度 %
	DECIMALPOINT("Decimal point"),  // 小數位數
	DENOMINATOR("Denominator"),     // 分母
	NUMERATOR_DP("Numerator DP"),   // 分子小數位數
	STRATEGY("Strategy"),
	SPELL_NAME("Spell name"),
	DETAIL_EN("Detail EN"),
	DETAIL_TW("Detail TW"),
	DETAIL_CN("Detail CN"),
	SPOT_ENNAME("Spot ENName"),
	SPOT_CNNAME("Spot CNName"),
	SPOT_TWNAME("Spot TWName"),
	CODE("Code"),							// WindCode
	INDEX_SESSION_TYPE("Index Session Type"),  // marketSessionType settlement/spot/exchange
	SUBSCRIBE_SYMBOL("Subscribe symbol"),
	INSTRUMENT_TYPE("Instrument type"),
	COMMISSION_TABLE("Commission table"),
	TRADABLE_DAYS("Tradable days"),
	CONTRACT_POLICY("Contract policy")
	;

	static HashMap<String, RefDataField> map = new HashMap<String, RefDataField>();

	private String value;
	RefDataField(String value) {
		this.value = value;
	}
	public String value() {
		return value;
	}

	static public RefDataField getValue(String str) {
		return map.get(str);
	}

	public static void validate() throws Exception {
		map.clear();
		for (RefDataField field: RefDataField.values()) {
			if (map.containsKey(field.value())) {
				throw new Exception("RefDataField duplicated: " + field.value);
			} else {
				map.put(field.value(), field);
			}
		}

	}


}
