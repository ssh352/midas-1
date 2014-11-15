package com.cyanspring.server.validation;

import java.util.Map;

import webcurve.util.PriceUtils;

import com.cyanspring.common.business.OrderField;
import com.cyanspring.common.business.ParentOrder;
import com.cyanspring.common.validation.IOrderValidator;
import com.cyanspring.common.validation.OrderValidationException;

public class StopLossOrderValidator implements IOrderValidator {

	@Override
	public void validate(Map<String, Object> map, ParentOrder order)
			throws OrderValidationException {
		if(null == order) {
			Object obj = map.get(OrderField.STOP_LOSS_PRICE.value());
			if(null == obj)
				throw new OrderValidationException("Stop Loss price can't be empty");
			
			double price = (Double)obj;
			try {
				if(PriceUtils.isZero(price))
					throw new OrderValidationException("Stop Loss price can't be 0");
			} catch (Exception e){
				throw new OrderValidationException(e.getMessage());
			}
		} 
	}

}
