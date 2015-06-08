package com.cyanspring.adaptor.future.ctp.trader.generated;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * \u5e02\u573a\u884c\u60c5<br>
 * <i>native declaration : ThostFtdcUserApiStruct.h:3860</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("Trader") 
public class CThostFtdcMarketDataField extends StructObject {
	static {
		BridJ.register();
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(0) 
	public Pointer<Byte > TradingDay() {
		return this.io.getPointerField(this, 0);
	}
	/** C type : TThostFtdcInstrumentIDType */
	@Array({31}) 
	@Field(1) 
	public Pointer<Byte > InstrumentID() {
		return this.io.getPointerField(this, 1);
	}
	/** C type : TThostFtdcExchangeIDType */
	@Array({9}) 
	@Field(2) 
	public Pointer<Byte > ExchangeID() {
		return this.io.getPointerField(this, 2);
	}
	/** C type : TThostFtdcExchangeInstIDType */
	@Array({31}) 
	@Field(3) 
	public Pointer<Byte > ExchangeInstID() {
		return this.io.getPointerField(this, 3);
	}
	/** C type : TThostFtdcPriceType */
	@Field(4) 
	public double LastPrice() {
		return this.io.getDoubleField(this, 4);
	}
	/** C type : TThostFtdcPriceType */
	@Field(4) 
	public CThostFtdcMarketDataField LastPrice(double LastPrice) {
		this.io.setDoubleField(this, 4, LastPrice);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(5) 
	public double PreSettlementPrice() {
		return this.io.getDoubleField(this, 5);
	}
	/** C type : TThostFtdcPriceType */
	@Field(5) 
	public CThostFtdcMarketDataField PreSettlementPrice(double PreSettlementPrice) {
		this.io.setDoubleField(this, 5, PreSettlementPrice);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(6) 
	public double PreClosePrice() {
		return this.io.getDoubleField(this, 6);
	}
	/** C type : TThostFtdcPriceType */
	@Field(6) 
	public CThostFtdcMarketDataField PreClosePrice(double PreClosePrice) {
		this.io.setDoubleField(this, 6, PreClosePrice);
		return this;
	}
	/** C type : TThostFtdcLargeVolumeType */
	@Field(7) 
	public double PreOpenInterest() {
		return this.io.getDoubleField(this, 7);
	}
	/** C type : TThostFtdcLargeVolumeType */
	@Field(7) 
	public CThostFtdcMarketDataField PreOpenInterest(double PreOpenInterest) {
		this.io.setDoubleField(this, 7, PreOpenInterest);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(8) 
	public double OpenPrice() {
		return this.io.getDoubleField(this, 8);
	}
	/** C type : TThostFtdcPriceType */
	@Field(8) 
	public CThostFtdcMarketDataField OpenPrice(double OpenPrice) {
		this.io.setDoubleField(this, 8, OpenPrice);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(9) 
	public double HighestPrice() {
		return this.io.getDoubleField(this, 9);
	}
	/** C type : TThostFtdcPriceType */
	@Field(9) 
	public CThostFtdcMarketDataField HighestPrice(double HighestPrice) {
		this.io.setDoubleField(this, 9, HighestPrice);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(10) 
	public double LowestPrice() {
		return this.io.getDoubleField(this, 10);
	}
	/** C type : TThostFtdcPriceType */
	@Field(10) 
	public CThostFtdcMarketDataField LowestPrice(double LowestPrice) {
		this.io.setDoubleField(this, 10, LowestPrice);
		return this;
	}
	/** C type : TThostFtdcVolumeType */
	@Field(11) 
	public int Volume() {
		return this.io.getIntField(this, 11);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(11) 
	public CThostFtdcMarketDataField Volume(int Volume) {
		this.io.setIntField(this, 11, Volume);
		return this;
	}
	/** C type : TThostFtdcMoneyType */
	@Field(12) 
	public double Turnover() {
		return this.io.getDoubleField(this, 12);
	}
	/** C type : TThostFtdcMoneyType */
	@Field(12) 
	public CThostFtdcMarketDataField Turnover(double Turnover) {
		this.io.setDoubleField(this, 12, Turnover);
		return this;
	}
	/** C type : TThostFtdcLargeVolumeType */
	@Field(13) 
	public double OpenInterest() {
		return this.io.getDoubleField(this, 13);
	}
	/** C type : TThostFtdcLargeVolumeType */
	@Field(13) 
	public CThostFtdcMarketDataField OpenInterest(double OpenInterest) {
		this.io.setDoubleField(this, 13, OpenInterest);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(14) 
	public double ClosePrice() {
		return this.io.getDoubleField(this, 14);
	}
	/** C type : TThostFtdcPriceType */
	@Field(14) 
	public CThostFtdcMarketDataField ClosePrice(double ClosePrice) {
		this.io.setDoubleField(this, 14, ClosePrice);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(15) 
	public double SettlementPrice() {
		return this.io.getDoubleField(this, 15);
	}
	/** C type : TThostFtdcPriceType */
	@Field(15) 
	public CThostFtdcMarketDataField SettlementPrice(double SettlementPrice) {
		this.io.setDoubleField(this, 15, SettlementPrice);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(16) 
	public double UpperLimitPrice() {
		return this.io.getDoubleField(this, 16);
	}
	/** C type : TThostFtdcPriceType */
	@Field(16) 
	public CThostFtdcMarketDataField UpperLimitPrice(double UpperLimitPrice) {
		this.io.setDoubleField(this, 16, UpperLimitPrice);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(17) 
	public double LowerLimitPrice() {
		return this.io.getDoubleField(this, 17);
	}
	/** C type : TThostFtdcPriceType */
	@Field(17) 
	public CThostFtdcMarketDataField LowerLimitPrice(double LowerLimitPrice) {
		this.io.setDoubleField(this, 17, LowerLimitPrice);
		return this;
	}
	/** C type : TThostFtdcRatioType */
	@Field(18) 
	public double PreDelta() {
		return this.io.getDoubleField(this, 18);
	}
	/** C type : TThostFtdcRatioType */
	@Field(18) 
	public CThostFtdcMarketDataField PreDelta(double PreDelta) {
		this.io.setDoubleField(this, 18, PreDelta);
		return this;
	}
	/** C type : TThostFtdcRatioType */
	@Field(19) 
	public double CurrDelta() {
		return this.io.getDoubleField(this, 19);
	}
	/** C type : TThostFtdcRatioType */
	@Field(19) 
	public CThostFtdcMarketDataField CurrDelta(double CurrDelta) {
		this.io.setDoubleField(this, 19, CurrDelta);
		return this;
	}
	/** C type : TThostFtdcTimeType */
	@Array({9}) 
	@Field(20) 
	public Pointer<Byte > UpdateTime() {
		return this.io.getPointerField(this, 20);
	}
	/** C type : TThostFtdcMillisecType */
	@Field(21) 
	public int UpdateMillisec() {
		return this.io.getIntField(this, 21);
	}
	/** C type : TThostFtdcMillisecType */
	@Field(21) 
	public CThostFtdcMarketDataField UpdateMillisec(int UpdateMillisec) {
		this.io.setIntField(this, 21, UpdateMillisec);
		return this;
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(22) 
	public Pointer<Byte > ActionDay() {
		return this.io.getPointerField(this, 22);
	}
	public CThostFtdcMarketDataField() {
		super();
	}
	public CThostFtdcMarketDataField(Pointer pointer) {
		super(pointer);
	}
}
