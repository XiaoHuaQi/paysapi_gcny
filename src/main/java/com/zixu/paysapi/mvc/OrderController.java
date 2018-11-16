package com.zixu.paysapi.mvc;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.zixu.paysapi.jpa.dto.ReturnDto;
import com.zixu.paysapi.jpa.dto.StatisticsDto;
import com.zixu.paysapi.jpa.dto.UserDto;
import com.zixu.paysapi.jpa.entity.AdminConfig;
import com.zixu.paysapi.jpa.entity.Commodity;
import com.zixu.paysapi.jpa.entity.Config;
import com.zixu.paysapi.jpa.entity.Order;
import com.zixu.paysapi.jpa.entity.Qrcode;
import com.zixu.paysapi.jpa.entity.RechargeList;
import com.zixu.paysapi.jpa.entity.RechargeUserDetailed;
import com.zixu.paysapi.jpa.entity.SetMeal;
import com.zixu.paysapi.jpa.entity.SetMealPurchase;
import com.zixu.paysapi.jpa.entity.User;
import com.zixu.paysapi.jpa.service.AdminConfigService;
import com.zixu.paysapi.jpa.service.ChangeDetailService;
import com.zixu.paysapi.jpa.service.CommodityService;
import com.zixu.paysapi.jpa.service.ConfigService;
import com.zixu.paysapi.jpa.service.OrderService;
import com.zixu.paysapi.jpa.service.QrcodeService;
import com.zixu.paysapi.jpa.service.RechargeListService;
import com.zixu.paysapi.jpa.service.RechargeUserDetailedService;
import com.zixu.paysapi.jpa.service.SetMealPurchaseService;
import com.zixu.paysapi.jpa.service.SetMealService;
import com.zixu.paysapi.jpa.service.UserService;
import com.zixu.paysapi.mvc.util.GcnyOrderAddNotice;
import com.zixu.paysapi.mvc.util.GcnyOrderChangeNotice;
import com.zixu.paysapi.mvc.util.HttpClientUtils;
import com.zixu.paysapi.mvc.util.RequestJson;
import com.zixu.paysapi.util.AsciiOrder;
import com.zixu.paysapi.util.DateUtil;
import com.zixu.paysapi.util.MD5;
import com.zixu.paysapi.util.RandomUtil;
import com.zixu.paysapi.util.SysUtil;
import com.zixuapp.redis.RedisOperationManager;

import net.sf.json.JSONObject;

@RequestMapping("/order")
@Controller
public class OrderController extends Thread{
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private ChangeDetailService changeDetailService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ConfigService configService;
	
	@Autowired
	private QrcodeService qrcodeService;
	
	@Autowired
	private CommodityService commodityService;
	
	@Autowired
	private SetMealPurchaseService setMealPurchaseService;
	
	@Autowired
	private AdminConfigService adminConfigService;
	
	@Autowired
	private SetMealService setMealService;
	
	@Autowired
	private RechargeListService rechargeListService;
	
	@Autowired
	private RechargeUserDetailedService rechargeUserDetailedService;
	
	
	private static String redisKey = "api_pay:";
	
	private String orderID;
	
