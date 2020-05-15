/**
 * Copyright &copy; Edwin All rights reserved.
 */
package com.parker.api.common.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 封装各种生成唯一性ID算法的工具类.
 * @author Edwin
 * @version 2016-01-15
 */
public final class IdGen{

	private static SecureRandom random = new SecureRandom();

	/** 私有化构造函数 */
	private IdGen(){}

	/**
	 * 封装JDK自带的UUID, 通过Random数字生成, 中间无-分割.
	 */
	public static String uuid() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}
	
	/**
	 * 使用SecureRandom随机生成Long. 
	 */
	public static long randomLong() {
		return Math.abs(random.nextLong());
	}

	/**
	 * 基于Base62编码的SecureRandom随机生成bytes.
	 */
	public static String randomBase62(int length) {
		byte[] randomBytes = new byte[length];
		random.nextBytes(randomBytes);
		return Encodes.encodeBase62(randomBytes);
	}
	

	
	public static void main(String[] args) {
		System.out.println(IdGen.uuid());
		System.out.println(IdGen.uuid().length());
		for (int i=0; i<1000; i++){
			System.out.println(IdGen.randomLong() + "  " + IdGen.randomBase62(5));
		}
	}

}
