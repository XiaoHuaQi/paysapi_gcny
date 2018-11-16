package com.zixu.paysapi.jpa.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zixu.paysapi.jpa.dao.RechargeListDao;
import com.zixu.paysapi.jpa.entity.RechargeList;
import com.zixu.paysapi.jpa.service.RechargeListService;
import com.zixu.paysapi.util.SysUtil;

@Service
@Transactional
public class RechargeListServiceImpl implements RechargeListService {
	
	@Autowired
	private RechargeListDao dao;

	@Override
	public List<RechargeList> findAll() {
		
		return dao.findAll();
	}

	@Override
	public RechargeList save(RechargeList rechargeList) {
		
		rechargeList.setId(SysUtil.generalPK());
		
		return dao.save(rechargeList);
	}

	@Override
	public void delete(String id) {
		
		dao.delete(id);
		
		String sql = "delete from com_zixu_recharge_qrcode where rechargeID = ?";
		dao.nativeExecuteUpdate(sql,id);
	}

	@Override
	public RechargeList findByID(String id) {
		return dao.findOne(id);
	}
	
	
	
}
