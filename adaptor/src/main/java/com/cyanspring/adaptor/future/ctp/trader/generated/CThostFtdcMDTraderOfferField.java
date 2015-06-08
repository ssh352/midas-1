package com.cyanspring.adaptor.future.ctp.trader.generated;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * \u4ea4\u6613\u6240\u884c\u60c5\u62a5\u76d8\u673a<br>
 * <i>native declaration : ThostFtdcUserApiStruct.h:4226</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("Trader") 
public class CThostFtdcMDTraderOfferField extends StructObject {
	static {
		BridJ.register();
	}
	/** C type : TThostFtdcExchangeIDType */
	@Array({9}) 
	@Field(0) 
	public Pointer<Byte > ExchangeID() {
		return this.io.getPointerField(this, 0);
	}
	/** C type : TThostFtdcTraderIDType */
	@Array({21}) 
	@Field(1) 
	public Pointer<Byte > TraderID() {
		return this.io.getPointerField(this, 1);
	}
	/** C type : TThostFtdcParticipantIDType */
	@Array({11}) 
	@Field(2) 
	public Pointer<Byte > ParticipantID() {
		return this.io.getPointerField(this, 2);
	}
	/** C type : TThostFtdcPasswordType */
	@Array({41}) 
	@Field(3) 
	public Pointer<Byte > Password() {
		return this.io.getPointerField(this, 3);
	}
	/** C type : TThostFtdcInstallIDType */
	@Field(4) 
	public int InstallID() {
		return this.io.getIntField(this, 4);
	}
	/** C type : TThostFtdcInstallIDType */
	@Field(4) 
	public CThostFtdcMDTraderOfferField InstallID(int InstallID) {
		this.io.setIntField(this, 4, InstallID);
		return this;
	}
	/** C type : TThostFtdcOrderLocalIDType */
	@Array({13}) 
	@Field(5) 
	public Pointer<Byte > OrderLocalID() {
		return this.io.getPointerField(this, 5);
	}
	/** C type : TThostFtdcTraderConnectStatusType */
	@Field(6) 
	public byte TraderConnectStatus() {
		return this.io.getByteField(this, 6);
	}
	/** C type : TThostFtdcTraderConnectStatusType */
	@Field(6) 
	public CThostFtdcMDTraderOfferField TraderConnectStatus(byte TraderConnectStatus) {
		this.io.setByteField(this, 6, TraderConnectStatus);
		return this;
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(7) 
	public Pointer<Byte > ConnectRequestDate() {
		return this.io.getPointerField(this, 7);
	}
	/** C type : TThostFtdcTimeType */
	@Array({9}) 
	@Field(8) 
	public Pointer<Byte > ConnectRequestTime() {
		return this.io.getPointerField(this, 8);
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(9) 
	public Pointer<Byte > LastReportDate() {
		return this.io.getPointerField(this, 9);
	}
	/** C type : TThostFtdcTimeType */
	@Array({9}) 
	@Field(10) 
	public Pointer<Byte > LastReportTime() {
		return this.io.getPointerField(this, 10);
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(11) 
	public Pointer<Byte > ConnectDate() {
		return this.io.getPointerField(this, 11);
	}
	/** C type : TThostFtdcTimeType */
	@Array({9}) 
	@Field(12) 
	public Pointer<Byte > ConnectTime() {
		return this.io.getPointerField(this, 12);
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(13) 
	public Pointer<Byte > StartDate() {
		return this.io.getPointerField(this, 13);
	}
	/** C type : TThostFtdcTimeType */
	@Array({9}) 
	@Field(14) 
	public Pointer<Byte > StartTime() {
		return this.io.getPointerField(this, 14);
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(15) 
	public Pointer<Byte > TradingDay() {
		return this.io.getPointerField(this, 15);
	}
	/** C type : TThostFtdcBrokerIDType */
	@Array({11}) 
	@Field(16) 
	public Pointer<Byte > BrokerID() {
		return this.io.getPointerField(this, 16);
	}
	/** C type : TThostFtdcTradeIDType */
	@Array({21}) 
	@Field(17) 
	public Pointer<Byte > MaxTradeID() {
		return this.io.getPointerField(this, 17);
	}
	/** C type : TThostFtdcReturnCodeType */
	@Array({7}) 
	@Field(18) 
	public Pointer<Byte > MaxOrderMessageReference() {
		return this.io.getPointerField(this, 18);
	}
	public CThostFtdcMDTraderOfferField() {
		super();
	}
	public CThostFtdcMDTraderOfferField(Pointer pointer) {
		super(pointer);
	}
}
