package com.zixu.paysapi.jpa.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="com_zixu_user_log")
public class UserLog implements Serializable{
	
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="id")
	private String id;
	
	@Column(name="account")
	private String account;
	
	@Column(name="loginIp")
	private String loginIp;
	
	@Column(name="changeDate")
	private String changeDate;


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getLoginIp() {
		return loginIp;
	}

	public void setLoginIp(String loginIp) {
		this.loginIp = loginIp;
	}

	public String getChangeDate() {
		return changeDate;
	}

	public void setChangeDate(String changeDate) {
		this.changeDate = changeDate;
	}
	
	
	
}
