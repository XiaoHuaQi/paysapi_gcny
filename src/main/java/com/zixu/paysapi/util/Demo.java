package com.zixu.paysapi.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class Demo {
	
	public static void main(String[] args) {
		
		/*String token = "e10adc3949ba59abbe56e057f20f883e";
		
		Map<String, String> res = new HashMap<>();
		
		res.put("chain_add", "d2e985211a7a3455d530bfcf25c4e0c9");
		res.put("commdityName", "1000元包");
		res.put("outTradeNo", "20181101105800");
		res.put("price", "100000");
		res.put("type", "alipay");
		res.put("notifyUrl", "aa");
		res.put("nonceStr", "71fb24ab160e18707727be2f9c20591f");
		res.put("sign", sign(res, token));
		
		System.out.println(res);*/
		
		for (int i = 0; i < 100; i++) {
			System.out.println((int)(Math.random() * 4));
		}
		
		//String reString = sendPost("http://119.29.27.13/order/api/pay", JSONObject.toJSONString(res), null);
		//System.out.println(reString);
	}
	
	public static String sign(Map<String,String> params,String weixinkey){
        //按参数名asscic码排序
        List<String> names = new ArrayList<>();
        names.addAll(params.keySet());
        java.util.Collections.sort(names);
        String strSign = "";
        for(String key:names){
        	 strSign+=key+"="+params.get(key)+"&";
        }
        String secret = "key="+weixinkey;
        strSign+=secret;
        return Md5Utils.md5(strSign).toLowerCase();
    }
	
	 public static String sendPost(String url, String param, String ContentType) {
	        String result = null;
	        OkHttpClient httpClient = new OkHttpClient();
	        String ContentTypea = "text/html;charset=utf-8";
	        if(ContentType != null){
	            ContentTypea = ContentType;
	        }
	        RequestBody requestBody = RequestBody.create(MediaType.parse(ContentTypea), param);
	        Request request = new Request.Builder().url(url).post(requestBody).build();
	        try {
	            Response response = httpClient.newCall(request).execute();
	            result = response.body().string();
	        } catch (IOException e) {
	            return null;
	        }
	        return result;
	    }
	
}
