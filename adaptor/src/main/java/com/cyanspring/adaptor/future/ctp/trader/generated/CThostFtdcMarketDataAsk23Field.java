package com.cyanspring.adaptor.future.ctp.trader.generated;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * \u884c\u60c5\u7533\u5356\u4e8c\u3001\u4e09\u5c5e\u6027<br>
 * <i>native declaration : ThostFtdcUserApiStruct.h:3986</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("Trader") 
public class CThostFtdcMarketDataAsk23Field extends StructObject {
	static {
		BridJ.register();
	}
	/** C type : TThostFtdcPriceType */
	@Field(0) 
	public double AskPrice2() {
		return this.io.getDoubleField(this, 0);
	}
	/** C type : TThostFtdcPriceType */
	@Field(0) 
	public CThostFtdcMarketDataAsk23Field AskPrice2(double AskPrice2) {
		this.io.setDoubleField(this, 0, AskPrice2);
		return this;
	}
	/** C type : TThostFtdcVolumeType */
	@Field(1) 
	public int AskVolume2() {
		return this.io.getIntField(this, 1);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(1) 
	public CThostFtdcMarketDataAsk23Field AskVolume2(int AskVolume2) {
		this.io.setIntField(this, 1, AskVolume2);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(2) 
	public double AskPrice3() {
		return this.io.getDoubleField(this, 2);
	}
	/** C type : TThostFtdcPriceType */
	@Field(2) 
	public CThostFtdcMarketDataAsk23Field AskPrice3(double AskPrice3) {
		this.io.setDoubleField(this, 2, AskPrice3);
		return this;
	}
	/** C type : TThostFtdcVolumeType */
	@Field(3) 
	public int AskVolume3() {
		return this.io.getIntField(this, 3);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(3) 
	public CThostFtdcMarketDataAsk23Field AskVolume3(int AskVolume3) {
		this.io.setIntField(this, 3, AskVolume3);
		return this;
	}
	public CThostFtdcMarketDataAsk23Field() {
		super();
	}
	public CThostFtdcMarketDataAsk23Field(Pointer pointer) {
		super(pointer);
	}
}
