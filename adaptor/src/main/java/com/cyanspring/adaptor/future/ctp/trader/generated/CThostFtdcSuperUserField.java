package com.cyanspring.adaptor.future.ctp.trader.generated;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * \u7ba1\u7406\u7528\u6237<br>
 * <i>native declaration : ThostFtdcUserApiStruct.h:514</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("Trader") 
public class CThostFtdcSuperUserField extends StructObject {
	static {
		BridJ.register();
	}
	/** C type : TThostFtdcUserIDType */
	@Array({16}) 
	@Field(0) 
	public Pointer<Byte > UserID() {
		return this.io.getPointerField(this, 0);
	}
	/** C type : TThostFtdcUserNameType */
	@Array({81}) 
	@Field(1) 
	public Pointer<Byte > UserName() {
		return this.io.getPointerField(this, 1);
	}
	/** C type : TThostFtdcPasswordType */
	@Array({41}) 
	@Field(2) 
	public Pointer<Byte > Password() {
		return this.io.getPointerField(this, 2);
	}
	/** C type : TThostFtdcBoolType */
	@Field(3) 
	public int IsActive() {
		return this.io.getIntField(this, 3);
	}
	/** C type : TThostFtdcBoolType */
	@Field(3) 
	public CThostFtdcSuperUserField IsActive(int IsActive) {
		this.io.setIntField(this, 3, IsActive);
		return this;
	}
	public CThostFtdcSuperUserField() {
		super();
	}
	public CThostFtdcSuperUserField(Pointer pointer) {
		super(pointer);
	}
}
