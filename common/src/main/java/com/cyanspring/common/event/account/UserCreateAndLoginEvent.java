package com.cyanspring.common.event.account;

import com.cyanspring.common.account.User;
import com.cyanspring.common.event.EventPriority;
import com.cyanspring.common.event.RemoteAsyncEvent;

//Third-Party Authentication
public class UserCreateAndLoginEvent extends RemoteAsyncEvent {
	private User user;
	private String txId;
	private String country;
	private String language;
	private String original_id;
	private String thirdPartyId;
	private String market;
	private boolean existUser;

	private boolean isTransfer = false;
	private boolean updatedEmail = false;

	@Deprecated
	public UserCreateAndLoginEvent(String key, String receiver, User user, String country, String language,
								   String org_id, String txId, String thirdPartyId, String market) {
		this(key, receiver, user, country, language, org_id, txId, thirdPartyId, market, false, false, false);
	}

	public UserCreateAndLoginEvent(String key, String receiver, User user, String country, String language,
								   String org_id, String txId, String thirdPartyId, String market, boolean existUser) {
		this(key, receiver, user, country, language, org_id, txId, thirdPartyId, market, existUser, false, false);
	}

	public UserCreateAndLoginEvent(String key, String receiver, User user, String country, String language,
								   String org_id, String txId, String thirdPartyId, String market, boolean existUser,
								   boolean isTransfer, boolean updatedEmail) {
		super(key, receiver);
		this.user = user;
		this.country = country;
		this.language = language;
		this.txId = txId;
		this.original_id = org_id;
		this.thirdPartyId = thirdPartyId;
		this.market = market;
		this.existUser = existUser;
		this.isTransfer = isTransfer;
		this.updatedEmail = updatedEmail;
		setPriority(EventPriority.HIGH);
	}
	
	public String getOriginalID()
	{
		return original_id;
	}

	public User getUser() {
		return user;
	}

	public String getTxId() {
		return txId;
	}
	
	public String getCountry() {
		return country;
	}
	
	public String getLanguage() {
		return language;
	}

	public String getThirdPartyId() {
		return thirdPartyId;
	}

	public String getMarket() {
		return market;
	}

	public boolean isExistUser() {
		return existUser;
	}

	public boolean isTransfer() {
		return isTransfer;
	}

	public boolean isUpdatedEmail() {
		return updatedEmail;
	}
}
