package com.zixu.paysapi.jpa.dto;

import java.math.BigDecimal;

public class UserDto{
	

	
	private String id;
	
	private int sumFee;
	
	private BigDecimal alipayQuota;
	
	public BigDecimal getAlipayQuota() {
		return alipayQuota;
	}

	public void setAlipayQuota(BigDecimal alipayQuota) {
		this.alipayQuota = alipayQuota;
	}

	public BigDecimal getWechatQuota() {
		return wechatQuota;
	}

	public void setWechatQuota(BigDecimal wechatQuota) {
		this.wechatQuota = wechatQuota;
	}

	private BigDecimal wechatQuota;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getSumFee() {
		return sumFee;
	}

	public void setSumFee(int sumFee) {
		this.sumFee = sumFee;
	}
	
}
