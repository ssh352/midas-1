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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.cyanspring.common.account.UserType;
import com.cyanspring.common.event.account.*;
import com.google.common.base.Strings;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.cyanspring.common.Clock;
import com.cyanspring.common.Default;
import com.cyanspring.common.account.Account;
import com.cyanspring.common.account.AccountSetting;
import com.cyanspring.common.account.ClosedPosition;
import com.cyanspring.common.account.OpenPosition;
import com.cyanspring.common.account.PositionPeakPrice;
import com.cyanspring.common.account.User;
import com.cyanspring.common.account.UserException;
import com.cyanspring.common.business.ChildOrder;
import com.cyanspring.common.business.Execution;
import com.cyanspring.common.business.Instrument;
import com.cyanspring.common.business.MultiInstrumentStrategyData;
import com.cyanspring.common.business.OrderField;
import com.cyanspring.common.business.ParentOrder;
import com.cyanspring.common.data.DataObject;
import com.cyanspring.common.event.AsyncTimerEvent;
import com.cyanspring.common.event.IAsyncEventManager;
import com.cyanspring.common.event.IRemoteEventManager;
import com.cyanspring.common.event.ScheduleManager;
import com.cyanspring.common.event.order.UpdateChildOrderEvent;
import com.cyanspring.common.event.order.UpdateParentOrderEvent;
import com.cyanspring.common.event.signal.CancelSignalEvent;
import com.cyanspring.common.event.signal.SignalEvent;
import com.cyanspring.common.event.strategy.MultiInstrumentStrategyUpdateEvent;
import com.cyanspring.common.event.strategy.SingleInstrumentStrategyUpdateEvent;
import com.cyanspring.common.message.ErrorMessage;
import com.cyanspring.common.message.MessageLookup;
import com.cyanspring.common.type.PersistType;
import com.cyanspring.common.type.StrategyState;
import com.cyanspring.common.util.IdGenerator;
import com.cyanspring.common.util.TimeUtil;
import com.cyanspring.common.event.AsyncEventProcessor;
import com.cyanspring.server.account.AccountKeeper;
import com.cyanspring.server.account.UserKeeper;

