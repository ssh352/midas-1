package com.cyanspring.cstw.ui.rw.composite.table.provider;

import com.cyanspring.cstw.model.riskmgr.RCTradeRecordModel;
import com.cyanspring.cstw.ui.basic.DefaultLabelProviderAdapter;
import com.cyanspring.cstw.ui.utils.LTWStringUtils;

/**
 * @author Junfeng
 * @create 26 Oct 2015
 */
public class RWTradeRecordLabelProvider extends DefaultLabelProviderAdapter {

	@Override
	public String getColumnText(Object element, int columnIndex) {
		RCTradeRecordModel model = (RCTradeRecordModel) element;
		switch (columnIndex) {
		case 0:
			return model.getRecord();
		case 1:
			return model.getSymbol();
		case 2:
			return model.getType();
		case 3:
			return model.getVolume().toString();
		case 4:
			return model.getPrice().toString();
		case 5:
			return LTWStringUtils.doubleToString(model.getTotalPrice());
		case 6:
			return model.getTradeTime();
		case 7:
			return model.getTrader();
		default:
			return "";
		}
	}

}
