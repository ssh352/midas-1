package com.cyanspring.common.account;

import java.io.Serializable;
import java.util.Date;

import com.cyanspring.common.Clock;

public class User implements Cloneable, Serializable {
	private String id;
	private String name;
	private String password;
	private String email;
	private String phone;
	private Date created;
	private Date lastLogin;
	private UserType userType = UserType.NORMAL;
	private String defaultAccount;
	private boolean terminated = false;
	
	private User() {
		created = Clock.getInstance().now();
	}
	
	public User(String id, String password) {
		this();
		this.id = id;
		this.password = password;
	}

	public User(String id, String name, String password, String email, String phone,
			UserType userType) {
		this(id, name, password, email, phone, userType, false);
	}

	public User(String id, String name, String password, String email, String phone,
				UserType userType, boolean terminated) {
		this();
		this.id = id;
		this.name = name;
		this.password = password;
		this.phone = phone;
		this.email = email;
		this.userType = userType;
		this.terminated = terminated;
	}

	public synchronized String getName() {
		return name;
	}

	public synchronized void setName(String name) {
		this.name = name;
	}

	public synchronized String getPassword() {
		return password;
	}

	public synchronized void setPassword(String password) {
		this.password = password;
	}

	public synchronized String getEmail() {
		return email;
	}

	public synchronized void setEmail(String email) {
		this.email = email;
	}

	public synchronized UserType getUserType() {
		return userType;
	}

	public synchronized void setUserType(UserType userType) {
		this.userType = userType;
	}

	public synchronized String getDefaultAccount() {
		return defaultAccount;
	}

	public synchronized void setDefaultAccount(String defaultAccount) {
		this.defaultAccount = defaultAccount;
	}

	public synchronized String getId() {
		return id;
	}
	
	public synchronized void setId(String id) {
		this.id = id;
	}

	public synchronized Date getLastLogin() {
		return lastLogin;
	}

	public synchronized boolean isTerminated() {
		return terminated;
	}

	public void setTerminated(boolean terminated) {
		this.terminated = terminated;
	}

	public Date getCreated() {
		return created;
	}
	
	protected void setCreated(Date created) {
		this.created = created;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}
	
	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public synchronized boolean login(String id, String password) {
		boolean ok = this.id == id && this.password == password;
		if(ok)
			lastLogin = Clock.getInstance().now();
		return ok;
	}
	
	@Override
	public synchronized Account clone() throws CloneNotSupportedException {
		return (Account)super.clone();
	}

	@Override
	public synchronized String toString() {
		return "[" + this.id + ", " + this.getDefaultAccount() + ", " + this.lastLogin + "]";
	}

}
