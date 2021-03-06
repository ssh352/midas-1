package com.cyanspring.cstw.model.trader;

/**
 * 
 * @author NingXiaoFeng
 * @create date 2015/11/17
 *
 */
public final class ParentOrderModel {

	private String symbol;

	private String side;

	private String quantity;

	private double price;

	private String receiverId;

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getSide() {
		return side;
	}

	public void setSide(String side) {
		this.side = side;
	}

	public String getQuantity() {
		return quantity;
	}

	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public String getReceiverId() {
		return receiverId;
	}

	public void setReceiverId(String receiverId) {
		this.receiverId = receiverId;
	}

}
