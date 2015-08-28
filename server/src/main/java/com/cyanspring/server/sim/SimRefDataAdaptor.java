package com.cyanspring.server.sim;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.cyanspring.common.event.AsyncEventProcessor;
import com.cyanspring.common.event.AsyncTimerEvent;
import com.cyanspring.common.event.IAsyncEventManager;
import com.cyanspring.common.event.IRemoteEventManager;
import com.cyanspring.common.event.ScheduleManager;
import com.cyanspring.common.staticdata.CodeTableData;
import com.cyanspring.common.staticdata.IRefDataAdaptor;
import com.cyanspring.common.staticdata.IRefDataListener;
import com.cyanspring.common.staticdata.RefData;
import com.cyanspring.common.staticdata.RefDataUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class SimRefDataAdaptor implements IRefDataAdaptor {
	private static final Logger log = LoggerFactory.getLogger(SimRefDataAdaptor.class);

	private String filePath = "./conf/sim/codetable_fcc.xml";
	private XStream xstream;
	private Map<String, CodeTableData> map;
	private List<IRefDataListener> listeners;
	private Boolean status = false;
	private Timer timer;

	@Override
	public boolean getStatus() {
		return status;
	}

	@Override
	public void flush() {

	}

	@Override
	@SuppressWarnings("unchecked")
	public void init() throws Exception {
		log.info("Initializing");
		xstream = new XStream(new DomDriver("UTF-8"));
		listeners = new ArrayList<>();
		File file = new File(filePath);
		map = (Map<String, CodeTableData>) xstream.fromXML(file);
		
		log.info("Setting raw refData...");
		List<RefData> refDataList = new ArrayList<>();
		for (Entry<String, CodeTableData> e : map.entrySet()) {
			CodeTableData data = e.getValue();
			if (data.getSecurityType() == 1)
				continue;
			RefData refData = new RefData();
			refData.setRefSymbol(data.getWindCode());
			refData.setCNDisplayName(data.getCnName());
			refData.setExchange(data.getSecurityExchange());
			refData.setCode(data.getWindCode());
			refData.setIType(String.valueOf(data.getSecurityType()));
			refData.setCategory(RefDataUtil.getCategory(refData));
			refDataList.add(refData);
		}
		
		timer = new Timer("SimRefDataAdaptor");
		RefDataUpdateTask updateTask = new RefDataUpdateTask(listeners, refDataList, this);
		timer.schedule(updateTask, 5000);
	}

	@Override
	public void uninit() {
		xstream = null;
		listeners = null;
		map = null;
	}

	@Override
	public void subscribeRefData(IRefDataListener listener) throws Exception {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	@Override
	public void unsubscribeRefData(IRefDataListener listener) {
		if (listeners.contains(listener))
			listeners.remove(listener);
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	@Override
	public void setStatus(boolean status) {
		this.status = status;
	}

}