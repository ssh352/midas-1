/*******************************************************************************
' * Copyright (c) 2011-2012 Cyan Spring Limited
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms specified by license file attached.
 * 
 * Software distributed under the License is released on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 ******************************************************************************/
package com.cyanspring.cstw.business;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.BeanHolder;
import com.cyanspring.common.Clock;
import com.cyanspring.common.Default;
import com.cyanspring.common.SystemInfo;
import com.cyanspring.common.account.Account;
import com.cyanspring.common.account.AccountSetting;
import com.cyanspring.common.account.OverallPosition;
import com.cyanspring.common.account.UserGroup;
import com.cyanspring.common.account.UserRole;
import com.cyanspring.common.business.FieldDef;
import com.cyanspring.common.business.MultiInstrumentStrategyDisplayConfig;
import com.cyanspring.common.cstw.position.AllPositionManager;
import com.cyanspring.common.data.AlertType;
import com.cyanspring.common.event.AsyncEvent;
import com.cyanspring.common.event.AsyncTimerEvent;
import com.cyanspring.common.event.IAsyncEventListener;
import com.cyanspring.common.event.IRemoteEventManager;
import com.cyanspring.common.event.ScheduleManager;
import com.cyanspring.common.event.account.AccountSettingSnapshotReplyEvent;
import com.cyanspring.common.event.account.AccountSettingSnapshotRequestEvent;
import com.cyanspring.common.event.account.CSTWUserLoginReplyEvent;
import com.cyanspring.common.event.account.UserLoginReplyEvent;
import com.cyanspring.common.event.info.RateConverterReplyEvent;
import com.cyanspring.common.event.info.RateConverterRequestEvent;
import com.cyanspring.common.event.order.InitClientEvent;
import com.cyanspring.common.event.order.InitClientRequestEvent;
import com.cyanspring.common.event.strategy.MultiInstrumentStrategyFieldDefUpdateEvent;
import com.cyanspring.common.event.strategy.SingleInstrumentStrategyFieldDefUpdateEvent;
import com.cyanspring.common.event.strategy.SingleOrderStrategyFieldDefUpdateEvent;
import com.cyanspring.common.event.system.NodeInfoEvent;
import com.cyanspring.common.event.system.ServerHeartBeatEvent;
import com.cyanspring.common.fx.IFxConverter;
import com.cyanspring.common.marketdata.DataReceiver;
import com.cyanspring.common.marketsession.DefaultStartEndTime;
import com.cyanspring.common.server.event.ServerReadyEvent;
import com.cyanspring.common.util.IdGenerator;
import com.cyanspring.common.util.TimeUtil;
import com.cyanspring.cstw.cachingmanager.quote.QuoteCachingManager;
import com.cyanspring.cstw.cachingmanager.riskcontrol.FrontRCOrderCachingManager;
import com.cyanspring.cstw.cachingmanager.riskcontrol.FrontRCPositionCachingManager;
import com.cyanspring.cstw.cachingmanager.riskcontrol.eventcontroller.BackRCOpenPositionEventController;
import com.cyanspring.cstw.cachingmanager.riskcontrol.eventcontroller.FrontRCOpenPositionEventController;
import com.cyanspring.cstw.cachingmanager.riskcontrol.eventcontroller.RCIndividualEventController;
import com.cyanspring.cstw.cachingmanager.riskcontrol.eventcontroller.RCInstrumentStatisticsEventController;
import com.cyanspring.cstw.cachingmanager.riskcontrol.eventcontroller.RCInstrumentSummaryEventController;
import com.cyanspring.cstw.cachingmanager.riskcontrol.eventcontroller.RCOrderEventController;
import com.cyanspring.cstw.cachingmanager.riskcontrol.eventcontroller.RCTradeEventController;
import com.cyanspring.cstw.event.SelectUserAccountEvent;
import com.cyanspring.cstw.event.ServerStatusEvent;
import com.cyanspring.cstw.gui.session.GuiSession;
import com.cyanspring.cstw.ui.views.ServerStatusDisplay;