	@RequestMapping("/api/pay")
	@ResponseBody
	public ReturnDto pay(HttpServletRequest request) throws IOException {
		
		String jsonStr = RequestJson.returnJson(request);
		
		if(jsonStr == null || "".equals(jsonStr)) {
			return ReturnDto.send(100001);
		}
		
		Map<String, String> map = null;
		try {
			map = (Map<String, String>) JSONObject.toBean(JSONObject.fromObject(jsonStr),Map.class);
		} catch (Exception e) {
			return ReturnDto.send(100009);
		}
		
		//String uid = map.get("uid");
		String price = map.get("price");
		String type = map.get("type");
		String notifyUrl = map.get("notifyUrl");
		String outTradeNo = map.get("outTradeNo");
		String nonceStr = map.get("nonceStr");
		String chainAdd = map.get("chain_add");
		String sign = map.get("sign");
		
		//必填校验
		if(chainAdd == null || price == null || type == null || notifyUrl == null || outTradeNo == null || nonceStr == null || sign == null) {
			return ReturnDto.send(100001);
		}
		
		//数据格式校验
		int fee = 0;
		try {
			fee = Integer.valueOf(price);
		} catch (Exception e) {
			return ReturnDto.send(100009);
		}
		if(fee == 0) {
			return ReturnDto.send(100009);
		}
		if(!"wechat".equals(type) && !"alipay".equals(type)) {
			return ReturnDto.send(100010);
		}
		if(outTradeNo.length() > 32) {
			return ReturnDto.send(100009);
		}
		if(nonceStr.length() > 32) {
			return ReturnDto.send(100009);
		}
		
		//获取用户信息
		//User user = userService.findByUid(uid);
		
		//if(user == null) {
		//	return ReturnDto.send(100002);
		//}
		
		//验证签名
		map.remove("sign");
		if(!sign.equals(AsciiOrder.sign(map, "e10adc3949ba59abbe56e057f20f883e"))) {
			return ReturnDto.send(100011);
		}
		
		/*//是否购买套餐
		if(setMealPurchaseService.findByUserIDAndExpireDate(user.getId()) == null) {
			return ReturnDto.send(100019);
		}
		
		//账户余额校验
		int userFee = rechargeUserDetailedService.sum(user.getId());
		if(userFee <= 0) {
			return ReturnDto.send(100018);
		}
		
		//计算手续费
		int purchase = calculation(setMealPurchaseService, user.getId(), fee);
		
		Config config = configService.findById(user.getId());
		if(config == null || config.getOverdueTime() == 0) {
			config = new Config();
			config.setOverdueTime(300);
		}
		Commodity commodity =  commodityService.findByFee(fee, user.getId());
		if(commodity == null) {
			return ReturnDto.send(100020);
		}
		
		//获取金额二维码
		String userID = user.getId();
		if(userFee < purchase) {
			userID = null;
		}
		*/
		Map<String, String> qrinfo = RandomUser(userService, fee, setMealPurchaseService, commodityService, qrcodeService, type);
		
		if(qrinfo == null) {
			return ReturnDto.send(100012);
		}
		String finalFee = qrinfo.get("fee");
		String qrcodeUrl = qrinfo.get("qrcodeUrl");
		String commodityID = qrinfo.get("commodityID");
		String userID = qrinfo.get("userID");
		String procedures = qrinfo.get("procedures");
		String proceduresFee = qrinfo.get("proceduresFee");
		User user = userService.findById(userID);
		
		//添加数据
		Order order = new Order();
		order.setCommdityID(commodityID);
		order.setCommdityName(String.valueOf(fee));
		order.setNonceStr(nonceStr);
		order.setNotifyState("0");
		order.setPayState("0");
		order.setPrice(Integer.valueOf(finalFee));
		order.setType(type);
		order.setUserID(user.getId());
		order.setOutTradeNo(outTradeNo);
		order.setUid(user.getUid());
		order.setNotifyUrl(notifyUrl);
		order.setQrcodeUrl(qrcodeUrl);
		order.setOrderType("0");
		order.setProcedures(0);
		order.setProceduresFee(0);
		order.setChainAdd(chainAdd);
		order.setProcedures(Integer.valueOf(procedures));
		order.setProceduresFee(Integer.valueOf(proceduresFee));
		order = orderService.save(order);
		if(order == null) {
			return ReturnDto.send(100005);
		}
		
		//冻结商户余额
		RechargeUserDetailed rechargeUserDetailed = new RechargeUserDetailed();
		rechargeUserDetailed.setFee(-Integer.valueOf(proceduresFee));
		rechargeUserDetailed.setUserID(order.getUserID());
		rechargeUserDetailed.setRemarks("下单冻结手续费");
		rechargeUserDetailed.setOutTradeNo(outTradeNo);
		rechargeUserDetailed =  rechargeUserDetailedService.save(rechargeUserDetailed);
		if(rechargeUserDetailed == null) {
			return ReturnDto.send(100005);
		}
		
		
		Map<String, String> res = new HashMap<>();
		res.put("uid", user.getUid());
		res.put("outTradeNo", outTradeNo);
		res.put("qrcodeUrl", qrcodeUrl);
		res.put("price", finalFee);
		res.put("overdueTime", "360");
		
		GcnyOrderAddNotice notice = new GcnyOrderAddNotice(outTradeNo, orderService);
		notice.start();
		
		return ReturnDto.send(res);
	}
	

