package com.cyanspring.adaptor.future.ctp.trader.generated;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * \u5408\u7ea6<br>
 * <i>native declaration : ThostFtdcUserApiStruct.h:356</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("Trader") 
public class CThostFtdcInstrumentField extends StructObject {
	static {
		BridJ.register();
	}
	/** C type : TThostFtdcInstrumentIDType */
	@Array({31}) 
	@Field(0) 
	public Pointer<Byte > InstrumentID() {
		return this.io.getPointerField(this, 0);
	}
	/** C type : TThostFtdcExchangeIDType */
	@Array({9}) 
	@Field(1) 
	public Pointer<Byte > ExchangeID() {
		return this.io.getPointerField(this, 1);
	}
	/** C type : TThostFtdcInstrumentNameType */
	@Array({21}) 
	@Field(2) 
	public Pointer<Byte > InstrumentName() {
		return this.io.getPointerField(this, 2);
	}
	/** C type : TThostFtdcExchangeInstIDType */
	@Array({31}) 
	@Field(3) 
	public Pointer<Byte > ExchangeInstID() {
		return this.io.getPointerField(this, 3);
	}
	/** C type : TThostFtdcInstrumentIDType */
	@Array({31}) 
	@Field(4) 
	public Pointer<Byte > ProductID() {
		return this.io.getPointerField(this, 4);
	}
	/** C type : TThostFtdcProductClassType */
	@Field(5) 
	public byte ProductClass() {
		return this.io.getByteField(this, 5);
	}
	/** C type : TThostFtdcProductClassType */
	@Field(5) 
	public CThostFtdcInstrumentField ProductClass(byte ProductClass) {
		this.io.setByteField(this, 5, ProductClass);
		return this;
	}
	/** C type : TThostFtdcYearType */
	@Field(6) 
	public int DeliveryYear() {
		return this.io.getIntField(this, 6);
	}
	/** C type : TThostFtdcYearType */
	@Field(6) 
	public CThostFtdcInstrumentField DeliveryYear(int DeliveryYear) {
		this.io.setIntField(this, 6, DeliveryYear);
		return this;
	}
	/** C type : TThostFtdcMonthType */
	@Field(7) 
	public int DeliveryMonth() {
		return this.io.getIntField(this, 7);
	}
	/** C type : TThostFtdcMonthType */
	@Field(7) 
	public CThostFtdcInstrumentField DeliveryMonth(int DeliveryMonth) {
		this.io.setIntField(this, 7, DeliveryMonth);
		return this;
	}
	/** C type : TThostFtdcVolumeType */
	@Field(8) 
	public int MaxMarketOrderVolume() {
		return this.io.getIntField(this, 8);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(8) 
	public CThostFtdcInstrumentField MaxMarketOrderVolume(int MaxMarketOrderVolume) {
		this.io.setIntField(this, 8, MaxMarketOrderVolume);
		return this;
	}
	/** C type : TThostFtdcVolumeType */
	@Field(9) 
	public int MinMarketOrderVolume() {
		return this.io.getIntField(this, 9);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(9) 
	public CThostFtdcInstrumentField MinMarketOrderVolume(int MinMarketOrderVolume) {
		this.io.setIntField(this, 9, MinMarketOrderVolume);
		return this;
	}
	/** C type : TThostFtdcVolumeType */
	@Field(10) 
	public int MaxLimitOrderVolume() {
		return this.io.getIntField(this, 10);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(10) 
	public CThostFtdcInstrumentField MaxLimitOrderVolume(int MaxLimitOrderVolume) {
		this.io.setIntField(this, 10, MaxLimitOrderVolume);
		return this;
	}
	/** C type : TThostFtdcVolumeType */
	@Field(11) 
	public int MinLimitOrderVolume() {
		return this.io.getIntField(this, 11);
	}
	/** C type : TThostFtdcVolumeType */
	@Field(11) 
	public CThostFtdcInstrumentField MinLimitOrderVolume(int MinLimitOrderVolume) {
		this.io.setIntField(this, 11, MinLimitOrderVolume);
		return this;
	}
	/** C type : TThostFtdcVolumeMultipleType */
	@Field(12) 
	public int VolumeMultiple() {
		return this.io.getIntField(this, 12);
	}
	/** C type : TThostFtdcVolumeMultipleType */
	@Field(12) 
	public CThostFtdcInstrumentField VolumeMultiple(int VolumeMultiple) {
		this.io.setIntField(this, 12, VolumeMultiple);
		return this;
	}
	/** C type : TThostFtdcPriceType */
	@Field(13) 
	public double PriceTick() {
		return this.io.getDoubleField(this, 13);
	}
	/** C type : TThostFtdcPriceType */
	@Field(13) 
	public CThostFtdcInstrumentField PriceTick(double PriceTick) {
		this.io.setDoubleField(this, 13, PriceTick);
		return this;
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(14) 
	public Pointer<Byte > CreateDate() {
		return this.io.getPointerField(this, 14);
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(15) 
	public Pointer<Byte > OpenDate() {
		return this.io.getPointerField(this, 15);
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(16) 
	public Pointer<Byte > ExpireDate() {
		return this.io.getPointerField(this, 16);
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(17) 
	public Pointer<Byte > StartDelivDate() {
		return this.io.getPointerField(this, 17);
	}
	/** C type : TThostFtdcDateType */
	@Array({9}) 
	@Field(18) 
	public Pointer<Byte > EndDelivDate() {
		return this.io.getPointerField(this, 18);
	}
	/** C type : TThostFtdcInstLifePhaseType */
	@Field(19) 
	public byte InstLifePhase() {
		return this.io.getByteField(this, 19);
	}
	/** C type : TThostFtdcInstLifePhaseType */
	@Field(19) 
	public CThostFtdcInstrumentField InstLifePhase(byte InstLifePhase) {
		this.io.setByteField(this, 19, InstLifePhase);
		return this;
	}
	/** C type : TThostFtdcBoolType */
	@Field(20) 
	public int IsTrading() {
		return this.io.getIntField(this, 20);
	}
	/** C type : TThostFtdcBoolType */
	@Field(20) 
	public CThostFtdcInstrumentField IsTrading(int IsTrading) {
		this.io.setIntField(this, 20, IsTrading);
		return this;
	}
	/** C type : TThostFtdcPositionTypeType */
	@Field(21) 
	public byte PositionType() {
		return this.io.getByteField(this, 21);
	}
	/** C type : TThostFtdcPositionTypeType */
	@Field(21) 
	public CThostFtdcInstrumentField PositionType(byte PositionType) {
		this.io.setByteField(this, 21, PositionType);
		return this;
	}
	/** C type : TThostFtdcPositionDateTypeType */
	@Field(22) 
	public byte PositionDateType() {
		return this.io.getByteField(this, 22);
	}
	/** C type : TThostFtdcPositionDateTypeType */
	@Field(22) 
	public CThostFtdcInstrumentField PositionDateType(byte PositionDateType) {
		this.io.setByteField(this, 22, PositionDateType);
		return this;
	}
	/** C type : TThostFtdcRatioType */
	@Field(23) 
	public double LongMarginRatio() {
		return this.io.getDoubleField(this, 23);
	}
	/** C type : TThostFtdcRatioType */
	@Field(23) 
	public CThostFtdcInstrumentField LongMarginRatio(double LongMarginRatio) {
		this.io.setDoubleField(this, 23, LongMarginRatio);
		return this;
	}
	/** C type : TThostFtdcRatioType */
	@Field(24) 
	public double ShortMarginRatio() {
		return this.io.getDoubleField(this, 24);
	}
	/** C type : TThostFtdcRatioType */
	@Field(24) 
	public CThostFtdcInstrumentField ShortMarginRatio(double ShortMarginRatio) {
		this.io.setDoubleField(this, 24, ShortMarginRatio);
		return this;
	}
	/** C type : TThostFtdcMaxMarginSideAlgorithmType */
	@Field(25) 
	public byte MaxMarginSideAlgorithm() {
		return this.io.getByteField(this, 25);
	}
	/** C type : TThostFtdcMaxMarginSideAlgorithmType */
	@Field(25) 
	public CThostFtdcInstrumentField MaxMarginSideAlgorithm(byte MaxMarginSideAlgorithm) {
		this.io.setByteField(this, 25, MaxMarginSideAlgorithm);
		return this;
	}
	/** C type : TThostFtdcInstrumentIDType */
	@Array({31}) 
	@Field(26) 
	public Pointer<Byte > UnderlyingInstrID() {
		return this.io.getPointerField(this, 26);
	}
	/** C type : TThostFtdcPriceType */
	@Field(27) 
	public double StrikePrice() {
		return this.io.getDoubleField(this, 27);
	}
	/** C type : TThostFtdcPriceType */
	@Field(27) 
	public CThostFtdcInstrumentField StrikePrice(double StrikePrice) {
		this.io.setDoubleField(this, 27, StrikePrice);
		return this;
	}
	/** C type : TThostFtdcOptionsTypeType */
	@Field(28) 
	public byte OptionsType() {
		return this.io.getByteField(this, 28);
	}
	/** C type : TThostFtdcOptionsTypeType */
	@Field(28) 
	public CThostFtdcInstrumentField OptionsType(byte OptionsType) {
		this.io.setByteField(this, 28, OptionsType);
		return this;
	}
	/** C type : TThostFtdcUnderlyingMultipleType */
	@Field(29) 
	public double UnderlyingMultiple() {
		return this.io.getDoubleField(this, 29);
	}
	/** C type : TThostFtdcUnderlyingMultipleType */
	@Field(29) 
	public CThostFtdcInstrumentField UnderlyingMultiple(double UnderlyingMultiple) {
		this.io.setDoubleField(this, 29, UnderlyingMultiple);
		return this;
	}
	/** C type : TThostFtdcCombinationTypeType */
	@Field(30) 
	public byte CombinationType() {
		return this.io.getByteField(this, 30);
	}
	/** C type : TThostFtdcCombinationTypeType */
	@Field(30) 
	public CThostFtdcInstrumentField CombinationType(byte CombinationType) {
		this.io.setByteField(this, 30, CombinationType);
		return this;
	}
	public CThostFtdcInstrumentField() {
		super();
	}
	public CThostFtdcInstrumentField(Pointer pointer) {
		super(pointer);
	}
}