public class Business {
	private static Logger log = LoggerFactory.getLogger(Business.class);
	private static Business instance; // Singleton
	private CSTWBeanPool beanPool;

	private SystemInfo systemInfo;
	private IRemoteEventManager eventManager;
	private OrderCachingManager orderManager;
	private String inbox;
	private String channel;
	private String nodeInfoChannel;
	private HashMap<String, Boolean> servers = new HashMap<String, Boolean>();
	private EventListenerImpl listener = new EventListenerImpl();
	private List<String> singleOrderDisplayFieldList;
	private List<String> singleInstrumentDisplayFieldList;
	private List<String> multiInstrumentDisplayFieldList;
	private Map<String, Map<String, FieldDef>> singleOrderFieldDefMap;
	private Map<String, Map<String, FieldDef>> singleInstrumentFieldDefMap;
	private Map<String, MultiInstrumentStrategyDisplayConfig> multiInstrumentFieldDefMap;
	private ScheduleManager scheduleManager = new ScheduleManager();
	private AsyncTimerEvent timerEvent = new AsyncTimerEvent();
	private int heartBeatInterval = 10000; // 5 seconds
	private HashMap<String, Date> lastHeartBeatMap = new HashMap<String, Date>();
	private DefaultStartEndTime defaultStartEndTime;
	private Map<AlertType, Integer> alertColorConfig;
	private String user = Default.getUser();
	private String account = Default.getAccount();
	private Account loginAccount = null;
	private AccountSetting accountSetting = null;
	private UserGroup userGroup = new UserGroup("Admin", UserRole.Admin);
	private List<String> accountGroupList = new ArrayList<String>();
	private List<Account> accountList = new ArrayList<Account>();

	private TraderInfoListener traderInfoListener = null;
	private DataReceiver quoteDataReceiver = null;
	private AllPositionManager allPositionManager = null;
	private IFxConverter rateConverter = null;

	public static Business getInstance() {
		if (null == instance) {
			instance = new Business();
		}
		return instance;
	}

	/*
	 * singleton implementation
	 */
	private Business() {
	}

	public void init() throws Exception {
		Version ver = new Version();
		log.info(ver.getVersionDetails());
		log.info("Initializing business obj...");
		this.systemInfo = BeanHolder.getInstance().getSystemInfo();
		this.user = Default.getUser();
		this.account = Default.getAccount();

		// create node.info subscriber and publisher
		this.channel = systemInfo.getEnv() + "." + systemInfo.getCategory()
				+ "." + "channel";
		this.nodeInfoChannel = systemInfo.getEnv() + "."
				+ systemInfo.getCategory() + "." + "node";
		InetAddress addr = InetAddress.getLocalHost();
		String hostName = addr.getHostName();
		String userName = System.getProperty("user.name");
		userName = userName == null ? "" : userName;
		this.inbox = hostName + "." + userName + "."
				+ IdGenerator.getInstance().getNextID();
		BeanHolder beanHolder = BeanHolder.getInstance();
		if (beanHolder == null) {
			throw new Exception("BeanHolder is not yet initialised");
		}
		beanPool = new CSTWBeanPool(beanHolder);
		// ActiveMQObjectService transport = new ActiveMQObjectService();
		// transport.setUrl(systemInfo.getUrl());

		// eventManager = new
		// RemoteEventManager(beanHolder.getTransportService());
		eventManager = beanHolder.getEventManager();
		alertColorConfig = beanHolder.getAlertColorConfig();
		quoteDataReceiver = beanHolder.getDataReceiver();
		allPositionManager = beanHolder.getAllPositionManager();
		boolean ok = false;
		while (!ok) {
			try {
				eventManager.init(channel, inbox);
			} catch (Exception e) {
				log.error(e.getMessage());
				log.debug("Retrying in 3 seconds...");
				ok = false;
				Thread.sleep(3000);
				continue;
			}
			ok = true;
		}

		eventManager.addEventChannel(this.channel);
		eventManager.addEventChannel(this.nodeInfoChannel);

		orderManager = new OrderCachingManager(eventManager);

		ServerStatusDisplay.getInstance().init();

		eventManager.subscribe(NodeInfoEvent.class, listener);
		eventManager.subscribe(InitClientEvent.class, listener);
		eventManager.subscribe(UserLoginReplyEvent.class, listener);
		eventManager.subscribe(SelectUserAccountEvent.class, listener);
		eventManager.subscribe(ServerHeartBeatEvent.class, listener);
		eventManager.subscribe(ServerReadyEvent.class, listener);
		eventManager.subscribe(SingleOrderStrategyFieldDefUpdateEvent.class,
				listener);
		eventManager.subscribe(
				MultiInstrumentStrategyFieldDefUpdateEvent.class, listener);
		eventManager.subscribe(CSTWUserLoginReplyEvent.class, listener);
		eventManager
				.subscribe(AccountSettingSnapshotReplyEvent.class, listener);
		eventManager.subscribe(RateConverterReplyEvent.class, listener);
		// schedule timer
		scheduleManager.scheduleRepeatTimerEvent(heartBeatInterval, listener,
				timerEvent);
		log.info("TraderInfoListener not init version");
		// traderInfoListener = new TraderInfoListener();
		// initSessionListener();

	}

