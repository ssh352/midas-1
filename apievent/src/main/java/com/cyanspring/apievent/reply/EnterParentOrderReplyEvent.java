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
package com.cyanspring.apievent.reply;

import com.cyanspring.apievent.obj.Order;

public final class EnterParentOrderReplyEvent extends ParentOrderReplyEvent {

	private String user;
	private String account;
	
	public EnterParentOrderReplyEvent(String key, String receiver, boolean ok,
									  String message, String txId, Order order, String user, String account) {
		super(key, receiver, ok, message, txId, order);
		
		this.user = user;
		this.account = account;
	}

	public String getUser()
	{
		return this.user;
	}
	
	public String getAccount()
	{
		return this.account;
	}
}