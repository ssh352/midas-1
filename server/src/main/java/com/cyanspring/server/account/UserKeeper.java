package com.cyanspring.server.account;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cyanspring.common.Default;
import com.cyanspring.common.account.User;
import com.cyanspring.common.account.UserException;
import com.cyanspring.common.account.UserGroup;
import com.cyanspring.common.account.UserRole;
import com.cyanspring.common.account.UserType;
import com.cyanspring.common.business.GroupManagement;
import com.cyanspring.common.message.ErrorMessage;

public class UserKeeper {
	private static final Logger log = LoggerFactory.getLogger(UserKeeper.class);

	private ConcurrentHashMap<String, User> users = new ConcurrentHashMap<String, User>();
	private ConcurrentHashMap<String, UserGroup> userGroups = new ConcurrentHashMap<String, UserGroup>();

	public String CSTWAdmin = "admin";
	public String CSTWPassword = "FDTADMIN";

	public void createUser(User user) throws UserException {
		String lowCases = user.getId().toLowerCase();
		if (users.containsKey(lowCases))
			throw new UserException("User already exists: " + lowCases,
					ErrorMessage.USER_ALREADY_EXIST);

		user.setId(lowCases);
		users.put(user.getId(), user);
	}

	public User getUser(String id) {
		return users.get(id);
	}

	public boolean userExists(String userId) {
		return users.containsKey(userId);
	}

	public UserGroup getUserGroup(String id) {
		return userGroups.get(id);
	}

	public boolean login(String userId, String password) throws UserException {
		String lowCases = userId.toLowerCase();
		User user = getUser(lowCases);
		if (null == user)
			throw new UserException("Invalid user id or password",
					ErrorMessage.INVALID_USER_ACCOUNT_PWD);
		/*
		 * synchronized(user) { if(!user.getId().equals(lowCases) ||
		 * !user.getPassword().equals(password)) throw new
		 * UserException("Invalid user or password"); }
		 */
		return true;
	}

	public User tryCreateDefaultUser() {
		if (!userExists(Default.getUser())) {
			User user = new User(Default.getUser(), "guess?");
			user.setDefaultAccount(Default.getAccount());
			user.setUserType(UserType.SUPPORT);
			user.setRole(UserRole.Admin);
			this.users.putIfAbsent(user.getId(), user);
			return user;
		}
		return null;
	}

	public void injectUsers(List<User> users) {
		for (User user : users) {
			this.users.put(user.getId(), user);
		}
	}

	public void deleteGroup(GroupManagement gm) throws UserException {
		String manager = gm.getManager();
		String managee = gm.getManaged();
		User managerInfo = users.get(manager);
		UserGroup managerGroup = userGroups.get(manager);
		UserGroup manageeGroup = userGroups.get(managee);
		if (null == managerInfo) {
			throw new UserException("Manager:" + manager
					+ " doen't exist in User",
					ErrorMessage.DELETE_GROUP_MANAGEMENT_FAILED);
		}

		if (null == managerGroup) {
			throw new UserException("Manager:" + manager
					+ " doen't exist in UserGroup",
					ErrorMessage.DELETE_GROUP_MANAGEMENT_FAILED);
		}

		if (managerGroup.isGroupPairExist(managee)) {
			try {
				managerGroup.deleteManagee(managee);
				manageeGroup.deleteManager(manager);
			} catch (Exception e) {
				throw new UserException("Exception : " + e.getMessage(),
						ErrorMessage.DELETE_GROUP_MANAGEMENT_FAILED);
			}
		} else {
			throw new UserException("Managee:" + managee
					+ " doen't exist in Manager UserGroup",
					ErrorMessage.DELETE_GROUP_MANAGEMENT_FAILED);
		}
	}

