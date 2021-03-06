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
package com.cyanspring.server.persistence;

import java.util.Date;

import com.cyanspring.common.type.PersistType;
import com.cyanspring.common.type.StrategyState;

public class ParentOrderDb extends TextObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7861415236682494413L;

	public ParentOrderDb(String id, PersistType persistType, StrategyState state, String user, 
			String account, String route, Date timeStamp, String xml, int line) {
		super(id, persistType, state, user, account, route, timeStamp, xml, line);
	}
	
}
