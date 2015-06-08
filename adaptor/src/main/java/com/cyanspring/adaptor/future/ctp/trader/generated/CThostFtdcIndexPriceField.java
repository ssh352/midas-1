package com.cyanspring.adaptor.future.ctp.trader.generated;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * \u80a1\u6307\u73b0\u8d27\u6307\u6570<br>
 * <i>native declaration : ThostFtdcUserApiStruct.h:2655</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("Trader") 
public class CThostFtdcIndexPriceField extends StructObject {
	static {
		BridJ.register();
	}
	/** C type : TThostFtdcBrokerIDType */
	@Array({11}) 
	@Field(0) 
	public Pointer<Byte > BrokerID() {
		return this.io.getPointerField(this, 0);
	}
	/** C type : TThostFtdcInstrumentIDType */
	@Array({31}) 
	@Field(1) 
	public Pointer<Byte > InstrumentID() {
		return this.io.getPointerField(this, 1);
	}
	/** C type : TThostFtdcPriceType */
	@Field(2) 
	public double ClosePrice() {
		return this.io.getDoubleField(this, 2);
	}
	/** C type : TThostFtdcPriceType */
	@Field(2) 
	public CThostFtdcIndexPriceField ClosePrice(double ClosePrice) {
		this.io.setDoubleField(this, 2, ClosePrice);
		return this;
	}
	public CThostFtdcIndexPriceField() {
		super();
	}
	public CThostFtdcIndexPriceField(Pointer pointer) {
		super(pointer);
	}
}
