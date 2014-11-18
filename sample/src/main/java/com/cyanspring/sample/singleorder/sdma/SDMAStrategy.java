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
package com.cyanspring.sample.singleorder.sdma;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.strategy.StrategyException;
import com.cyanspring.common.type.OrderType;
import com.cyanspring.strategy.singleorder.SingleOrderStrategy;

public class SDMAStrategy extends SingleOrderStrategy {
	private static final Logger log = LoggerFactory
		.getLogger(SDMAStrategy.class);
	
	@Override
	protected Logger getLog() {
		return log;
	}

	@Override
	public void init() throws StrategyException {
		if(!parentOrder.getOrderType().equals(OrderType.Market))
			setQuoteRequired(false);
		
		setTimerEventRequired(false);
		super.init();
	}
	

}