	@RequestMapping("/api/query")
	@ResponseBody
	public ReturnDto query(HttpServletRequest request) throws IOException {
		
		String jsonStr = RequestJson.returnJson(request);
		
		if(jsonStr == null || "".equals(jsonStr)) {
			return ReturnDto.send(100001);
		}
		
		Map<String, String> map = (Map<String, String>) JSONObject.toBean(JSONObject.fromObject(jsonStr),Map.class);
		
		String uid = map.get("uid");
		String outTradeNo = map.get("outTradeNo");
		String nonceStr = map.get("nonceStr");
		String sign = map.get("sign");
		
		//必填校验
		if(uid == null || outTradeNo == null || nonceStr == null || sign == null) {
			return ReturnDto.send(100001);
		}
		
		//获取用户信息
		User user = userService.findByUid(uid);
		
		if(user == null) {
			return ReturnDto.send(100002);
		}
		
		//验证签名
		map.remove("sign");
		if(!sign.equals(AsciiOrder.sign(map, user.getToken()))) {
			return ReturnDto.send(100011);
		}
		
		Order order = orderService.findByOutTradeNo(user.getId(),outTradeNo);
		if(order == null) {
			return ReturnDto.send(100013);
		}
		Map<String, String> res = new HashMap<>();
		res.put("outTradeNo", outTradeNo);
		res.put("price", String.valueOf(order.getPrice()));
		res.put("uid", uid);
		res.put("payState", order.getPayState());
		return ReturnDto.send(res);
	}
	
	
	public static Map<String, String> getQrCode(int fee,QrcodeService qrcodeService,String type){
		return null;
		/*int purchase = 
		if(time == 0) {
			time = 300;
		}
		Map<String, String> res = new HashMap<>();
		if(userID != null) {
			
		}
		List<Qrcode>  list = qrcodeService.findByQrcodeList(userID, commodityID);
		if(list == null || list.size() == 0) {
			res.put("errorCode", "100012");
			return res;
		}
		for (Qrcode qrcode : list) {
			
			String qrcodeUrl = RedisOperationManager.getString(redisKey+"fee="+qrcode.getFee()+"&userID="+userID+"&type="+type);
			
			if(qrcodeUrl != null) {
				continue;
			}
			if(qrcode.getFee() == 0) {
				res.put("errorCode", "100012");
				return res;
			}
			res.put("fee", String.valueOf(qrcode.getFee()));
			res.put("qrcodeUrl", qrcode.getUrl());
			
			RedisOperationManager.setString(redisKey+"fee="+qrcode.getFee()+"&userID="+userID+"&type="+type, qrcode.getUrl(), time);
			
			return res;
			
		}
		res.put("errorCode", "100012");
		return res;*/
		/*
		
		
		if(qrcodeUrl != null) {
			return getQrCode(fee-1, qrcodeService, time, userID,type);
		}
		
		Qrcode qrcode =  qrcodeService.findByFee(fee, userID, type);
		if(qrcode != null) {
			Map<String, String> res = new HashMap<>();
			res.put("fee", String.valueOf(fee));
			res.put("qrcodeUrl", qrcode.getUrl());
			res.put("commdityID", qrcode.getCommodityID());
			
			RedisOperationManager.setString(redisKey+"fee="+fee+"&userID="+userID+"&type="+type, qrcode.getUrl(), time);
			
			return res;
		}else {
			Map<String, String> res = new HashMap<>();
			qrcode =  qrcodeService.findByFee(0, userID, type);
			if(qrcode == null) {
				res.put("errorCode", "100012");
				return res;
			}
			res.put("fee", String.valueOf(fee));
			res.put("qrcodeUrl", qrcode.getUrl());
			res.put("commdityID", qrcode.getCommodityID());
			
			RedisOperationManager.setString(redisKey+"fee="+fee+"&userID="+userID+"&type="+type, qrcode.getUrl(), time);
			
			return res;
		}*/
	}
	
