package com.cyanspring.common.staticdata;

import com.cyanspring.common.event.refdata.RefDataUpdateEvent;

import java.util.List;

public interface IRefDataListener {
	void init() throws Exception;
	void uninit();
    void onRefData(List<RefData> refDataList) throws Exception;
    void onRefDataUpdate(List<RefData> refDataList,RefDataUpdateEvent.Action action);
}
