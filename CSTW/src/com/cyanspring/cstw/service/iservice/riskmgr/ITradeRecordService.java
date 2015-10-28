/**
 * 
 */
package com.cyanspring.cstw.service.iservice.riskmgr;

import java.util.List;

import com.cyanspring.cstw.service.iservice.IBasicService;
import com.cyanspring.cstw.service.model.riskmgr.RCTradeRecordModel;

/**
 * @author Yu-Junfeng
 * @create 30 Jul 2015
 */
public interface ITradeRecordService extends IBasicService {
	
	void queryTradeRecord();
	
	List<RCTradeRecordModel> getTradeRecordModelList();
	
}