	@RequestMapping("/client/transmit")
	@ResponseBody
	public void transmit(HttpServletRequest request,HttpServletResponse response) throws IOException {
		
		PrintWriter writer =  response.getWriter();
		
		String jsonStr = RequestJson.returnJson(request);
		if(jsonStr == null || "".equals(jsonStr)) {
			writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100001)));
			writer.close();
			return;
		}
		Map<String, String> map = (Map<String, String>) JSONObject.toBean(JSONObject.fromObject(jsonStr),Map.class);
		
		if(map.get("userName") == null || map.get("password") == null || map.get("price") == null || map.get("type") == null) {
			writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100001)));
			writer.close();
			return;
		}
		
		String userName = map.get("userName");
		String password = map.get("password");
		String price = map.get("price");
		String type = map.get("type");
		
		int fee = 0;
		try {
			fee = Integer.valueOf(price);
		} catch (Exception e) {
			writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100009)));
			writer.close();
			return;
		}
		
		if(!type.equals("wechat") && !type.equals("alipay")) {
			writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100010)));
			writer.close();
			return;
		}
		
		User user = userService.findByName(userName);
		if(user == null) {
			writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100002)));
			writer.close();
			return;
		}
		
		if(!user.getPassword().equals(MD5.MD5Encode(password))) {
			writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100003)));
			writer.close();
			return;
		}
		
		if(userName.equals("admin")) {
			
			Order order = orderService.findByFee(fee, type);
			
			if(order != null) {
				
				order.setPayState("1");
				order = orderService.save(order);
				
				if(order.getOrderType().equals("1")) {
					//购买套餐
					SetMeal setMeal = setMealService.findById(order.getCommdityID());
					if(setMeal == null) {
						writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100005)));
						writer.close();
						return;
					}
					int number = setMeal.getNumber();
					if("1".equals(setMeal.getType())) {
						number = number * 1;//日
					}else if("2".equals(setMeal.getType())) {
						number = number * 30;//月
					}else if("3".equals(setMeal.getType())) {
						number = number * 365;//年
					}else {
						writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100005)));
						writer.close();
						return;
					}
					
					SetMealPurchase setMealPurchase =  setMealPurchaseService.save(order.getUserID(), String.valueOf(number),setMeal.getProcedures(),setMeal.getId());
					if(setMealPurchase == null) {
						writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100005)));
						writer.close();
						return;
					}
					writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(true)));
					writer.close();
					
					AdminConfig adminConfig = adminConfigService.findByOne();
					if(adminConfig != null && "1".equals(adminConfig.getImmediately())) {
						RedisOperationManager.del("pay_set_meal:"+"fee="+fee+"&type="+type);
					}
					
					/*RechargeUserDetailed rechargeUserDetailed = new RechargeUserDetailed();
					rechargeUserDetailed.setFee(-order.getPrice());
					rechargeUserDetailed.setUserID(order.getUserID());
					rechargeUserDetailed.setRemarks("购买套餐");
					rechargeUserDetailed =  rechargeUserDetailedService.save(rechargeUserDetailed);*/
					
					return;
					
				}else if(order.getOrderType().equals("2")) {
					
					RechargeList rechargeList =  rechargeListService.findByID(order.getCommdityID());
					if(rechargeList == null) {
						writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100005)));
						writer.close();
						return;
					}
					RechargeUserDetailed rechargeUserDetailed = new RechargeUserDetailed();
					rechargeUserDetailed.setFee(order.getPrice());
					rechargeUserDetailed.setUserID(order.getUserID());
					rechargeUserDetailed.setRemarks("充值");
					rechargeUserDetailed =  rechargeUserDetailedService.save(rechargeUserDetailed);
					if(rechargeUserDetailed == null) {
						writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100005)));
						writer.close();
						return;
					}
					
					writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(true)));
					writer.close();
					
					AdminConfig adminConfig = adminConfigService.findByOne();
					if(adminConfig != null && "1".equals(adminConfig.getImmediately())) {
						RedisOperationManager.del("mysql_recharge_qrcode_pay:"+"fee="+fee+"&type="+type);
					}
					return;
					
				}
			}
			
		}
		
		Order order = orderService.findByFee(fee, user.getId(),type);
		
		//添加明细
		changeDetailService.save(user.getId(), fee);
		
		//是否删除缓存
		Config config = configService.findById(user.getId());
		if(config != null && "1".equals(config.getImmediately())) {
			RedisOperationManager.del(redisKey+"fee="+fee+"&userID="+user.getId()+"&type="+type);
		}
		
		
		if(order == null || order.getPayState().equals("1")) {
			//无匹配订单
			order = new Order();
			order.setType(type);
			order.setUserID(user.getId());
			order.setPrice(fee);
			order.setPayState("1");
			order.setPayTime(DateUtil.getStringDateTime());
			order = orderService.save(order);
			if(order == null) {
				writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100005)));
				writer.close();
				return;
			}else {
				writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(true)));
				writer.close();
				return;
			}
		}else {
			//修改订单状态
			order.setPayState("1");
			order = orderService.save(order);
			if(order == null) {
				writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100005)));
				writer.close();
				return;
			}else {
				
				
				RechargeUserDetailed userDetailed = rechargeUserDetailedService.findByOutTradeNo(order.getOutTradeNo());
				
				if(userDetailed != null) {
					
					userDetailed.setRemarks("下单扣除手续费");
					userDetailed.setOutTradeNo(null);
					rechargeUserDetailedService.save(userDetailed);
				}else {

					//扣除手续费
					SetMealPurchase setMealPurchase = setMealPurchaseService.findByUserIDAndExpireDate(user.getId());
					if(setMealPurchase != null) {
						int procedures = setMealPurchase.getProcedures();
						if(procedures > 0) {
							int proceduresFee = 0;
							try {
								proceduresFee = new BigDecimal(Math.ceil(new BigDecimal(fee).multiply(new BigDecimal(procedures).divide(new BigDecimal(1000))).doubleValue())).intValue();
							} catch (Exception e) {
							}
							if(proceduresFee != 0) {
								RechargeUserDetailed rechargeUserDetailed = new RechargeUserDetailed();
								rechargeUserDetailed.setFee(-proceduresFee);
								rechargeUserDetailed.setUserID(order.getUserID());
								rechargeUserDetailed.setRemarks("下单扣除手续费");
								rechargeUserDetailed =  rechargeUserDetailedService.save(rechargeUserDetailed);
								
								order.setProcedures(procedures);
								order.setProceduresFee(proceduresFee);
								orderService.save(order);
							}
						}
						
					}
				}
				
				
				
				//查询剩余金额
				int sum = rechargeUserDetailedService.sum(order.getUserID());
				Config metConfig = configService.findById(order.getUserID());
				boolean state = true;
				if(metConfig != null && metConfig.getMinFee() != 0) {
					if(sum <= config.getMinFee()) {
						state = false;
					}
				}
				//返回
				writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(0,true,state)));
				writer.close();
				
				//开启线程
				OrderController orderController = new OrderController(order.getId(),orderService,userService);
				orderController.start();
				
				//gcny
				GcnyOrderChangeNotice gcnyOrderChangeNotice = new GcnyOrderChangeNotice(order.getOutTradeNo(), orderService);
				gcnyOrderChangeNotice.start();
			}
		}
		
	}
	
	public void run() {
		
		Order order = orderService.findById(orderID);
		
		if(order == null || order.getNotifyState().equals("1") || order.getNotifyUrl() == null) {
			return;
		}
		User user = userService.findById(order.getUserID());
		
		
		Map<String, String> res = new HashMap<>();
		res.put("outTradeNo", order.getOutTradeNo());
		res.put("price", String.valueOf(order.getPrice()));
		res.put("type", order.getType());
		res.put("uid", order.getUid());
		res.put("payState", "SUCCESS");
		res.put("payTime", order.getPayTime());
		res.put("nonceStr", SysUtil.generalPK());
		res.put("chain_add", order.getChainAdd());
		res.put("sign", AsciiOrder.sign(res, user.getToken()));
		
		for (int i = 0; i < 5;i++) {
			
			String req = HttpClientUtils.sendPost(order.getNotifyUrl(),com.alibaba.fastjson.JSONObject.toJSONString(res));
			
			if("SUCCESS".equals(req)) {
				order.setNotifyState("1");
				order.setNotifyTime(DateUtil.getStringDateTime());
				orderService.save(order);
				return;
			}else {
				order.setErrorMsg(req);
				orderService.save(order);
			}
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

	public String getOrderID() {
		return orderID;
	}

	public void setOrderID(String orderID) {
		this.orderID = orderID;
	}

	public OrderController(String orderID,OrderService orderService,UserService userService) {
		this.orderID = orderID;
		this.orderService = orderService;
		this.userService = userService;
	}

	public OrderController() {
		super();
	}
	
	
	@RequestMapping("/index/data")
	@ResponseBody
	public void indexData(HttpServletRequest request,HttpServletResponse response) throws IOException {
		
		
		PrintWriter writer =  response.getWriter();
		
		String jsonStr = RequestJson.returnJson(request);
		if(jsonStr == null || "".equals(jsonStr)) {
			writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100001)));
			writer.close();
			return;
		}
		Map<String, String> map = (Map<String, String>) JSONObject.toBean(JSONObject.fromObject(jsonStr),Map.class);
		
		if(map.get("userName") == null || map.get("password") == null) {
			writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100001)));
			writer.close();
			return;
		}
		
		String userName = map.get("userName");
		String password = map.get("password");
		
		
		User user = userService.findByName(userName);
		if(user == null) {
			writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100002)));
			writer.close();
			return;
		}
		
		if(!user.getPassword().equals(MD5.MD5Encode(password))) {
			writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(100003)));
			writer.close();
			return;
		}
		
		String userID = user.getId();
		
		
		Map<String, Object> res = new HashMap<>();
		
		SetMealPurchase setMealPurchase =  setMealPurchaseService.findByUserID(userID);
		if(setMealPurchase == null) {
			//无购买套餐
			res.put("expireDate", "当前暂未购买套餐");
		}else {
			res.put("expireDate", setMealPurchase.getExpireDate());
		}
		
		//剩余金额
		int sum = rechargeUserDetailedService.sum(userID);
		res.put("fee", sum);
		
		StatisticsDto statisticsDto = orderService.statistics(userID);
		res.put("uid", user.getUid());
		Map<String, Object> resMap = new HashMap<>();
		resMap.put("statistics", statisticsDto);
		resMap.put("account", res);
		
		writer.write(com.alibaba.fastjson.JSONObject.toJSONString(ReturnDto.send(resMap)));
		writer.close();
		return;
	}
	
	
	public static int calculation(SetMealPurchaseService setMealPurchaseService,String userID,int orderFee) {
		
		SetMealPurchase setMealPurchase = setMealPurchaseService.findByUserIDAndExpireDate(userID);
		if(setMealPurchase != null) {
			int procedures = setMealPurchase.getProcedures();
			if(procedures > 0) {
				int proceduresFee = 0;
				try {
					proceduresFee = new BigDecimal(Math.ceil(new BigDecimal(orderFee).multiply(new BigDecimal(procedures).divide(new BigDecimal(1000))).doubleValue())).intValue();
					return proceduresFee;
				} catch (Exception e) {
				}
			}
			
		}
		return 0;
	}
	
	public static Map<String, String> RandomUser(UserService userService,int fee,SetMealPurchaseService setMealPurchaseService,
			CommodityService commodityService,QrcodeService qrcodeService,String type) {
		
		List<UserDto> list = userService.findByOrderPay();
		
		return getRandomUser(list,commodityService,fee,qrcodeService,setMealPurchaseService,type);
	}
	
	public static Map<String, String> getRandomUser(List<UserDto> list,CommodityService commodityService,int fee,
			QrcodeService qrcodeService,SetMealPurchaseService setMealPurchaseService,String type) {
		
		if(list == null || list.size() == 0) {
			return null;
		}
		int size = list.size();
		//int row = (int)(Math.random()) * (size+1);
		int row =0;
		if(row > size) {
			return getRandomUser(list,commodityService,fee,qrcodeService,setMealPurchaseService,type);
		}else {
			UserDto user = list.get(row);
			if(user.getSumFee() == 0) {
				list.remove(row);
				return getRandomUser(list,commodityService,fee,qrcodeService,setMealPurchaseService,type);
			}
			if(setMealPurchaseService.findByUserIDAndExpireDate(user.getId()) == null) {
				list.remove(row);
				return getRandomUser(list,commodityService,fee,qrcodeService,setMealPurchaseService,type);
			}
			int proceduresFee = calculation(setMealPurchaseService, user.getId(), fee);
			int procedures = 0;
			SetMealPurchase setMealPurchase = setMealPurchaseService.findByUserIDAndExpireDate(user.getId());
			if(setMealPurchase != null) {
				procedures = setMealPurchase.getProcedures();
				if(procedures > 0) {
					try {
						proceduresFee = new BigDecimal(Math.ceil(new BigDecimal(fee).multiply(new BigDecimal(procedures).divide(new BigDecimal(1000))).doubleValue())).intValue();
					} catch (Exception e) {
						
					}
				}
			}
			if(proceduresFee > Integer.valueOf(user.getSumFee())) {
				list.remove(row);
				return getRandomUser(list,commodityService,fee,qrcodeService,setMealPurchaseService,type);
			}
			
			Commodity commodity =  commodityService.findByFee(fee, user.getId());
			if(commodity == null) {
				list.remove(row);
				return getRandomUser(list,commodityService,fee,qrcodeService,setMealPurchaseService,type);
			}
			
			List<Qrcode> qrcodes = qrcodeService.findByQrcode(fee, user.getId(), type, commodity.getId());
			if(qrcodes == null || qrcodes.size() == 0) {
				list.remove(row);
				return getRandomUser(list,commodityService,fee,qrcodeService,setMealPurchaseService,type);
			}
			Map<String, String> res = new HashMap<>();
			for (Qrcode  qrcode : qrcodes) {
				
				String qrcodeUrl = RedisOperationManager.getString(redisKey+"fee="+qrcode.getFee()+"&userID="+user.getId()+"&type="+qrcode.getType());
				
				if(qrcodeUrl != null) {
					continue;
				}
				if(qrcode.getFee() == 0) {
					res.put("errorCode", "100012");
					return res;
				}
				res.put("fee", String.valueOf(qrcode.getFee()));
				res.put("qrcodeUrl", qrcode.getUrl());
				res.put("userID", user.getId());
				res.put("commodityID", commodity.getId());
				res.put("procedures", String.valueOf(procedures));
				res.put("proceduresFee", String.valueOf(proceduresFee));
				
				RedisOperationManager.setString(redisKey+"fee="+qrcode.getFee()+"&userID="+user.getId()+"&type="+qrcode.getType(), qrcode.getUrl(), 360);
				
				return res;
			}
			
			list.remove(row);
			return getRandomUser(list,commodityService,fee,qrcodeService,setMealPurchaseService,type);
			
		}
	}
	@RequestMapping("/test")
	public void test() {
		List<UserDto> list = userService.findByOrderPay();
		System.out.println(list);
	}
}
