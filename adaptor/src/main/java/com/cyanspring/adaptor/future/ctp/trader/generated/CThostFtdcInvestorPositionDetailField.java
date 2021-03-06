package com.cyanspring.adaptor.future.ctp.trader.generated;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * \u6295\u8d44\u8005\u6301\u4ed3\u660e\u7ec6<br>
 * <i>native declaration : ThostFtdcUserApiStruct.h:4156</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("Trader") 
public class CThostFtdcInvestorPositionDetailField extends StructObject {
	static {
		BridJ.register();
	}
	/** C type : TThostFtdcInstrumentIDType */
	@Array({31}) 
	@Field(0) 
	public Pointer<Byte > InstrumentID() {
		return this.io.getPointerField(this, 0);
	}
	/** C type : TThostFtdcBrokerIDType */
	@Array({11}) 
	@Field(1) 
	public Pointer<Byte > BrokerID() {
		return this.io.getPointerField(this, 1);
	}
	/** C type : TThostFtdcInvestorIDType */
	@Array({13}) 
	@Field(2) 
	public Pointer<Byte > InvestorID() {
		return this.io.getPointerField(this, 2);
	}
	/** C type : TThostFtdcHedgeFlagType */
	@Field(3) 
	public byte HedgeFlag() {
		return this.io.getByteField(this, 3);
	}
	/** C type : TThostFtdcHedgeFlagType */
	@Field(3) 
	public CThostFtdcInvestorPositionDetailField HedgeFlag(byte HedgeFlag) {
		this.io.setByteField(this, 3, HedgeFlag);
		return this;
	}
	/** C type : TThostFtdcDirectionType */
	@Field(4) 
	public byte Direction() {
		return this.io.getByteField(this, 4);
	}
	/** C type : TThostFtdcDirectionType */
	@Field(4) 
	public CThostFtdcInvestorPositionDetailField Direction(byte Direction) {
		this.io.setByteField(this, 4, Direction);
		return this;
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(5) 
	public Pointer<Byte > OpenDate() {
		return this.io.getPointerField(this, 5);
	}
	/** C type : TThostFtdcTradeIDType */
	@Array({21}) 
	@Field(6) 
	public Pointer<Byte > TradeID() {
		return this.io.getPointerField(this, 6);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(7) 
	public int Volume() {
		return this.io.getIntField(this, 7);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(7) 
	public CThostFtdcInvestorPositionDetailField Volume(int Volume) {
		this.io.setIntField(this, 7, Volume);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(8) 
	public double OpenPrice() {
		return this.io.getDoubleField(this, 8);
	}
	/** C type : TThostFtdcPriceType */
	@Field(8) 
	public CThostFtdcInvestorPositionDetailField OpenPrice(double OpenPrice) {
		this.io.setDoubleField(this, 8, OpenPrice);
		return this;
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(9) 
	public Pointer<Byte > TradingDay() {
		return this.io.getPointerField(this, 9);
	}
	/** C type : TThostFtdcSettlementIDType */
	@Field(10) 
	public int SettlementID() {
		return this.io.getIntField(this, 10);
	}
	/** C type : TThostFtdcSettlementIDType */
	@Field(10) 
	public CThostFtdcInvestorPositionDetailField SettlementID(int SettlementID) {
		this.io.setIntField(this, 10, SettlementID);
		return this;
	}
	/** C type : TThostFtdcTradeTypeType */
	@Field(11) 
	public byte TradeType() {
		return this.io.getByteField(this, 11);
	}
	/** C type : TThostFtdcTradeTypeType */
	@Field(11) 
	public CThostFtdcInvestorPositionDetailField TradeType(byte TradeType) {
		this.io.setByteField(this, 11, TradeType);
		return this;
	}
	/** C type : TThostFtdcInstrumentIDType */
	@Array({31}) 
	@Field(12) 
	public Pointer<Byte > CombInstrumentID() {
		return this.io.getPointerField(this, 12);
	}
	/** C type : TThostFtdcExchangeIDType */
	@Array({9}) 
	@Field(13) 
	public Pointer<Byte > ExchangeID() {
		return this.io.getPointerField(this, 13);
	}
	/** C type : TThostFtdcMoneyType */
	@Field(14) 
	public double CloseProfitByDate() {
		return this.io.getDoubleField(this, 14);
	}
	/** C type : TThostFtdcMoneyType */
	@Field(14) 
	public CThostFtdcInvestorPositionDetailField CloseProfitByDate(double CloseProfitByDate) {
		this.io.setDoubleField(this, 14, CloseProfitByDate);
		return this;
	}
	/** C type : TThostFtdcMoneyType */
	@Field(15) 
	public double CloseProfitByTrade() {
		return this.io.getDoubleField(this, 15);
	}
	/** C type : TThostFtdcMoneyType */
	@Field(15) 
	public CThostFtdcInvestorPositionDetailField CloseProfitByTrade(double CloseProfitByTrade) {
		this.io.setDoubleField(this, 15, CloseProfitByTrade);
		return this;
	}
	/** C type : TThostFtdcMoneyType */
	@Field(16) 
	public double PositionProfitByDate() {
		return this.io.getDoubleField(this, 16);
	}
	/** C type : TThostFtdcMoneyType */
	@Field(16) 
	public CThostFtdcInvestorPositionDetailField PositionProfitByDate(double PositionProfitByDate) {
		this.io.setDoubleField(this, 16, PositionProfitByDate);
		return this;
	}
	/** C type : TThostFtdcMoneyType */
	@Field(17) 
	public double PositionProfitByTrade() {
		return this.io.getDoubleField(this, 17);
	}
	/** C type : TThostFtdcMoneyType */
	@Field(17) 
	public CThostFtdcInvestorPositionDetailField PositionProfitByTrade(double PositionProfitByTrade) {
		this.io.setDoubleField(this, 17, PositionProfitByTrade);
		return this;
	}
	/** C type : TThostFtdcMoneyType */
	@Field(18) 
	public double Margin() {
		return this.io.getDoubleField(this, 18);
	}
	/** C type : TThostFtdcMoneyType */
	@Field(18) 
	public CThostFtdcInvestorPositionDetailField Margin(double Margin) {
		this.io.setDoubleField(this, 18, Margin);
		return this;
	}
	/** C type : TThostFtdcMoneyType */
	@Field(19) 
	public double ExchMargin() {
		return this.io.getDoubleField(this, 19);
	}
	/** C type : TThostFtdcMoneyType */
	@Field(19) 
	public CThostFtdcInvestorPositionDetailField ExchMargin(double ExchMargin) {
		this.io.setDoubleField(this, 19, ExchMargin);
		return this;
	}
	/** C type : TThostFtdcRatioType */
	@Field(20) 
	public double MarginRateByMoney() {
		return this.io.getDoubleField(this, 20);
	}
	/** C type : TThostFtdcRatioType */
	@Field(20) 
	public CThostFtdcInvestorPositionDetailField MarginRateByMoney(double MarginRateByMoney) {
		this.io.setDoubleField(this, 20, MarginRateByMoney);
		return this;
	}
	/** C type : TThostFtdcRatioType */
	@Field(21) 
	public double MarginRateByVolume() {
		return this.io.getDoubleField(this, 21);
	}
	/** C type : TThostFtdcRatioType */
	@Field(21) 
	public CThostFtdcInvestorPositionDetailField MarginRateByVolume(double MarginRateByVolume) {
		this.io.setDoubleField(this, 21, MarginRateByVolume);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(22) 
	public double LastSettlementPrice() {
		return this.io.getDoubleField(this, 22);
	}
	/** C type : TThostFtdcPriceType */
	@Field(22) 
	public CThostFtdcInvestorPositionDetailField LastSettlementPrice(double LastSettlementPrice) {
		this.io.setDoubleField(this, 22, LastSettlementPrice);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(23) 
	public double SettlementPrice() {
		return this.io.getDoubleField(this, 23);
	}
	/** C type : TThostFtdcPriceType */
	@Field(23) 
	public CThostFtdcInvestorPositionDetailField SettlementPrice(double SettlementPrice) {
		this.io.setDoubleField(this, 23, SettlementPrice);
		return this;
	}
	/** C type : TThostFtdcVolumeType */
	@Field(24) 
	public int CloseVolume() {
		return this.io.getIntField(this, 24);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(24) 
	public CThostFtdcInvestorPositionDetailField CloseVolume(int CloseVolume) {
		this.io.setIntField(this, 24, CloseVolume);
		return this;
	}
	/** C type : TThostFtdcMoneyType */
	@Field(25) 
	public double CloseAmount() {
		return this.io.getDoubleField(this, 25);
	}
	/** C type : TThostFtdcMoneyType */
	@Field(25) 
	public CThostFtdcInvestorPositionDetailField CloseAmount(double CloseAmount) {
		this.io.setDoubleField(this, 25, CloseAmount);
		return this;
	}
	public CThostFtdcInvestorPositionDetailField() {
		super();
	}
	public CThostFtdcInvestorPositionDetailField(Pointer pointer) {
		super(pointer);
	}
}
