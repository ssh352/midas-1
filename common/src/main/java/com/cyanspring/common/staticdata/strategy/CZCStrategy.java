package com.cyanspring.common.staticdata.strategy;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cyanspring.common.marketdata.Quote;
import com.cyanspring.common.staticdata.RefData;
import com.cyanspring.common.staticdata.RefDataUtil;

public class CZCStrategy extends AbstractRefDataStrategy {

	protected static final Logger log = LoggerFactory.getLogger(CZCStrategy.class);

	@Override
	public void init(Calendar cal, Map<String, Quote> map) {
		super.init(cal, map);
	}

    @Override
    public List<RefData> updateRefData(RefData refData) {
		try {
			Calendar cal = Calendar.getInstance();
			if(refData.getCategory().equals("TC")){//動力煤
				refData.setSettlementDate(RefDataUtil.calSettlementDateByTradeDate(refData, cal,5));
			}else{
				refData.setSettlementDate(RefDataUtil.calSettlementDateByTradeDate(refData, cal,10));
			}
			refData.setIndexSessionType(getIndexSessionType(refData));
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}

		return super.updateRefData(refData);
    }

    @Override
    public void setRequireData(Object... objects) {
    	super.setRequireData(objects);
    }
}
