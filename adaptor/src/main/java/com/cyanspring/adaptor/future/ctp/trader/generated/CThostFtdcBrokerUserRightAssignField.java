package com.cyanspring.adaptor.future.ctp.trader.generated;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * \u7ecf\u6d4e\u516c\u53f8\u662f\u5426\u6709\u5728\u672c\u6807\u793a\u7684\u4ea4\u6613\u6743\u9650<br>
 * <i>native declaration : ThostFtdcUserApiStruct.h:7538</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("Trader") 
public class CThostFtdcBrokerUserRightAssignField extends StructObject {
	static {
		BridJ.register();
	}
	/** C type : TThostFtdcBrokerIDType */
	@Array({11}) 
	@Field(0) 
	public Pointer<Byte > BrokerID() {
		return this.io.getPointerField(this, 0);
	}
	/** C type : TThostFtdcDRIdentityIDType */
	@Field(1) 
	public int DRIdentityID() {
		return this.io.getIntField(this, 1);
	}
	/** C type : TThostFtdcDRIdentityIDType */
	@Field(1) 
	public CThostFtdcBrokerUserRightAssignField DRIdentityID(int DRIdentityID) {
		this.io.setIntField(this, 1, DRIdentityID);
		return this;
	}
	/** C type : TThostFtdcBoolType */
	@Field(2) 
	public int Tradeable() {
		return this.io.getIntField(this, 2);
	}
	/** C type : TThostFtdcBoolType */
	@Field(2) 
	public CThostFtdcBrokerUserRightAssignField Tradeable(int Tradeable) {
		this.io.setIntField(this, 2, Tradeable);
		return this;
	}
	public CThostFtdcBrokerUserRightAssignField() {
		super();
	}
	public CThostFtdcBrokerUserRightAssignField(Pointer pointer) {
		super(pointer);
	}
}