import org.apache.derby.drda.NetworkServerControl;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PersistenceManager {
	private static final Logger log = LoggerFactory
			.getLogger(PersistenceManager.class);
	
	public static String ID = PersistenceManager.class.toString();
	
	@Autowired
	private IRemoteEventManager eventManager;
	
	@Autowired
	SessionFactory sessionFactory;
	
	@Autowired
	ScheduleManager scheduleManager;
	
	@Autowired
	CentralDbConnector centralDbConnector;
	
	private CheckEmailType checkEmailUnique = CheckEmailType.allCheck;
    private CheckPhoneType checkPhoneUnique = CheckPhoneType.allCheck;
	private boolean syncCentralDb = true;
	private boolean embeddedSQLServer;
	private int textSize = 4000;
	private boolean cleanStart;
	private boolean todayOnly;
	private long purgeOrderDays = 20;
	private boolean deleteTerminated = true;
	protected boolean persistSignal;
	NetworkServerControl server;
	private String embeddedHost = "localhost";
	private int embeddedPort = 1527;
	
	private AsyncTimerEvent timerEvent = new AsyncTimerEvent();
	private long timeInterval = 10*60*1000;
	private AsyncEventProcessor eventProcessor = new AsyncEventProcessor() {

		@Override
		public void subscribeToEvents() {
			subscribeToEvent(UpdateParentOrderEvent.class, null);
			subscribeToEvent(UpdateChildOrderEvent.class, null);
			subscribeToEvent(SingleInstrumentStrategyUpdateEvent.class, null);
			subscribeToEvent(MultiInstrumentStrategyUpdateEvent.class, null);
			subscribeToEvent(PmUpdateUserEvent.class, PersistenceManager.ID);
			subscribeToEvent(PmCreateAccountEvent.class, PersistenceManager.ID);
			subscribeToEvent(PmUpdateAccountEvent.class, PersistenceManager.ID);
			subscribeToEvent(PmRemoveDetailOpenPositionEvent.class, PersistenceManager.ID);
			subscribeToEvent(PmUpdateDetailOpenPositionEvent.class, PersistenceManager.ID);
			subscribeToEvent(ClosedPositionUpdateEvent.class, null);
			subscribeToEvent(PmChangeAccountSettingEvent.class, PersistenceManager.ID);
			subscribeToEvent(PmEndOfDayRollEvent.class, PersistenceManager.ID);
			subscribeToEvent(InternalResetAccountRequestEvent.class, null);
			subscribeToEvent(PmPositionPeakPriceUpdateEvent.class, null);
			subscribeToEvent(PmPositionPeakPriceDeleteEvent.class, null);

			if(persistSignal) {
				subscribeToEvent(SignalEvent.class, null);
				subscribeToEvent(CancelSignalEvent.class, null);
			}
		}

		@Override
		public IAsyncEventManager getEventManager() {
			return eventManager;
		}
	};
	
	private AsyncEventProcessor userEventProcessor = new AsyncEventProcessor() {

		@Override
		public void subscribeToEvents() {
			subscribeToEvent(AsyncTimerEvent.class, null);
			subscribeToEvent(PmUserLoginEvent.class, PersistenceManager.ID);
			subscribeToEvent(PmUserCreateAndLoginEvent.class, PersistenceManager.ID);
			subscribeToEvent(PmCreateUserEvent.class, PersistenceManager.ID);
			subscribeToEvent(ChangeUserPasswordEvent.class, null);
			subscribeToEvent(UserTerminateEvent.class, null);
            subscribeToEvent(UserMappingEvent.class, null);
            subscribeToEvent(UserMappingDetachEvent.class, null);
		}

		@Override
		public IAsyncEventManager getEventManager() {
			return eventManager;
		}
		
	};
	
	
	public PersistenceManager() {
	}
	
	public void init() throws Exception {
		log.info("initialising");
		if(embeddedSQLServer)
			startEmbeddedSQLServer();
		
		if(cleanStart)
			truncateData(Clock.getInstance().now());
		else if (todayOnly)
			truncateData(TimeUtil.getOnlyDate(Clock.getInstance().now()));
		else if(purgeOrderDays > 0) {
			truncateOrders();
		}

		// subscribe to events
		eventProcessor.setHandler(this);
		eventProcessor.init();
		if(eventProcessor.getThread() != null)
			eventProcessor.getThread().setName("PersistenceManager");
		
		userEventProcessor.setHandler(this);
		userEventProcessor.init();
		if(userEventProcessor.getThread() != null)
			userEventProcessor.getThread().setName("PersistenceManager(Users)");
		
		scheduleManager.scheduleRepeatTimerEvent(timeInterval, userEventProcessor, timerEvent);

	}

	public void uninit() {
		log.info("uninitialising");
		eventProcessor.uninit();
		if(embeddedSQLServer)
			stopEmbeddedSQLServer();
	}
	
	private void startEmbeddedSQLServer() throws UnknownHostException, Exception {
		server = new NetworkServerControl
				(InetAddress.getByName(embeddedHost),embeddedPort);
		server.start(null);
		log.info("Embedded SQL server started");	
	}
	
	private void stopEmbeddedSQLServer() {
		try {
			server.shutdown();
			log.info("Embedded SQL server stopped");	
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	private void truncateData(Date date) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
		    tx = session.beginTransaction();
		    String hql = "delete from User where created < :created";
		    Query query = session.createQuery(hql);
	        query.setParameter("created", date);
	        int rowCount = query.executeUpdate();
	        log.debug("USER Records deleted: " + rowCount);
		    
	        hql = "delete from Account where created < :created";
	        query = session.createQuery(hql);
	        query.setParameter("created", date);
	        rowCount = query.executeUpdate();
	        log.debug("ACCOUNT Records deleted: " + rowCount);

	        hql = "delete from ClosedPosition where created < :created";
	        query = session.createQuery(hql);
	        query.setParameter("created", date);
	        rowCount = query.executeUpdate();
	        log.debug("CLOSED_POSITION Records deleted: " + rowCount);

	        hql = "delete from OpenPosition where created < :created";
	        query = session.createQuery(hql);
	        query.setParameter("created", date);
	        rowCount = query.executeUpdate();
	        log.debug("OPEN_POSITION Records deleted: " + rowCount);

	        hql = "delete from TextObject where timeStamp < :timeStamp and serverId = :serverId";
	        query = session.createQuery(hql);
	        query.setParameter("timeStamp", date);
	        query.setParameter("serverId", IdGenerator.getInstance().getSystemId());
	        rowCount = query.executeUpdate();
	        log.debug("TextObject Records deleted: " + rowCount);
	        
	        hql = "delete from ChildOrder where created < :created and serverId = :serverId";
	        query = session.createQuery(hql);
	        query.setParameter("created", date);
	        query.setParameter("serverId", IdGenerator.getInstance().getSystemId());
	        rowCount = query.executeUpdate();
	        log.debug("ChildOrder Records deleted: " + rowCount);

	        hql = "delete from ChildOrderAudit where created < :created and serverId = :serverId";
	        query = session.createQuery(hql);
	        query.setParameter("created", date);
	        query.setParameter("serverId", IdGenerator.getInstance().getSystemId());
	        rowCount = query.executeUpdate();
	        log.debug("ChildOrderAudit Records deleted: " + rowCount);

	        hql = "delete from Execution where created < :created and serverId = :serverId";
	        query = session.createQuery(hql);
	        query.setParameter("created", date);
	        query.setParameter("serverId", IdGenerator.getInstance().getSystemId());
	        rowCount = query.executeUpdate();
	        log.debug("Execution Records deleted: " + rowCount);

	        tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
	}

	private void truncateOrders() {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			Date date = new Date();
			date = new Date(date.getTime() - purgeOrderDays * TimeUtil.millisInDay);
			
		    tx = session.beginTransaction();
		    String hql;
		    Query query;
		    int rowCount;
		    
	        hql = "delete from TextObject where timeStamp < :timeStamp and serverId = :serverId";
	        query = session.createQuery(hql);
	        query.setParameter("timeStamp", date);
	        query.setParameter("serverId", IdGenerator.getInstance().getSystemId());
	        rowCount = query.executeUpdate();
	        log.debug("TextObject Records deleted: " + rowCount);
	        
	        hql = "delete from ChildOrder where created < :created and serverId = :serverId";
	        query = session.createQuery(hql);
	        query.setParameter("created", date);
	        query.setParameter("serverId", IdGenerator.getInstance().getSystemId());
	        rowCount = query.executeUpdate();
	        log.debug("ChildOrder Records deleted: " + rowCount);

	        tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
	}
	
	public void processInternalResetAccountRequestEvent(InternalResetAccountRequestEvent event) {
		ResetAccountRequestEvent evt = event.getEvent();
		String account = evt.getAccount();
		log.info("Received InternalResetAccountRequestEvent: " + account);
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			Date date = new Date();
			date = new Date(date.getTime() - purgeOrderDays * TimeUtil.millisInDay);
			
		    tx = session.beginTransaction();
		    String hql;
		    Query query;
		    int rowCount;
		    
	        hql = "delete from TextObject where account = :account";
	        query = session.createQuery(hql);
	        query.setParameter("account", account);
	        rowCount = query.executeUpdate();
	        log.info("TextObject Records deleted: " + rowCount);
	        
	        hql = "delete from ChildOrder where account = :account";
	        query = session.createQuery(hql);
	        query.setParameter("account", account);
	        rowCount = query.executeUpdate();
	        log.info("ChildOrder Records deleted: " + rowCount);

	        hql = "delete from ClosedPosition where account = :account";
	        query = session.createQuery(hql);
	        query.setParameter("account", account);
	        rowCount = query.executeUpdate();
	        log.info("CLOSED_POSITION Records deleted: " + rowCount);

	        hql = "delete from OpenPosition where account = :account";
	        query = session.createQuery(hql);
	        query.setParameter("account", account);
	        rowCount = query.executeUpdate();
	        log.info("OPEN_POSITION Records deleted: " + rowCount);

	        hql = "delete from Execution where account = :account";
	        query = session.createQuery(hql);
	        query.setParameter("account", account);
	        rowCount = query.executeUpdate();
	        log.info("Execution Records deleted: " + rowCount);
	        
	        query = session.getNamedQuery("cleanAccountsDailyByAccount");
		    query.setParameter("account", account);
	        rowCount = query.executeUpdate();
	        log.info("AccountsDaily Records deleted: " + rowCount);

	        tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}		
	}

	private void persistXml(String id, PersistType persistType, StrategyState state, String user, 
			String account, String route, String xml) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
		    tx = session.beginTransaction();
		    @SuppressWarnings("unchecked")
			List<TextObject> list1 = (List<TextObject>)session.createCriteria(TextObject.class)
			    .add( Restrictions.eq("id", id ) )
			    .add(Restrictions.eq("persistType", persistType))
		    .list();
			
		    for(TextObject obj: list1) {
		    	session.delete(obj);
		    }
	        
	        List<TextObject> list2 = TextObject.createTextObjects(id, persistType, state, user, account, route, xml, textSize);
		    for(TextObject obj: list2) {
		    	session.save(obj);
		    }
	        
		    tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
	}
	
	private void deleteXml(String id, PersistType persistType) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
		    tx = session.beginTransaction();
		    @SuppressWarnings("unchecked")
			List<TextObject> list1 = (List<TextObject>)session.createCriteria(TextObject.class)
			    .add( Restrictions.eq("id", id ) )
			    .add( Restrictions.eq("persistType", persistType))
		    .list();
			
		    for(TextObject obj: list1) {
		    	session.delete(obj);
		    }
	        
		    tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
	}
	
	public void processAsyncTimerEvent(AsyncTimerEvent event) {
		if(syncCentralDb)
			log.debug("Received AsyncTimerEvent, connection:" + centralDbConnector.updateConnection());
	}
		
	public void processPmUserLoginEvent(PmUserLoginEvent event)
	{
		log.debug("Received PmUserLoginEvent: " + event.getOriginalEvent().getUserId());
		String userId = event.getOriginalEvent().getUserId().toLowerCase();
		UserKeeper userKeeper = (UserKeeper)event.getUserKeeper();
		AccountKeeper accountKeeper = (AccountKeeper)event.getAccountKeeper();
		boolean ok = false;
		String message = "";
		User user = null;
		Account defaultAccount = null;
		List<Account> list = null;

		if(null != userKeeper) {
			Session session = sessionFactory.openSession();
			Transaction tx = null;
			ErrorMessage msg = null;

			try 
			{
				/*
				if(!syncCentralDb || centralDbConnector.userLogin(userId, event.getOriginalEvent().getPassword()))
					ok = userKeeper.login(userId, event.getOriginalEvent().getPassword());
					*/

				if(!syncCentralDb)
				{
					ok = userKeeper.login(userId, event.getOriginalEvent().getPassword());
					if(ok)
					{
						user = userKeeper.getUser(userId);

						if(null != user.getDefaultAccount() && !user.getDefaultAccount().isEmpty()) {
							defaultAccount = accountKeeper.getAccount(user.getDefaultAccount());
						} 
						
						list = accountKeeper.getAccounts(userId);
						
						if(defaultAccount == null && (list == null || list.size() <= 0)) {
							ok = false;
							//message = "No trading account available for this user";
							message = MessageLookup.buildEventMessage(ErrorMessage.NO_TRADING_ACCOUNT,"No trading account available for this user");
						}
					}
					else{
						
						//message = "userid or password invalid";
						message = MessageLookup.buildEventMessage(ErrorMessage.INVALID_USER_ACCOUNT_PWD,"userid or password invalid");

					}
				}
				else
				{
					user = centralDbConnector.userLoginEx(userId, event.getOriginalEvent().getPassword());
					if(null != user) // login successful from mysql
					{
						if (user.getTerminationStatus().isTerminated()) {
							ok = false;
							msg = ErrorMessage.USER_IS_TERMINATED;
							throw new UserException("User is terminated");
						}

						ok = userKeeper.userExists(userId);
						if(!ok) // user created by another LTS, must be created here again
						{
							//generating default Account
							String defaultAccountId = user.getDefaultAccount();
							if(null == user.getDefaultAccount() || user.getDefaultAccount().equals("")) {
								if(!accountKeeper.accountExists(user.getId() + "-" + Default.getMarket())) {
									defaultAccountId = user.getId() + "-" + Default.getMarket();
								} else {
									defaultAccountId = Default.getAccountPrefix() + IdGenerator.getInstance().getNextSimpleId();
									if(accountKeeper.accountExists(defaultAccountId)) {
										msg = ErrorMessage.CREATE_USER_FAILED;
										throw new UserException("[PmUserLoginEvent]Cannot create default account for user: " +
												user.getId() + ", last try: " + defaultAccountId);
									}
								}
							}
							
							//account creating process
							defaultAccount = new Account(defaultAccountId, userId);
							user.setDefaultAccount(defaultAccountId);
							accountKeeper.setupAccount(defaultAccount);
							createAccount(defaultAccount);
							list = new ArrayList<Account>();
							list.add(defaultAccount);
							eventManager.sendEvent(new OnUserCreatedEvent(user, list));
							eventManager.sendRemoteEvent(new AccountUpdateEvent(event.getOriginalEvent().getKey(), null, defaultAccount));
							
							tx = session.beginTransaction();
							session.save(user);
							tx.commit();
							log.info("[PmUserLoginEvent] Created user: " + userId);
							ok = true;
							
						}
						else //user exists in derby
						{
							user = userKeeper.getUser(userId);
							list = accountKeeper.getAccounts(userId);
						}
					}
					else
					{
						ok = false;
						//message = "userid or password invalid";
						message = MessageLookup.buildEventMessage(ErrorMessage.INVALID_USER_ACCOUNT_PWD,"userid or password invalid");

					}
				}
				
				if(ok == true && null == user.getDefaultAccount() ) {
					ok = false;
					msg = ErrorMessage.NO_TRADING_ACCOUNT;
					throw new UserException("[PmUserLoginEvent]No trading account available for this user: " + userId);
				}

			} catch (Exception ue) {
				
				log.error(ue.getMessage(), ue);
				//message = ue.getMessage();
				if(ue instanceof UserException){
					message = MessageLookup.buildEventMessage(msg,ue.getMessage());
					if(msg==null)
						message = MessageLookup.buildEventMessage(ErrorMessage.INVALID_USER_ACCOUNT_PWD,ue.getMessage());					
				}else{
					message = MessageLookup.buildEventMessage(ErrorMessage.EXCEPTION_MESSAGE,ue.getMessage());
				}		
				
			    if (tx!=null) 
			    	tx.rollback();
			    
			}
			finally {
				session.close();
			}
			
		} else {
			ok = false;
			//message = "Server is not set up for login";
			message = MessageLookup.buildEventMessage(ErrorMessage.SYSTEM_NOT_READY,"Server is not set up for login");

		}
		
		try {
			eventManager.sendRemoteEvent(new UserLoginReplyEvent(event.getOriginalEvent().getKey(),
                    event.getOriginalEvent().getSender(), user, defaultAccount, list, ok, message, event.getOriginalEvent().getTxId()));
			
			if(ok) {
				user.setLastLogin(Clock.getInstance().now());
				eventManager.sendEvent(new PmUpdateUserEvent(PersistenceManager.ID, null, user));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		log.info("Login: " + event.getOriginalEvent().getUserId() + ", " + ok);
	}

	public void processPmUserCreateAndLoginEvent(PmUserCreateAndLoginEvent event)
	{
		log.debug("Received PmUserCreateAndLoginEvent: " + event.getOriginalEvent().getUser().getId());
		
		UserKeeper userKeeper = (UserKeeper)event.getUserKeeper();
		AccountKeeper accountKeeper = (AccountKeeper)event.getAccountKeeper();
		boolean ok = true;

		if(null == event.getUser())	//user exist , getAccount
		{
            ok = loginAndGetAccount(event, userKeeper, accountKeeper);
			log.info("Login: " + event.getOriginalEvent().getUser().getId() + ", " + ok);
		}
        else if (Strings.isNullOrEmpty(event.getUser().getId()))
        {
            ok = loginFromThirdPartyIdAndGetAccount(event, userKeeper, accountKeeper);
            log.info("Login 3rd: " + event.getOriginalEvent().getThirdPartyId() + ", " + ok);
        }
		else	//user not exist, create user and then getAccount
		{
            ok = createUserAndGetAccount(event);
			log.info("CreateAndLogin: " + event.getOriginalEvent().getUser().getId() + ", " + ok);
		}
	}

    private boolean createUserAndGetAccount(PmUserCreateAndLoginEvent event) {
        boolean ok;
        Session session = sessionFactory.openSession();
        Account defaultAccount = event.getAccounts().size() > 0 ? event.getAccounts().get(0) : null;
        User user = event.getUser();
        Transaction tx = null;
        ok = true;
        ErrorMessage msg = null;
        String message = "";

        try {
			if (Strings.isNullOrEmpty(event.getOriginalEvent().getThirdPartyId()) &&
					centralDbConnector.isThirdPartyUserAnyMappingExist(user.getId())) {

				throw new CentralDbException("This third party id is already used in the new version app",
						ErrorMessage.THIRD_PARTY_ID_USED_IN_NEW_APP);
			}

            createCentralDbUser(event, user);

            // the 3rd user type is recorded in THIRD_PARTY_USER table.
            if (user.getUserType().isThirdParty() && !Strings.isNullOrEmpty(event.getOriginalEvent().getThirdPartyId())) {
                user.setUserType(UserType.NORMAL);
            }

            tx = session.beginTransaction();
            session.save(user);
            tx.commit();
            log.info("Created user: " + event.getUser());
        } catch (Exception e) {
            if (e instanceof CentralDbException) {
                msg = ((CentralDbException) e).getClientMessage();
                log.warn(e.getMessage(), e);
            } else {
                msg = ErrorMessage.EXCEPTION_MESSAGE;
                log.error(e.getMessage(), e);
            }

            ok = false;
            message = MessageLookup.buildEventMessageWithCode(msg, e.getMessage());

            if (tx != null)
                tx.rollback();
        } finally {
            session.close();
        }

        if (ok) {
            for (Account account : event.getAccounts()) {
                createAccount(account);
            }

            eventManager.sendEvent(new OnUserCreatedEvent(user, event.getAccounts()));
        }

        if (event.getOriginalEvent() != null) {
            try {
                eventManager.sendRemoteEvent(new UserCreateAndLoginReplyEvent(event.getOriginalEvent().getKey(),
                        event.getOriginalEvent().getSender(), user, defaultAccount, event.getAccounts(), ok, event.getOriginalEvent().getOriginalID()
                        , message, event.getOriginalEvent().getTxId(), true));
                if (ok) {
                    for (Account account : event.getAccounts())
                        eventManager.sendRemoteEvent(new AccountUpdateEvent(event.getOriginalEvent().getKey(), null, account));
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return ok;
    }

    private boolean loginAndGetAccount(PmUserCreateAndLoginEvent event, UserKeeper userKeeper, AccountKeeper accountKeeper) {
        boolean ok = true;
        Account defaultAccount = null;
        List<Account> list = null;
        String message = "";
        User user = null;
        ErrorMessage msg = null;

        try {
			if (!Strings.isNullOrEmpty(event.getOriginalEvent().getThirdPartyId())) {

                // login (backward compatibility, old version skip this.)
                user = centralDbConnector.userLoginEx(event.getOriginalEvent().getUser().getId(), event.getOriginalEvent().getUser().getPassword());

                if (null == user) {
                    ok = false;
                    msg = ErrorMessage.INVALID_USER_ACCOUNT_PWD;
                    throw new UserException("userid or password invalid");
                }

                if (user.getTerminationStatus().isTerminated()) {
                    ok = false;
                    msg = ErrorMessage.USER_IS_TERMINATED;
                    throw new UserException("User is terminated");
                }
				
				String userId = centralDbConnector.getUserIdFromThirdPartyId(event.getOriginalEvent().getThirdPartyId(),
                        event.getOriginalEvent().getMarket(), event.getOriginalEvent().getLanguage());

				if (Strings.isNullOrEmpty(userId)) {

					if (!centralDbConnector.registerThirdPartyUser(event.getOriginalEvent().getUser().getId(),
							event.getOriginalEvent().getUser().getUserType(), event.getOriginalEvent().getThirdPartyId(),
                            event.getOriginalEvent().getMarket(), event.getOriginalEvent().getLanguage())) {

						ok = false;
						msg = ErrorMessage.THIRD_PARTY_ID_REGISTER_FAILED;
						throw new UserException("Register third party id failed");
					}

				} else {
					if (!userId.equals(event.getOriginalEvent().getUser().getId())) {

						ok = false;
						msg = ErrorMessage.THIRD_PARTY_ID_NOT_MATCH_USER_ID;
						throw new UserException("Third party id is not match with the user id");
					}
				}
			}

			// getAccount
            user = userKeeper.getUser(event.getOriginalEvent().getUser().getId());

            if (null != user.getDefaultAccount() && !user.getDefaultAccount().isEmpty()) {
                defaultAccount = accountKeeper.getAccount(user.getDefaultAccount());
            }

            list = accountKeeper.getAccounts(event.getOriginalEvent().getUser().getId());

            if (defaultAccount == null && (list == null || list.size() <= 0)) {
                ok = false;
                message = MessageLookup.buildEventMessage(ErrorMessage.NO_TRADING_ACCOUNT, "No trading account available for this user");
            }

        } catch (Exception ue) {

            log.error(ue.getMessage(), ue);

            if(ue instanceof UserException){

                message = MessageLookup.buildEventMessage(msg, ue.getMessage());
            }else{
                message = MessageLookup.buildEventMessage(ErrorMessage.EXCEPTION_MESSAGE,ue.getMessage());
			}
		}

		try {
			eventManager.sendRemoteEvent(new UserCreateAndLoginReplyEvent(event.getOriginalEvent().getKey(),
					event.getOriginalEvent().getSender(), user, defaultAccount, list, ok, event.getOriginalEvent().getOriginalID(),
                    message, event.getOriginalEvent().getTxId(), false));
            if(ok) {
                user.setLastLogin(Clock.getInstance().now());
                eventManager.sendEvent(new PmUpdateUserEvent(PersistenceManager.ID, null, user));
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return ok;
    }

    private boolean loginFromThirdPartyIdAndGetAccount(PmUserCreateAndLoginEvent event, UserKeeper userKeeper, AccountKeeper accountKeeper) {
        boolean ok = true;
        Account defaultAccount = null;
        List<Account> list = null;
        String message = "";
        User user = null;
        ErrorMessage msg = null;

        try {

            String userId = centralDbConnector.getUserIdFromThirdPartyId(event.getOriginalEvent().getThirdPartyId(),
                    event.getOriginalEvent().getMarket(), event.getOriginalEvent().getLanguage());

            if (Strings.isNullOrEmpty(userId) && /* phase 1 */ !centralDbConnector.isUserExist(event.getOriginalEvent().getThirdPartyId().toLowerCase())) {

                ok = false;
                msg = ErrorMessage.INVALID_USER_ACCOUNT_PWD;
                throw new UserException("userid or password invalid");
            }

            /* phase 1 */
            if (Strings.isNullOrEmpty(userId)) {
                userId = event.getOriginalEvent().getThirdPartyId();
            }

            event.getOriginalEvent().getUser().setId(userId);

            // TODO: Get user from MySQL and check termination?

            user = userKeeper.getUser(event.getOriginalEvent().getUser().getId());

            if (null != user.getDefaultAccount() && !user.getDefaultAccount().isEmpty()) {
                defaultAccount = accountKeeper.getAccount(user.getDefaultAccount());
            }

            list = accountKeeper.getAccounts(event.getOriginalEvent().getUser().getId());

            if (defaultAccount == null && (list == null || list.size() <= 0)) {
                ok = false;
                message = MessageLookup.buildEventMessage(ErrorMessage.NO_TRADING_ACCOUNT, "No trading account available for this user");
            }

        } catch (Exception ue) {

            log.error(ue.getMessage(), ue);

            if(ue instanceof UserException){

                message = MessageLookup.buildEventMessage(msg, ue.getMessage());
            }else{
                message = MessageLookup.buildEventMessage(ErrorMessage.EXCEPTION_MESSAGE,ue.getMessage());
			}
		}

		try {
			eventManager.sendRemoteEvent(new UserCreateAndLoginReplyEvent(event.getOriginalEvent().getKey(),
					event.getOriginalEvent().getSender(), user, defaultAccount, list, ok, event.getOriginalEvent().getOriginalID(),
                    message, event.getOriginalEvent().getTxId(), false));
            if(ok) {
                user.setLastLogin(Clock.getInstance().now());
                eventManager.sendEvent(new PmUpdateUserEvent(PersistenceManager.ID, null, user));
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return ok;
    }

    private void createCentralDbUser(PmUserCreateAndLoginEvent event, User user) throws CentralDbException {
        if (syncCentralDb) {
            if (!centralDbConnector.isUserExist(user.getId())) { // user dose not exist in Mysql either

                if (checkEmailUnique.equals(CheckEmailType.allCheck) ||
                        (checkEmailUnique.equals(CheckEmailType.onlyExist) && !Strings.isNullOrEmpty(user.getEmail()))) {
                    if (centralDbConnector.isEmailExist(user.getEmail())) {
                        //fixed because of #3090
//						throw new CentralDbException("This email already exists: " + user.getEmail());
                        throw new CentralDbException("Your " + user.getUserType().name() +
                                " account email has been used to register an FDT Account. Please login it with " +
                                user.getEmail() + " now.", ErrorMessage.CREATE_USER_FAILED);
                    }
                }

                if (checkPhoneUnique == CheckPhoneType.allCheck ||
                        (checkPhoneUnique == CheckPhoneType.onlyExist && !Strings.isNullOrEmpty(user.getPhone()))) {

                    if (centralDbConnector.isPhoneExist(user.getPhone())) {
                        throw new CentralDbException("Your phone has been used to register an FDT Account.",
                                ErrorMessage.USER_PHONE_EXIST);
                    }
                }

                if (!centralDbConnector.registerUser(user.getId(), user.getName(), user.getPassword(), user.getEmail(),
                        user.getPhone(), user.getUserType(), event.getOriginalEvent().getCountry(),
                        event.getOriginalEvent().getLanguage(), event.getOriginalEvent().getThirdPartyId(),
                        event.getOriginalEvent().getMarket())) {

                    throw new CentralDbException("can't create this user: " + user.getId(), ErrorMessage.CREATE_USER_FAILED);
                }
            }
        }
    }


    public void processSignalEvent(SignalEvent event) {
		persistXml(event.getKey(), PersistType.SIGNAL, StrategyState.Running, null, null, null, event.getSignal().toCompactXML());
	}
	
	public void processCancelSignalEvent(CancelSignalEvent event) {
		log.info("Deleting signal: " + event.getKey());
		deleteXml(event.getKey(), PersistType.SIGNAL);
	}
	
	public void processUpdateParentOrderEvent(UpdateParentOrderEvent event) {
		ParentOrder order = event.getParent();
		StrategyState state = order.getState();
		persistXml(order.getId(), PersistType.SINGLE_ORDER_STRATEGY, state, order.getUser(), order.getAccount(), order.getRoute(), order.toCompactXML());
	}

	public void processMultiInstrumentStrategyUpdateEvent(MultiInstrumentStrategyUpdateEvent event) {
		MultiInstrumentStrategyData data = event.getStrategyData();
		StrategyState state = data.getState();
		persistXml(data.getId(), PersistType.MULTI_INSTRUMENT_STRATEGY, state, data.getUser(), data.getAccount(), data.getRoute(), data.toCompactXML());
	}

	public void processSingleInstrumentStrategyUpdateEvent(SingleInstrumentStrategyUpdateEvent event) {
		Instrument data = event.getInstrument();
		StrategyState state = data.getState();
		persistXml(data.getId(), PersistType.SINGLE_INSTRUMENT_STRATEGY, state, data.getUser(), data.getAccount(), data.getRoute(), data.toCompactXML());
	}

	public void processUpdateChildOrderEvent(UpdateChildOrderEvent event) {
		Session session = sessionFactory.openSession();
		ChildOrder order = event.getOrder();
		ChildOrderAudit audit = new ChildOrderAudit(event.getExecType(), order);
		Transaction tx = null;
		try {
		    tx = session.beginTransaction();
		    if(order.getOrdStatus().isCompleted()) {
		    	session.delete(order);
		    } else {
		    	session.saveOrUpdate(order);
		    }
	    	session.save(audit);
	    	
	    	if(event.getExecution() != null) {
	    		session.save(event.getExecution());
	    	}
		    tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<User> recoverUsers() {
		Session session = sessionFactory.openSession();
		List<User> result = new ArrayList<User>();
		try {
			result = (List<User>)session.createCriteria(User.class)
				.list();
		} catch (HibernateException e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			session.close();
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public List<Account> recoverAccounts() {
		Session session = sessionFactory.openSession();
		List<Account> result = new ArrayList<Account>();
		try {
			result = (List<Account>)session.createCriteria(Account.class)
				.list();
		} catch (HibernateException e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			session.close();
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public List<AccountSetting> recoverAccountSettings() {
		Session session = sessionFactory.openSession();
		List<AccountSetting> result = new ArrayList<AccountSetting>();
		try {
			result = (List<AccountSetting>)session.createCriteria(AccountSetting.class)
				.list();
		} catch (HibernateException e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			session.close();
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public List<OpenPosition> recoverOpenPositions() {
		Session session = sessionFactory.openSession();
		List<OpenPosition> result = new ArrayList<OpenPosition>();
		try {
			result = (List<OpenPosition>)session.createCriteria(OpenPosition.class)
					.addOrder(Order.asc("created"))
				//.add(Restrictions.eq("class", OpenPosition.class)) not working!!!
				.list();
		} catch (HibernateException e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			session.close();
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public List<ClosedPosition> recoverClosedPositions() {
		Session session = sessionFactory.openSession();
		List<ClosedPosition> result = new ArrayList<ClosedPosition>();
		try {
			result = (List<ClosedPosition>)session.createCriteria(ClosedPosition.class)
				.addOrder(Order.asc("created"))
				.list();
		} catch (HibernateException e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			session.close();
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public List<Execution> recoverExecutions() {
		Session session = sessionFactory.openSession();
		List<Execution> result = new ArrayList<Execution>();
		try {
			result = (List<Execution>)session.createCriteria(Execution.class)
				.add( Restrictions.eq("serverId", IdGenerator.getInstance().getSystemId()))
				.add(Restrictions.gt("created", todayOnly ? TimeUtil.getOnlyDate(Clock.getInstance().now()) : new Date(0)))
				.addOrder(Order.asc("created"))
				.list();
		} catch (HibernateException e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			session.close();
		}
		return result;
	}

	private void processTextObject(List<DataObject> result, List<String> toBeRemoved, DataObject dataObject) {
		result.add(dataObject);
		if(deleteTerminated) {
			StrategyState state = dataObject.get(StrategyState.class, OrderField.STATE.value());
			if(null != state && state.equals(StrategyState.Terminated))
				toBeRemoved.add(dataObject.get(String.class, OrderField.ID.value()));
		}
	}
	
	public List<DataObject> recoverObject(PersistType persistType) {
		Session session = sessionFactory.openSession();
		
		List<DataObject> result = new ArrayList<DataObject>();
		List<String> toBeRemoved = new ArrayList<String>();
		try {
			@SuppressWarnings("unchecked")
			List<TextObject> list = (List<TextObject>)session.createCriteria(TextObject.class)
			.add(Restrictions.eq("serverId", IdGenerator.getInstance().getSystemId()))
			.add(Restrictions.eq("persistType", persistType))
			.add(Restrictions.gt("timeStamp", todayOnly?TimeUtil.getOnlyDate(Clock.getInstance().now()):new Date(0)))
			.addOrder( Order.asc("id") ) 
			.addOrder( Order.asc("line") ) 
			.list();

			String currentId = "";
			StringBuilder xml = new StringBuilder();
			for(TextObject obj: list) {
				if(!currentId.equals(obj.getId())) {
					if(xml.length() != 0) {
						DataObject dataObject = DataObject.fromString(DataObject.class, xml.toString());
						processTextObject(result, toBeRemoved, dataObject);
					}
					currentId = obj.getId();
					xml.setLength(0);
				}
				xml.append(obj.getXml());
			}
			if(xml.length() != 0) {
				DataObject dataObject = DataObject.fromString(DataObject.class, xml.toString());
				processTextObject(result, toBeRemoved, dataObject);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			session.close();
		}
		log.info("loaded " + persistType + " " + result.size());
		session = sessionFactory.openSession();
		try {
			for(String id: toBeRemoved) {
				Transaction tx = session.beginTransaction();
			    @SuppressWarnings("unchecked")
				List<TextObject> list = (List<TextObject>)session.createCriteria(TextObject.class)
				    .add( Restrictions.eq("id", id ) )
			    .list();
				
			    for(TextObject obj: list) {
			    	session.delete(obj);
			    }
			    tx.commit();
			}
			log.info("Deleted " + persistType + " " + toBeRemoved.size() + " terminated items");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			session.close();
		}
		
		return result;
	}
	
	public void processPmPositionPeakPriceDeleteEvent(PmPositionPeakPriceDeleteEvent event) {
		Session session = null;
		try{
			
			session = sessionFactory.openSession();

			PositionPeakPrice ppp =  event.getItem();
					
			session.delete(ppp);
			
			session.flush();
			
		}catch(Exception e){
			
			log.error(e.getMessage(),e);
					
		}finally{
		
			if(null != session)
				session.close();
		}	
	}
	
	public void processPmPositionPeakPriceUpdateEvent(PmPositionPeakPriceUpdateEvent event) {
		Session session = null;
		Transaction tx = null;
		try{
			
			session = sessionFactory.openSession();
			tx = session.beginTransaction();
			tx.begin();
			for(PositionPeakPrice ppp: event.getUpdates()) {				
				session.saveOrUpdate(ppp);
			}
			tx.commit();
			
		}catch(Exception e){
			
			log.error(e.getMessage(),e);
			
			if(null != tx )
				tx.rollback();
			
		}finally{
		
			if(null != session)
				session.close();
		}

	}
	@SuppressWarnings("unchecked")
	public List<PositionPeakPrice> recoverPositionPeakPrices() {
		
		List<PositionPeakPrice> result = new ArrayList<PositionPeakPrice>();
		Session session = null;
		try {
			
			session = sessionFactory.openSession();
			result = (List<PositionPeakPrice>)session.createCriteria(PositionPeakPrice.class)
				.list();
			
		} catch (HibernateException e) {
			
			log.error(e.getMessage(), e);
			
		} finally {
			
			if(session!=null)
				session.close();
			
		}
		return result;
	}
	
	public void processPmCreateUserEvent(PmCreateUserEvent event) {
		Session session = sessionFactory.openSession();
		User user = event.getUser();
		Transaction tx = null;
		boolean ok = true;
		String message = "";
		
		try 
		{
			if(syncCentralDb)
			{
				if(centralDbConnector.isUserExist(user.getId()))
					throw new CentralDbException("This user already exists: " + user.getId(),ErrorMessage.USER_ALREADY_EXIST);
				
				if( checkEmailUnique.equals(CheckEmailType.allCheck) || 
					    (checkEmailUnique.equals(CheckEmailType.onlyExist) && null != user.getEmail() && !user.getEmail().isEmpty())) {
					if(centralDbConnector.isEmailExist(user.getEmail()))
						throw new CentralDbException("This email already exists: " + user.getEmail(),ErrorMessage.USER_EMAIL_EXIST);
				}

                if (checkPhoneUnique == CheckPhoneType.allCheck ||
                        (checkPhoneUnique == CheckPhoneType.onlyExist && !Strings.isNullOrEmpty(user.getPhone()))) {

                    if (centralDbConnector.isPhoneExist(user.getPhone())) {
                        throw new CentralDbException("This phone already exists: " + user.getPhone(),
                                ErrorMessage.USER_PHONE_EXIST);
                    }
                }

				if(!centralDbConnector.registerUser(user.getId(), user.getName(), user.getPassword(), user.getEmail(), 
						user.getPhone(), user.getUserType(), event.getOriginalEvent().getCountry(),
						event.getOriginalEvent().getLanguage()))
					throw new CentralDbException("can't create this user: " + user.getId(),ErrorMessage.CREATE_DEFAULT_ACCOUNT_ERROR);
			}
			
			tx = session.beginTransaction();
			session.save(user);
			tx.commit();
			log.info("Created user: " + event.getUser());
		}
		catch (Exception e) {
			if(e instanceof CentralDbException){
				log.warn(e.getMessage(), e);
				message = MessageLookup.buildEventMessage(((CentralDbException) e).getClientMessage(), String.format("can't create user, err=[%s]", e.getMessage()));
			}else {
				message = MessageLookup.buildEventMessage(ErrorMessage.EXCEPTION_MESSAGE, String.format("can't create user, err=[%s]", e.getMessage()));
				log.error(e.getMessage(), e);
			}
			ok = false;
			//message = String.format("can't create user, err=[%s]", e.getMessage());
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
		
		if(ok)
		{
			for(Account account : event.getAccounts())
				createAccount(account);
			eventManager.sendEvent(new OnUserCreatedEvent(user, event.getAccounts()));
		}
		
		if(event.getOriginalEvent() != null)
		{
			try {
				eventManager.sendRemoteEvent(new CreateUserReplyEvent(event.getOriginalEvent().getKey(), 
						event.getOriginalEvent().getSender(), user, ok, message, event.getOriginalEvent().getTxId()));
				if(ok) {
					for(Account account : event.getAccounts())
						eventManager.sendRemoteEvent(new AccountUpdateEvent(event.getOriginalEvent().getKey(), null, account));
				}

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
	
	public void processPmUpdateUserEvent(PmUpdateUserEvent event) {
		Session session = sessionFactory.openSession();
		User user = event.getUser();
		Transaction tx = null;
		try {
		    tx = session.beginTransaction();
	    	session.update(user);
		    tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
	}
	
	public void processPmCreateAccountEvent(PmCreateAccountEvent event) {
		Account account = event.getAccount();
		createAccount(account);
	}
	
	protected void createAccount(Account account)
	{
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
		    tx = session.beginTransaction();
			session.save(account);
			tx.commit();
			log.debug("Persisted account=[" + account.getUserId() + ":" + account.getId() + "]");
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
	}

	public void processPmUpdateAccountEvent(PmUpdateAccountEvent event) {
		Session session = sessionFactory.openSession();
		Account account = event.getAccount();
		Transaction tx = null;
		try {
		    tx = session.beginTransaction();
	    	session.update(account);
		    tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
	}
	
	public void processPmUpdateDetailOpenPositionEvent(PmUpdateDetailOpenPositionEvent event) {
		Session session = sessionFactory.openSession();
		OpenPosition position = event.getPosition();
		Transaction tx = null;
		try {
		    tx = session.beginTransaction();
	    	session.saveOrUpdate(position);
		    tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
	}
	
	public void processPmRemoveDetailOpenPositionEvent(PmRemoveDetailOpenPositionEvent event) {
		Session session = sessionFactory.openSession();
		OpenPosition position = event.getPosition();
		Transaction tx = null;
        try {
		    tx = session.beginTransaction();
	    	session.delete(position);
		    tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
	}
	
	public void processClosedPositionUpdateEvent(ClosedPositionUpdateEvent event) {
		Session session = sessionFactory.openSession();
		ClosedPosition position = event.getPosition();
		Transaction tx = null;
		try {
		    tx = session.beginTransaction();
	    	session.save(position);
		    tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
		
	}

	public void processPmChangeAccountSettingEvent(PmChangeAccountSettingEvent event) {
		Session session = sessionFactory.openSession();
		AccountSetting accountSetting = event.getAccountSetting();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
	    	session.saveOrUpdate(accountSetting);
		    tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
		
	}
	
	public void processPmEndOfDayRollEvent(PmEndOfDayRollEvent event) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
		    tx = session.beginTransaction();
		    Query query;
		    query = session.getNamedQuery("rollEndOfDay1");
	        query.executeUpdate();
		    query = session.getNamedQuery("rollEndOfDay2");
		    query.setParameter("tradeDate", event.getTradeDateTime());
			query.executeUpdate();
		    query = session.getNamedQuery("rollEndOfDay3");
	        query.executeUpdate();
	        tx.commit();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		    if (tx!=null) 
		    	tx.rollback();
		}
		finally {
			session.close();
		}
		log.info("Account day end processing end");
	}	
	
	public void processChangeUserPasswordEvent(ChangeUserPasswordEvent event)
	{
		boolean ok = false;
		String message = "";
		
		try 
		{
			if(!syncCentralDb || centralDbConnector.changePassword(event.getUser(), event.getOriginalPassword(), event.getNewPassword()))
			{
				ok = true;
				log.info("Change password, user: " + event.getUser());
			}
			else{				
				//message = "can't change user's password";
				message = MessageLookup.buildEventMessage(ErrorMessage.CHANGE_USER_PWD_FAILED, "can't change user's password");
			}
		}
		catch (Exception e) 
		{
			log.error(e.getMessage(), e);
			ok = false;
			//message = String.format("can't change user's password, err=[%s]", e.getMessage());
			message = MessageLookup.buildEventMessage(ErrorMessage.CHANGE_USER_PWD_FAILED, String.format("can't change user's password, err=[%s]", e.getMessage()));

		}

		try {
			eventManager.sendRemoteEvent(new ChangeUserPasswordReplyEvent(event.getKey(),
					event.getSender(), event.getUser(), ok, message, event.getTxId()));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void processUserTerminateEvent(UserTerminateEvent event) {

		boolean ok = false;
		String message = "";

		try {

			if (!syncCentralDb || centralDbConnector.changeTermination(event.getUserId(), event.getTerminationStatus())) {

				ok = true;
				log.info("Change user termination status, user: {} terminate: {}", event.getUserId(), event.getTerminationStatus());

			} else {
				MessageLookup.buildEventMessage(ErrorMessage.TERMINATE_USER_FAILED, String.format("Can't change user termination status"));
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			ok = false;
			message = MessageLookup.buildEventMessage(ErrorMessage.TERMINATE_USER_FAILED, String.format("Can't change user termination status, err=[%s]", e.getMessage()));
		}

		try {
			eventManager.sendRemoteEvent(new UserTerminateReplyEvent(event.getKey(), event.getSender(), ok, message, event.getUserId(), event.getTerminationStatus()));
			eventManager.sendRemoteEvent(new UserTerminateUpdateEvent(event.getKey(), null, event.getUserId(), event.getTerminationStatus()));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

    public void processUserMappingEvent(UserMappingEvent event) {

        if (!syncCentralDb) {
            return;
        }

        boolean userExist= false;
        boolean userThirdPartyExist = false;
		boolean isPendingTransfer = false;
        String userId = event.getUser();

        if (!Strings.isNullOrEmpty(event.getUser())) {
            userExist = centralDbConnector.isUserExist(event.getUser().toLowerCase());
        }

        if (!Strings.isNullOrEmpty(event.getUserThirdParty())) {
            userThirdPartyExist = centralDbConnector.isThirdPartyUserExist(event.getUserThirdParty().toLowerCase(),
					event.getMarket(), event.getLanguage());

            if (userThirdPartyExist && centralDbConnector.isThirdPartyUserPendingTransfer(event.getUserThirdParty().toLowerCase())) {
                isPendingTransfer = true;
            }

            if (userThirdPartyExist && Strings.isNullOrEmpty(event.getUser())) {

                userId = centralDbConnector.getUserIdFromThirdPartyId(event.getUserThirdParty(), event.getMarket(),
                        event.getLanguage());
                userExist = true;
            }
        }

		try {
            UserMappingReplyEvent reply = new UserMappingReplyEvent(event.getKey(), event.getSender(), event.getTxId(),
                    userId, event.getUserThirdParty(), userExist, userThirdPartyExist, event.getMarket(),
                    event.getLanguage(), event.getClientId());
            reply.setTransferring(isPendingTransfer);

			eventManager.sendRemoteEvent(reply);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void processUserMappingDetachEvent(UserMappingDetachEvent event) {

        if (!syncCentralDb) {
            return;
        }

        boolean ok = false;
        String message = "";

        try {
            if (centralDbConnector.detachThirdPartyUser(event.getUser(), event.getPassword(), event.getUserThirdParty(),
                    event.getMarket(), event.getLanguage())) {

                ok = true;
                log.info("Detach third party id, {}", event.toString());
            } else {
                MessageLookup.buildEventMessage(ErrorMessage.DETACH_THIRD_PARTY_ID_FAILED, String.format("Can't detach third party id"));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ok = false;
            message = MessageLookup.buildEventMessage(ErrorMessage.DETACH_THIRD_PARTY_ID_FAILED, String.format("Can't detach third party id, err=[%s]", e.getMessage()));
		}

		try {
			eventManager.sendRemoteEvent(new UserMappingDetachReplyEvent(event.getKey(), event.getSender(), ok, message,
					event.getTxId(), event.getUser(), event.getUserThirdParty(), event.getMarket(), event.getLanguage()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
	
	// getters and setters
	public int getTextSize() {
		return textSize;
	}

	public void setTextSize(int textSize) {
		this.textSize = textSize;
	}

	public boolean isCleanStart() {
		return cleanStart;
	}

	public void setCleanStart(boolean cleanStart) {
		this.cleanStart = cleanStart;
	}

	public boolean isTodayOnly() {
		return todayOnly;
	}

	public void setTodayOnly(boolean todayOnly) {
		this.todayOnly = todayOnly;
	}

	public boolean isDeleteTerminated() {
		return deleteTerminated;
	}

	public void setDeleteTerminated(boolean deleteTerminated) {
		this.deleteTerminated = deleteTerminated;
	}

	public boolean isPersistSignal() {
		return persistSignal;
	}

	public void setPersistSignal(boolean persistSignal) {
		this.persistSignal = persistSignal;
	}

	public boolean isEmbeddedSQLServer() {
		return embeddedSQLServer;
	}

	public void setEmbeddedSQLServer(boolean embeddedSQLServer) {
		this.embeddedSQLServer = embeddedSQLServer;
	}
	
	public boolean isSyncCentralDb(){
		return this.syncCentralDb;
	}
	
	public void setSyncCentralDb(boolean syncCentralDb){
		this.syncCentralDb = syncCentralDb;
	}

	public String getEmbeddedHost() {
		return embeddedHost;
	}

	public void setEmbeddedHost(String embeddedHost) {
		this.embeddedHost = embeddedHost;
	}

	public int getEmbeddedPort() {
		return embeddedPort;
	}

	public void setEmbeddedPort(int embeddedPort) {
		this.embeddedPort = embeddedPort;
	}

	public CheckEmailType isCheckEmailUnique() {
		return checkEmailUnique;
	}

	public void setCheckEmailUnique(CheckEmailType checkEmailUnique) {
		this.checkEmailUnique = checkEmailUnique;
	}

    public CheckPhoneType getCheckPhoneUnique() {
        return checkPhoneUnique;
    }

    public void setCheckPhoneUnique(CheckPhoneType checkPhoneUnique) {
        this.checkPhoneUnique = checkPhoneUnique;
    }

    public long getPurgeOrderDays() {
		return purgeOrderDays;
	}

	public void setPurgeOrderDays(long purgeOrderDays) {
		this.purgeOrderDays = purgeOrderDays;
	}
	
}

