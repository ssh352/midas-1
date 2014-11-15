package com.cyanspring.common.account;

import com.cyanspring.common.business.Execution;
import com.cyanspring.common.fx.FxUtils;
import com.cyanspring.common.fx.IFxConverter;
import com.cyanspring.common.staticdata.RefDataManager;

public class ClosedPosition extends Position {
	private double buyPrice;
	private double sellPrice; // close price is always sell price
	private double acPnL;

	protected ClosedPosition() {
		
	}
	
	public double getBuyPrice() {
		return buyPrice;
	}

	protected void setBuyPrice(double buyPrice) {
		this.buyPrice = buyPrice;
	}

	public double getSellPrice() {
		return sellPrice;
	}

	protected void setSellPrice(double closePrice) {
		this.sellPrice = closePrice;
	}
	
	public double getAcPnL() {
		return acPnL;
	}

	protected void setAcPnL(double acPnL) {
		this.acPnL = acPnL;
	}
	
	static public ClosedPosition create(RefDataManager refDataManager, IFxConverter fxConverter, 
			OpenPosition position, Execution execution, Account account) {
		ClosedPosition result = new ClosedPosition();
		result.setId(position.getId() + "_" + execution.getId());
		result.setUser(position.getUser());
		result.setAccount(position.getAccount());
		result.setSymbol(position.getSymbol());
		result.setQty(Math.abs(position.getQty()));
		
		if(position.isBuy()) {
			result.setBuyPrice(position.getPrice());
			result.setSellPrice(execution.getPrice());
		} else {
			result.setBuyPrice(execution.getPrice());
			result.setSellPrice(position.getPrice());
		}
		result.setPnL((result.getSellPrice() - result.getBuyPrice()) * result.getQty());
		result.setAcPnL(FxUtils.convertPnLToCurrency(refDataManager, fxConverter, 
				account.getCurrency(), position.getSymbol(), result.getPnL()));
		result.setCreated(execution.getCreated());
		return result;
	}

	@Override
	protected String formatString() {
		return super.formatString() + ", " + sellPrice;
	}

	@Override
	public ClosedPosition clone() throws CloneNotSupportedException {
		return (ClosedPosition)super.clone();
	}

}