	public void start() throws Exception {
		// publish my node info
		NodeInfoEvent nodeInfo = new NodeInfoEvent(null, null, false, true,
				inbox, inbox);
		eventManager.publishRemoteEvent(nodeInfoChannel, nodeInfo);
		log.info("Published my node info");

	}

	class EventListenerImpl implements IAsyncEventListener {
		@Override
		public void onEvent(AsyncEvent event) {
			// log.info("Received message: " + message);
			if (event instanceof NodeInfoEvent) {
				NodeInfoEvent nodeInfo = (NodeInfoEvent) event;
				if (nodeInfo.getServer()) {
					log.info("NodeInfoEvent received: " + nodeInfo.getSender());
					Boolean serverIsUp = servers.get(nodeInfo.getInbox());
					if (serverIsUp != null && serverIsUp) {
						log.error("ignore since server " + nodeInfo.getInbox()
								+ " is still up");
						return;
					}
					servers.put(nodeInfo.getInbox(), true);
					lastHeartBeatMap.put(nodeInfo.getInbox(), Clock
							.getInstance().now());
				}
			} else if (event instanceof InitClientEvent) {
				log.debug("Received event: " + event);
				InitClientEvent initClientEvent = (InitClientEvent) event;
				singleOrderFieldDefMap = initClientEvent
						.getSingleOrderFieldDefs();
				singleOrderDisplayFieldList = initClientEvent
						.getSingleOrderDisplayFields();
				singleInstrumentFieldDefMap = initClientEvent
						.getSingleInstrumentFieldDefs();
				singleInstrumentDisplayFieldList = initClientEvent
						.getSingleInstrumentDisplayFields();
				defaultStartEndTime = initClientEvent.getDefaultStartEndTime();
				multiInstrumentDisplayFieldList = initClientEvent
						.getMultiInstrumentDisplayFields();
				multiInstrumentFieldDefMap = initClientEvent
						.getMultiInstrumentStrategyFieldDefs();
				if (!isLoginRequired()) {
					requestStrategyInfo(initClientEvent.getSender());
				}

			} else if (event instanceof CSTWUserLoginReplyEvent) {
				CSTWUserLoginReplyEvent evt = (CSTWUserLoginReplyEvent) event;
				processCSTWUserLoginReplyEvent(evt);
				if (evt.isOk()) {
					beanPool.getTickManager().init(getFirstServer());
					requestRateConverter();
					// if(null != loginAccount);
					// traderInfoListener.init(loginAccount);

				}
				if (isLoginRequired() && evt.isOk()) {
					requestStrategyInfo(evt.getSender());
				}

			} else if (event instanceof AccountSettingSnapshotReplyEvent) {

				AccountSettingSnapshotReplyEvent evt = (AccountSettingSnapshotReplyEvent) event;
				processAccountSettingSnapshotReplyEvent(evt);
			} else if (event instanceof UserLoginReplyEvent) {

				UserLoginReplyEvent evt = (UserLoginReplyEvent) event;
				processUserLoginReplyEvent(evt);
				if (isLoginRequired() && evt.isOk()) {
					requestStrategyInfo(evt.getSender());
				}
			} else if (event instanceof ServerReadyEvent) {
				log.info("ServerReadyEvent  received: "
						+ ((ServerReadyEvent) event).getSender());
				InitClientRequestEvent request = new InitClientRequestEvent(
						null, ((ServerReadyEvent) event).getSender());
				try {
					eventManager.sendRemoteEvent(request);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					e.printStackTrace();
				}
			} else if (event instanceof ServerHeartBeatEvent) {
				processServerHeartBeatEvent((ServerHeartBeatEvent) event);
			} else if (event instanceof SingleOrderStrategyFieldDefUpdateEvent) {
				processingSingleOrderStrategyFieldDefUpdateEvent((SingleOrderStrategyFieldDefUpdateEvent) event);
			} else if (event instanceof SingleInstrumentStrategyFieldDefUpdateEvent) {
				processingSingleInstrumentStrategyFieldDefUpdateEvent((SingleInstrumentStrategyFieldDefUpdateEvent) event);
			} else if (event instanceof MultiInstrumentStrategyFieldDefUpdateEvent) {
				processingMultiInstrumentStrategyFieldDefUpdateEvent((MultiInstrumentStrategyFieldDefUpdateEvent) event);
			} else if (event instanceof AsyncTimerEvent) {
				processAsyncTimerEvent((AsyncTimerEvent) event);
			} else if (event instanceof SelectUserAccountEvent) {
				processSelectUserAccountEvent((SelectUserAccountEvent) event);
			} else if (event instanceof RateConverterReplyEvent) {
				RateConverterReplyEvent e = (RateConverterReplyEvent) event;
				rateConverter = e.getConverter();
			} else {
				log.error("I dont expect this event: " + event);
			}
		}

	}