	public void createGroup(GroupManagement gm) throws UserException {
		String manager = gm.getManager();
		String managee = gm.getManaged();
		User managerInfo = users.get(manager);
		User manageeInfo = users.get(managee);
		UserGroup managerGroup = null;
		UserGroup manageeGroup = null;

		if (manager.equals(managee)) {
			throw new UserException("Manager and Managee are same person!",
					ErrorMessage.CREATE_GROUP_MANAGEMENT_FAILED);
		}

		if (null == managerInfo) {
			throw new UserException("Manager:" + manager
					+ " doen't exist in User",
					ErrorMessage.CREATE_GROUP_MANAGEMENT_FAILED);
		}

		if (null == manageeInfo) {
			throw new UserException("Managee:" + managee
					+ " doen't exist in User",
					ErrorMessage.CREATE_GROUP_MANAGEMENT_FAILED);
		}

		log.info("create group:{}-{},{}-{}", new Object[] { manager,
				managerInfo.getRole(), managee, manageeInfo.getRole() });

		if (UserRole.Trader.equals(managerInfo.getRole())) {
			throw new UserException("Manager:" + manager
					+ " is a Trader who can't manage someone else",
					ErrorMessage.CREATE_GROUP_MANAGEMENT_FAILED);
		}

		if (!managerInfo.getRole().isManagerLevel()
				&& !managerInfo.getRole().equals(UserRole.Group)) {
			throw new UserException("Manager:" + manager + " role:"
					+ managerInfo.getRole() + " who can't manage someone else",
					ErrorMessage.CREATE_GROUP_MANAGEMENT_FAILED);
		}

		if (!userGroups.containsKey(manager)) {
			managerGroup = new UserGroup(manager, managerInfo.getRole());
			userGroups.put(manager, managerGroup);
		} else {
			managerGroup = userGroups.get(manager);
		}

		if (!userGroups.containsKey(managee)) {
			manageeGroup = new UserGroup(managee, manageeInfo.getRole());
			userGroups.put(managee, manageeGroup);
		} else {
			manageeGroup = userGroups.get(managee);
		}

		if (managerGroup.isGroupPairExist(managee)) {
			throw new UserException("Managee:" + managee + " already exist!",
					ErrorMessage.CREATE_GROUP_MANAGEMENT_FAILED);
		}

		if (manageeGroup.isOverMaxLevel(managerGroup)) {
			throw new UserException("Manager:" + manager + ", Managee:"
					+ managee + " excceds Max level : " + UserGroup.MAX_LEVEL,
					ErrorMessage.CREATE_GROUP_MANAGEMENT_FAILED);
		}

		/**
		 * if A manage B,check B managees not manage to A
		 */
		if (!manageeGroup.isManageRecursive(manager)) {
			managerGroup.putManagee(manageeGroup);
			manageeGroup.putManager(managerGroup);
		} else {
			throw new UserException("Manager:" + manager + ", Managee:"
					+ managee + " cause recursive management",
					ErrorMessage.CREATE_GROUP_MANAGEMENT_FAILED);
		}
	}

	public void injectGroup(List<GroupManagement> list) {
		for (GroupManagement gm : list) {
			try {
				createGroup(gm);
			} catch (Exception e) {
				log.warn(e.getLocalizedMessage());
			}
		}
	}

	public List<User> getAllUsers() {
		List<User> userList = new ArrayList<User>();
		userList.addAll(users.values());
		return userList;
	}

	public boolean isAdmin(String id, String pwd) {

		if (StringUtils.hasText(id) && StringUtils.hasText(pwd)
				&& id.equals(getCSTWAdmin()) && pwd.equals(getCSTWPassword())) {
			return true;
		}

		return false;
	}

	public List<User> getUsersByRole(UserRole role) {
		List<User> userList = new ArrayList<User>();
		for (User user : users.values()) {
			if (user.getRole() == role) {
				userList.add(user);
			}
		}
		return userList;
	}

	public String getCSTWAdmin() {
		return CSTWAdmin;
	}

	public void setCSTWAdmin(String cSTWAdmin) {
		CSTWAdmin = cSTWAdmin;
	}

	public String getCSTWPassword() {
		return CSTWPassword;
	}

	public void setCSTWPassword(String cSTWPassword) {
		CSTWPassword = cSTWPassword;
	}

}
