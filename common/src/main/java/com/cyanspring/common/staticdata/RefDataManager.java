/*******************************************************************************
 * Copyright (c) 2011-2012 Cyan Spring Limited
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms specified by license file attached.
 * 
 * Software distributed under the License is released on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 ******************************************************************************/
package com.cyanspring.common.staticdata;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.Default;
import com.cyanspring.common.IPlugin;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class RefDataManager extends RefDataService{

	Map<String, RefData> map = new HashMap<String, RefData>();

	@SuppressWarnings("unchecked")
	@Override
	public void init() throws Exception {
		log.info("initialising with " + refDataFile);
		XStream xstream = new XStream(new DomDriver());
		File file = new File(refDataFile);
		List<RefData> list;
		if (file.exists()) {
			list = (List<RefData>)xstream.fromXML(file);
		} else {
			throw new Exception("Missing refdata file: " + refDataFile);
		}
		
		for(RefData refData: list) {
            updateMarginRate(refData);
			map.put(refData.getSymbol(), refData);
		}
	}

    @Override
    public boolean update(String tradeDate) throws Exception {
        return false;
    }

    @Override
	public void uninit() {
		log.info("uninitialising");
		map.clear();
	}
	@Override
	public RefData getRefData(String symbol) {
		return map.get(symbol);
	}

	@Override
	public List<RefData> getRefDataList() {
		return new ArrayList<RefData>(map.values());
	}
}
