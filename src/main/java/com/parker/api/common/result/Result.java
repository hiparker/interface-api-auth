/**
 * Copyright &copy; 2015-2020 <a href="http://www.jeeplus.org/">JeePlus</a> All rights reserved.
 */
package com.parker.api.common.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * 统一返回参数
 *
 * @date 2020年5月15日10:40:54
 * @author Parker
 * 
 */
public class Result extends HashMap<String,Object> implements Serializable {


	public Result(){
		this.put("success", true);
		this.put("code", HttpStatus.OK.value());
		this.put("msg", "操作成功");
	}

	public String getMsg() {
		return (String)this.get("msg");
	}

	public void setMsg(String msg) {//向json中添加属性，在js中访问，请调用data.msg
		this.put("msg", msg);
	}


	public boolean isSuccess() {
		return (boolean)this.get("success");
	}

	public void setSuccess(boolean success) {
		this.put("success", success);
	}
	
	@JsonIgnore//返回对象时忽略此属性
	public String getJsonStr() {//返回json字符串数组，将访问msg和key的方式统一化，都使用data.key的方式直接访问。
		String json = JsonMapper.getInstance().toJson(this);
		return json;
	}
	@JsonIgnore//返回对象时忽略此属性
	public static Result success(String msg) {
		Result j = new Result();
		j.setMsg(msg);
		return j;
	}
	@JsonIgnore//返回对象时忽略此属性
	public static Result error(String msg) {
		Result j = new Result();
		j.setSuccess(false);
		j.setMsg(msg);
		return j;
	}

	public static Result success(Map<String, Object> map) {
		Result restResponse = new Result();
		restResponse.putAll(map);
		return restResponse;
	}

	public static Result success() {
		return new Result();
	}


	@Override
	public Result put(String key, Object value) {
		super.put(key, value);
		return this;
	}

	public Result putMap(Map m) {
		super.putAll(m);
		return this;
	}

	public int getCode() {
		return (int)this.get("code");
	}

	public void setCode(int code) {
		this.put("code", code);
	}

}
