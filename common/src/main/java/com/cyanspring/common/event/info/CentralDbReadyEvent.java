package com.cyanspring.common.event.info;

import java.util.List;
import java.util.Map;

import com.cyanspring.common.event.RemoteAsyncEvent;
import com.cyanspring.common.staticdata.AbstractTickTable;
import com.cyanspring.common.staticdata.ITickTable;

@SuppressWarnings("serial")
public class CentralDbReadyEvent  extends RemoteAsyncEvent
{
	private Map<String, AbstractTickTable> tickTableList = null;
	private List<String> indexList = null;
	public CentralDbReadyEvent(String key, String receiver) 
	{
		super(key, receiver);
	}
	public Map<String, AbstractTickTable> getTickTableList() {
		return tickTableList;
	}
	public void setTickTableList(Map<String, AbstractTickTable> tickTableList) {
		this.tickTableList = tickTableList;
	}
	public List<String> getIndexList()
	{
		return indexList;
	}
	public void setIndexList(List<String> indexList)
	{
		this.indexList = indexList;
	}
}
