package com.cyanspring.adaptor.future.ctp.trader.generated;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * \u9519\u8bef\u6267\u884c\u5ba3\u544a<br>
 * <i>native declaration : ThostFtdcUserApiStruct.h:3014</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("Trader") 
public class CThostFtdcErrExecOrderField extends StructObject {
	static {
		BridJ.register();
	}
	/** C type : TThostFtdcBrokerIDType */
	@Array({11}) 
	@Field(0) 
	public Pointer<Byte > BrokerID() {
		return this.io.getPointerField(this, 0);
	}
	/** C type : TThostFtdcInvestorIDType */
	@Array({13}) 
	@Field(1) 
	public Pointer<Byte > InvestorID() {
		return this.io.getPointerField(this, 1);
	}
	/** C type : TThostFtdcInstrumentIDType */
	@Array({31}) 
	@Field(2) 
	public Pointer<Byte > InstrumentID() {
		return this.io.getPointerField(this, 2);
	}
	/** C type : TThostFtdcOrderRefType */
	@Array({13}) 
	@Field(3) 
	public Pointer<Byte > ExecOrderRef() {
		return this.io.getPointerField(this, 3);
	}
	/** C type : TThostFtdcUserIDType */
	@Array({16}) 
	@Field(4) 
	public Pointer<Byte > UserID() {
		return this.io.getPointerField(this, 4);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(5) 
	public int Volume() {
		return this.io.getIntField(this, 5);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(5) 
	public CThostFtdcErrExecOrderField Volume(int Volume) {
		this.io.setIntField(this, 5, Volume);
		return this;
	}
	/** C type : TThostFtdcRequestIDType */
	@Field(6) 
	public int RequestID() {
		return this.io.getIntField(this, 6);
	}
	/** C type : TThostFtdcRequestIDType */
	@Field(6) 
	public CThostFtdcErrExecOrderField RequestID(int RequestID) {
		this.io.setIntField(this, 6, RequestID);
		return this;
	}
	/** C type : TThostFtdcBusinessUnitType */
	@Array({21}) 
	@Field(7) 
	public Pointer<Byte > BusinessUnit() {
		return this.io.getPointerField(this, 7);
	}
	/** C type : TThostFtdcOffsetFlagType */
	@Field(8) 
	public byte OffsetFlag() {
		return this.io.getByteField(this, 8);
	}
	/** C type : TThostFtdcOffsetFlagType */
	@Field(8) 
	public CThostFtdcErrExecOrderField OffsetFlag(byte OffsetFlag) {
		this.io.setByteField(this, 8, OffsetFlag);
		return this;
	}
	/** C type : TThostFtdcHedgeFlagType */
	@Field(9) 
	public byte HedgeFlag() {
		return this.io.getByteField(this, 9);
	}
	/** C type : TThostFtdcHedgeFlagType */
	@Field(9) 
	public CThostFtdcErrExecOrderField HedgeFlag(byte HedgeFlag) {
		this.io.setByteField(this, 9, HedgeFlag);
		return this;
	}
	/** C type : TThostFtdcActionTypeType */
	@Field(10) 
	public byte ActionType() {
		return this.io.getByteField(this, 10);
	}
	/** C type : TThostFtdcActionTypeType */
	@Field(10) 
	public CThostFtdcErrExecOrderField ActionType(byte ActionType) {
		this.io.setByteField(this, 10, ActionType);
		return this;
	}
	/** C type : TThostFtdcPosiDirectionType */
	@Field(11) 
	public byte PosiDirection() {
		return this.io.getByteField(this, 11);
	}
	/** C type : TThostFtdcPosiDirectionType */
	@Field(11) 
	public CThostFtdcErrExecOrderField PosiDirection(byte PosiDirection) {
		this.io.setByteField(this, 11, PosiDirection);
		return this;
	}
	/** C type : TThostFtdcExecOrderPositionFlagType */
	@Field(12) 
	public byte ReservePositionFlag() {
		return this.io.getByteField(this, 12);
	}
	/** C type : TThostFtdcExecOrderPositionFlagType */
	@Field(12) 
	public CThostFtdcErrExecOrderField ReservePositionFlag(byte ReservePositionFlag) {
		this.io.setByteField(this, 12, ReservePositionFlag);
		return this;
	}
	/** C type : TThostFtdcExecOrderCloseFlagType */
	@Field(13) 
	public byte CloseFlag() {
		return this.io.getByteField(this, 13);
	}
	/** C type : TThostFtdcExecOrderCloseFlagType */
	@Field(13) 
	public CThostFtdcErrExecOrderField CloseFlag(byte CloseFlag) {
		this.io.setByteField(this, 13, CloseFlag);
		return this;
	}
	/** C type : TThostFtdcErrorIDType */
	@Field(14) 
	public int ErrorID() {
		return this.io.getIntField(this, 14);
	}
	/** C type : TThostFtdcErrorIDType */
	@Field(14) 
	public CThostFtdcErrExecOrderField ErrorID(int ErrorID) {
		this.io.setIntField(this, 14, ErrorID);
		return this;
	}
	/** C type : TThostFtdcErrorMsgType */
	@Array({81}) 
	@Field(15) 
	public Pointer<Byte > ErrorMsg() {
		return this.io.getPointerField(this, 15);
	}
	public CThostFtdcErrExecOrderField() {
		super();
	}
	public CThostFtdcErrExecOrderField(Pointer pointer) {
		super(pointer);
	}
}
