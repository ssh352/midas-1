package com.cyanspring.cstw.service.iservice.admin;

import java.util.List;

import com.cyanspring.common.account.User;
import com.cyanspring.cstw.model.admin.AssignedModel;
import com.cyanspring.cstw.model.admin.ExchangeAccountModel;
import com.cyanspring.cstw.model.admin.InstrumentInfoModel;
import com.cyanspring.cstw.model.admin.SubAccountModel;
import com.cyanspring.cstw.service.iservice.IBasicService;

/**
 * @author Junfeng
 * @create 10 Nov 2015
 */
public interface ISubAccountManagerService extends IBasicService {
	
	void query();
	
	ExchangeAccountModel getExchangeAccoutById(String id);
	
	SubAccountModel getSubAccountById(String id);
	
	List<ExchangeAccountModel> getExchangeAccountList();
	
	List<SubAccountModel> getSubAccountListByExchangeAccountId(String id);
	
	List<InstrumentInfoModel> getInstrumentInfoModelListByExchangeAccountId(String id);
	
	List<InstrumentInfoModel> getInstrumentInfoModelListBySubAccountId(String id);
	
	List<AssignedModel> getAssignedModelListBySubAccountId(String id);
	
	List<User> getAvailableAssigneeList(SubAccountModel subAccount);
	
	void createNewExchangeAccount(String name);
	
	void createNewSubAccount(String exchangeId, String name);
	
	void createNewAssignedModel(SubAccountModel subAccount, AssignedModel assigned, int index);
	
	void removeExchangeAccount(ExchangeAccountModel exchange);
	
	void removeSubAccount(SubAccountModel subAccount);
	
	void removeAssignedUser(SubAccountModel subAccount, AssignedModel assign);
	
	void updateExchangeAccountName(ExchangeAccountModel exchange, String name);
	
	void updateSubAccountName(SubAccountModel subAccount, String name);
	
	void addExchangeInputChangeListener(IInputChangeListener listener);
	
	void addSubAccInputChangeListener(IInputChangeListener listener);
	
	void removeExchangeInputChangeListener(IInputChangeListener listener);
	
	void removeSubAccInputChangeListener(IInputChangeListener listener);
	
}
