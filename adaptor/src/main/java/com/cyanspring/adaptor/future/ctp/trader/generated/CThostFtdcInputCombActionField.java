package com.cyanspring.adaptor.future.ctp.trader.generated;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * \u8f93\u5165\u7684\u7533\u8bf7\u7ec4\u5408<br>
 * <i>native declaration : ThostFtdcUserApiStruct.h:3699</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("Trader") 
public class CThostFtdcInputCombActionField extends StructObject {
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
	public Pointer<Byte > CombActionRef() {
		return this.io.getPointerField(this, 3);
	}
	/** C type : TThostFtdcUserIDType */
	@Array({16}) 
	@Field(4) 
	public Pointer<Byte > UserID() {
		return this.io.getPointerField(this, 4);
	}
	/** C type : TThostFtdcDirectionType */
	@Field(5) 
	public byte Direction() {
		return this.io.getByteField(this, 5);
	}
	/** C type : TThostFtdcDirectionType */
	@Field(5) 
	public CThostFtdcInputCombActionField Direction(byte Direction) {
		this.io.setByteField(this, 5, Direction);
		return this;
	}
	/** C type : TThostFtdcVolumeType */
	@Field(6) 
	public int Volume() {
		return this.io.getIntField(this, 6);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(6) 
	public CThostFtdcInputCombActionField Volume(int Volume) {
		this.io.setIntField(this, 6, Volume);
		return this;
	}
	/** C type : TThostFtdcCombDirectionType */
	@Field(7) 
	public byte CombDirection() {
		return this.io.getByteField(this, 7);
	}
	/** C type : TThostFtdcCombDirectionType */
	@Field(7) 
	public CThostFtdcInputCombActionField CombDirection(byte CombDirection) {
		this.io.setByteField(this, 7, CombDirection);
		return this;
	}
	/** C type : TThostFtdcHedgeFlagType */
	@Field(8) 
	public byte HedgeFlag() {
		return this.io.getByteField(this, 8);
	}
	/** C type : TThostFtdcHedgeFlagType */
	@Field(8) 
	public CThostFtdcInputCombActionField HedgeFlag(byte HedgeFlag) {
		this.io.setByteField(this, 8, HedgeFlag);
		return this;
	}
	public CThostFtdcInputCombActionField() {
		super();
	}
	public CThostFtdcInputCombActionField(Pointer pointer) {
		super(pointer);
	}
}
