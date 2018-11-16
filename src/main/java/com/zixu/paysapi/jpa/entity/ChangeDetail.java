package com.zixu.paysapi.jpa.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="com_zixu_change_detail")
public class ChangeDetail implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@Id
	@Column(name="id")
	private String id;
	
	@Column(name="userID")
	private String userID;
	
	@Column(name="time")
	private String time;
	
	@Column(name="remarks")
	private String remarks;
	
	@Column(name="beforeFee")
	private int beforeFee;
	
	@Column(name="afterFee")
	private int afterFee;
	
	@Column(name="changeFee")
	private int changeFee;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public int getBeforeFee() {
		return beforeFee;
	}

	public void setBeforeFee(int beforeFee) {
		this.beforeFee = beforeFee;
	}

	public int getAfterFee() {
		return afterFee;
	}

	public void setAfterFee(int afterFee) {
		this.afterFee = afterFee;
	}

	public int getChangeFee() {
		return changeFee;
	}

	public void setChangeFee(int changeFee) {
		this.changeFee = changeFee;
	}
	

	
}