	private void requestRateConverter() {
		RateConverterRequestEvent request = new RateConverterRequestEvent(
				IdGenerator.getInstance().getNextID(), getFirstServer());
		try {
			getEventManager().sendRemoteEvent(request);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void processSelectUserAccountEvent(SelectUserAccountEvent event) {
		log.info("Setting current user/account to: " + this.user + "/"
				+ this.account);
		this.user = event.getUser();
		this.account = event.getAccount();
	}

	private void requestStrategyInfo(String server) {
		try {
			orderManager.init();
			eventManager.sendEvent(new ServerStatusEvent(server, true));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			e.printStackTrace();
		}

	}

	synchronized private void processingSingleOrderStrategyFieldDefUpdateEvent(
			SingleOrderStrategyFieldDefUpdateEvent event) {
		singleOrderFieldDefMap.put(event.getName(), event.getFieldDefs());
		log.info("Single-order strategy field def update: " + event.getName());
	}

	public void processingSingleInstrumentStrategyFieldDefUpdateEvent(
			SingleInstrumentStrategyFieldDefUpdateEvent event) {
		singleInstrumentFieldDefMap.put(event.getName(), event.getFieldDefs());
		log.info("Single-instrument strategy field def update: "
				+ event.getName());
	}

	synchronized private void processingMultiInstrumentStrategyFieldDefUpdateEvent(
			MultiInstrumentStrategyFieldDefUpdateEvent event) {
		multiInstrumentFieldDefMap.put(event.getConfig().getStrategy(),
				event.getConfig());
		log.info("Multi-Instrument strategy field def update: "
				+ event.getConfig().getStrategy());
	}

	public void processServerHeartBeatEvent(ServerHeartBeatEvent event) {
		lastHeartBeatMap.put(event.getSender(), Clock.getInstance().now());
	}

	public void processAsyncTimerEvent(AsyncTimerEvent event) {
		for (Entry<String, Date> entry : lastHeartBeatMap.entrySet()) {
			if (TimeUtil.getTimePass(entry.getValue()) > heartBeatInterval) {
				log.debug("Sending server down event: " + entry.getKey());
				servers.put(entry.getKey(), false);
				eventManager.sendEvent(new ServerStatusEvent(entry.getKey(),
						false));
			} else { // server heart beat can go back up
				Boolean up = servers.get(entry.getKey());
				if (null != up && !up) {
					log.debug("Sending server up event: " + entry.getKey());
					servers.put(entry.getKey(), true);
					eventManager.sendEvent(new ServerStatusEvent(
							entry.getKey(), true));
				}
			}
		}
	}

	public int getHeartBeatInterval() {
		return heartBeatInterval;
	}

	public void setHeartBeatInterval(int heartBeatInterval) {
		this.heartBeatInterval = heartBeatInterval;
	}

	public void stop() {
		log.info("Stopping business object...");
		try {
			scheduleManager.uninit();
			eventManager.close();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}

	public IRemoteEventManager getEventManager() {
		return eventManager;
	}

	public OrderCachingManager getOrderManager() {
		return orderManager;
	}

	public ScheduleManager getScheduleManager() {
		return scheduleManager;
	}

	public String getInbox() {
		return inbox;
	}

	synchronized public List<String> getParentOrderDisplayFields() {
		return singleOrderDisplayFieldList;
	}

	synchronized public DefaultStartEndTime getDefaultStartEndTime() {
		return defaultStartEndTime;
	}

	synchronized public List<String> getSingleOrderDisplayFields() {
		return singleOrderDisplayFieldList;
	}

	synchronized public Map<String, Map<String, FieldDef>> getSingleOrderFieldDefs() {
		return singleOrderFieldDefMap;
	}

	synchronized public List<String> getSingleInstrumentDisplayFields() {
		return singleInstrumentDisplayFieldList;
	}

	synchronized public Map<String, Map<String, FieldDef>> getSingleInstrumentFieldDefs() {
		return singleInstrumentFieldDefMap;
	}

	synchronized public Map<String, MultiInstrumentStrategyDisplayConfig> getMultiInstrumentFieldDefs() {
		return multiInstrumentFieldDefMap;
	}

	synchronized public List<String> getMultiInstrumentDisplayFields() {
		return multiInstrumentDisplayFieldList;
	}

	synchronized public List<String> getSingleOrderAmendableFields(String key) {
		List<String> result = new ArrayList<String>();
		Map<String, FieldDef> fieldDefs = singleOrderFieldDefMap.get(key);
		if (null != fieldDefs) {
			for (FieldDef fieldDef : fieldDefs.values()) {
				if (fieldDef.isAmendable())
					result.add(fieldDef.getName());
			}
		}
		return result;
	}

	synchronized public List<String> getSingleInstrumentAmendableFields(
			String key) {
		List<String> result = new ArrayList<String>();
		Map<String, FieldDef> fieldDefs = singleInstrumentFieldDefMap.get(key);
		if (null != fieldDefs) {
			for (FieldDef fieldDef : fieldDefs.values()) {
				if (fieldDef.isAmendable())
					result.add(fieldDef.getName());
			}
		}
		return result;
	}

	public Map<AlertType, Integer> getAlertColorConfig() {
		return alertColorConfig;
	}

	public String getFirstServer() {
		for (Entry<String, Boolean> entry : servers.entrySet()) {
			if (entry.getValue())
				return entry.getKey();
		}
		return null;
	}

	public boolean isFirstServerReady() {
		Boolean result = servers.get(getFirstServer());
		return result == null ? false : result;
	}

	public boolean isLoginRequired() {
		return BeanHolder.getInstance().isLoginRequired();
	}

	public void processAccountSettingSnapshotReplyEvent(
			AccountSettingSnapshotReplyEvent event) {
		if (null != event.getAccountSetting()) {
			accountSetting = event.getAccountSetting();
		}
	}

	public boolean processCSTWUserLoginReplyEvent(CSTWUserLoginReplyEvent event) {
		if (!event.isOk())
			return false;

		List<Account> accountList = event.getAccountList();
		if (null != accountList && !accountList.isEmpty()) {
			loginAccount = event.getAccountList().get(0);
			log.info("loginAccount:{}", loginAccount.getId());
			sendAccountSettingRequestEvent(loginAccount.getId());
		}
		Map<String, Account> user2AccoutMap = event.getUser2AccountMap();
		if (null != user2AccoutMap && !user2AccoutMap.isEmpty()) {
			accountList.addAll(user2AccoutMap.values());
			for (Account acc : user2AccoutMap.values()) {
				accountGroupList.add(acc.getId());
			}
		}
		UserGroup userGroup = event.getUserGroup();
		this.user = userGroup.getUser();

		if (null != loginAccount) {
			this.account = loginAccount.getId();
		} else {
			this.account = userGroup.getUser();
		}

		this.userGroup = userGroup;
		beanPool.setUserGroup(userGroup);
		log.info("login user:{},{}", user, userGroup.getRole());

		QuoteCachingManager.getInstance().init();

		if (this.userGroup.getRole() == UserRole.RiskManager
				|| this.userGroup.getRole() == UserRole.BackEndRiskManager) {
			allPositionManager.init(eventManager, getFirstServer(),
					accountList, getUserGroup());
			FrontRCPositionCachingManager.getInstance().init();
			FrontRCOrderCachingManager.getInstance().init();

			if (this.userGroup.getRole() == UserRole.RiskManager) {
				FrontRCOpenPositionEventController.getInstance().init();
			} else if (this.userGroup.getRole() == UserRole.BackEndRiskManager) {
				BackRCOpenPositionEventController.getInstance().init();
			}
			RCTradeEventController.getInstance().init();
			RCInstrumentStatisticsEventController.getInstance().init();
			RCIndividualEventController.getInstance().init();
			RCInstrumentSummaryEventController.getInstance().init();
			RCOrderEventController.getInstance().init();
		}

		return true;
	}

	private void sendAccountSettingRequestEvent(String accountId) {
		AccountSettingSnapshotRequestEvent settingRequestEvent = new AccountSettingSnapshotRequestEvent(
				IdGenerator.getInstance().getNextID(), Business.getInstance()
						.getFirstServer(), accountId, null);
		try {
			eventManager.sendRemoteEvent(settingRequestEvent);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}

	public boolean processUserLoginReplyEvent(UserLoginReplyEvent event) {
		if (!event.isOk())
			return false;

		this.user = event.getUser().getId();
		if (event.getDefaultAccount() != null)
			this.account = event.getDefaultAccount().getId();
		else if (null != event.getAccounts() && event.getAccounts().size() > 0)
			this.account = event.getAccounts().get(0).getId();
		else
			return false;

		return true;
	}

	public String getUser() {
		return user;
	}

	public String getAccount() {
		return account;
	}

	public UserGroup getUserGroup() {
		return userGroup;
	}

	public List<String> getAccountGroup() {
		return accountGroupList;
	}

	public boolean isManagee(String account) {

		if (userGroup.isAdmin())
			return true;

		if (userGroup.isGroupPairExist(account))
			return true;

		if (userGroup.isManageeExist(account))
			return true;

		return false;
	}

	public Account getLoginAccount() {
		return loginAccount;
	}

	public AccountSetting getAccountSetting() {
		return accountSetting;
	}

	public void setAccountSetting(AccountSetting accountSetting) {
		this.accountSetting = accountSetting;
	}

	private void initSessionListener() {
		GuiSession.getInstance().addPropertyChangeListener(
				GuiSession.Property.ACCOUNT_SETTING.toString(),
				new PropertyChangeListener() {

					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						accountSetting = GuiSession.getInstance()
								.getAccountSetting();
					}
				});
	}

	public DataReceiver getQuoteDataReceiver() {
		return quoteDataReceiver;
	}

	public AllPositionManager getAllPositionManager() {
		return allPositionManager;
	}

	public IFxConverter getRateConverter() {
		return rateConverter;
	}

	public List<OverallPosition> getOverallPositionList() {
		return allPositionManager.getOverAllPositionList();
	}

	public List<Account> getAccountList() {
		return this.accountList;
	}

	public static IBusinessService getBusinessService() {
		return instance.beanPool;
	}

}
