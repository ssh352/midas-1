package com.cyanspring.server.account;

import java.util.HashMap;
import java.util.Map;

import com.cyanspring.common.Default;
import webcurve.util.PriceUtils;

import com.cyanspring.common.account.AccountSetting;
import com.cyanspring.common.account.ICommissionManager;
import com.cyanspring.common.business.Execution;
import com.cyanspring.common.staticdata.RefData;

public class CommissionManager implements ICommissionManager{
	private Map<String, Double> commissionByMarket = new HashMap<>();
	private Map<String, Double> commissionByExchange = new HashMap<>();
	private double minCommissionFee = 2;

	@Override
	public double getCommission(RefData refData, AccountSetting settings, Execution execution) {
		return getCommission(refData, settings, 0, execution);
	}

	@Override
	public double getCommission(RefData refData, AccountSetting settings, double value, Execution execution) {
		double accountCommission = 1;
		if (settings != null && !PriceUtils.isZero(settings.getCommission())) {
			accountCommission = settings.getCommission();
		}

		double commission = Default.getCommission() * accountCommission;

		if (refData != null) {
			// If LOT_COMMISSION_FEE has value, COMMISSION_FEE and MINIMAL_COMMISSION_FEE wouldn't have values
			// Thus here means return {refData.getLotCommissionFee() * accountCommission}
			double lotCommissionFee = refData.getLotCommissionFee();
			if (!nullOrZero(lotCommissionFee)) {
				double quantity = execution.getQuantity();
				commission = lotCommissionFee * quantity * accountCommission;
				return Math.ceil(commission);
			}

			double minCF = refData.getMinimalCommissionFee();
			if (!nullOrZero(minCF)) {
				minCommissionFee = minCF * accountCommission;
			}

			double commissionPct = refData.getCommissionFee();
			if (!nullOrZero(commissionPct)) {
				commission = commissionPct * accountCommission;
			}
		}

		if (!nullOrZero(value)) {
			commission *= value; // The calculated commission fee
			if (PriceUtils.EqualLessThan(commission, minCommissionFee)) {
				commission =  minCommissionFee; // If less than minCF, take minCF
			}
		}

		return Math.ceil(commission);
	}

	private boolean nullOrZero(Double commission) {
		return commission == null || PriceUtils.isZero(commission);
	}

	public Map<String, Double> getCommissionByMarket() {
		return commissionByMarket;
	}

	public void setCommissionByMarket(Map<String, Double> commissionByMarket) {
		this.commissionByMarket = commissionByMarket;
	}

	public Map<String, Double> getCommissionByExchange() {
		return commissionByExchange;
	}

	public void setCommissionByExchange(Map<String, Double> commissionByExchange) {
		this.commissionByExchange = commissionByExchange;
	}
}
