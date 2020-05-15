package com.parker.api.common;

import com.alibaba.fastjson.JSONObject;
import com.parker.api.common.result.Result;
import com.parker.api.common.util.IdGen;
import com.parker.api.common.util.RedisUtil;
import com.parker.api.common.util.misc.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created Date by 2020/5/14 0014.
 *
 * 公用接口工具类
 * @author Parker
 */
@Slf4j
public final class InterfaceUtil {

    /** Redis前缀 */
    private static final String REDIS_PREFIX = "TOKEN_";

    /** 文本加密KEY */
    private static final String ENCRYPT_KEY = "1010110";
    /** 密码加密KEY */
    private static final String PASSWORD_KEY = "arcinbj";

    /** token 过期时间（秒） 600s = 10m */
    private static final int TOKEN_EXPIRE_TIME = 600;

    /** 参数 */
    private static final String INTERFACE_UUID = "uuid";
    public static final String INTERFACE_USER_LOGIN_NAME = "login_name";
    public static final String INTERFACE_USER_PASSWORD = "password";

    /**
     * 私有化 构造函数
     * 防止序列化 工具类
     */
    private InterfaceUtil(){}


    /**
     * 根据 cnf 密文 ，获得Token 密文
     * 注：
     *      Token 生命时长为 10分钟
     *      10分钟后 生成新的Token
     *      每个Token都不一样 防止接口被爬取 或者 盗链疯狂灌水
     *
     * @param cnfStr
     * @return
     */
    public static Result getToken(String cnfStr){
        Result result = new Result();
        String dcnfStr = "";
        boolean dcnfFlag = false;
        try {
            dcnfStr = InterfaceUtil.decryptCnfString(cnfStr);
            dcnfFlag = true;
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
        if(!dcnfFlag){
            return  Result.error("解析失败");
        }

        JSONObject jsonObject = JSONObject.parseObject(dcnfStr);

        // ----------
        // 用户登录名
        String userLoginName = (String) jsonObject.get(INTERFACE_USER_LOGIN_NAME);
        // 验证码 - UUID
        String UUID = IdGen.uuid();

        // ----------
        // 授权Token 密文
        String encryptStr = null;
        Object objToken = RedisUtil.get(REDIS_PREFIX + userLoginName);
        // 如果Token 过期 ，则重新生成一个Token放入到Redis
        if(objToken == null){
            JSONObject beforeEncryptJson = new JSONObject(true);
            beforeEncryptJson.put(INTERFACE_USER_LOGIN_NAME,userLoginName);
            beforeEncryptJson.put(INTERFACE_UUID,UUID);

            boolean encryptStrFlag = false;
            try {
                // 加密 json
                encryptStr = encryptTokenString(beforeEncryptJson);
                encryptStrFlag = true;
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
            if(!encryptStrFlag){
                return  Result.error("加密失败");
            }

            // 将生成好的 授权Token 加入到Redis 设置为10分钟过期
            RedisUtil.set(REDIS_PREFIX + userLoginName,encryptStr,TOKEN_EXPIRE_TIME);
        }else{
            encryptStr = (String) objToken;
        }

        // ----------
        // 判断 如果 encryptStr 密文token为空 则报错
        if(StringUtils.isEmpty(encryptStr)){
            result.setSuccess(false);
            result.setMsg("Token 获取失败！");
        }else{
            result.put("token",encryptStr);
        }
        return result;
    }


    /**
     * 加密CNF String
     * @param userLoginName 用户登录名
     * @param userPassword  用户密码
     * @return String
     */
    public static String encryptCnfString(String userLoginName,String userPassword){
        JSONObject beforeEncryptJson = new JSONObject(true);
        beforeEncryptJson.put(INTERFACE_USER_LOGIN_NAME,userLoginName);
        // 对于密码再次加密
        beforeEncryptJson.put(INTERFACE_USER_PASSWORD, AESUtil.encrypt(userPassword, PASSWORD_KEY));
        // 为了安全性 加密2次
        // 加密 json
        String encryptJsonStr = AESUtil.encrypt(beforeEncryptJson.toJSONString(), ENCRYPT_KEY);
        return AESUtil.encrypt(encryptJsonStr, ENCRYPT_KEY);
    }

    /**
     * 解密CNF String
     * @param cnfStr cnf 文本
     * @return String
     */
    public static String decryptCnfString(String cnfStr) throws Exception{
        // 第一次解密
        String encryptJsonStr = AESUtil.decrypt(cnfStr, ENCRYPT_KEY);
        // 第二次解密
        String encryptStr = AESUtil.decrypt(encryptJsonStr, ENCRYPT_KEY);

        JSONObject jsonObject = JSONObject.parseObject(encryptStr);
        String userPassword = (String) jsonObject.get(InterfaceUtil.INTERFACE_USER_PASSWORD);
        // 解密 密码
        jsonObject.put(INTERFACE_USER_PASSWORD,AESUtil.decrypt(userPassword, PASSWORD_KEY));
        return jsonObject.toJSONString();
    }

    /**
     * 加密Token String
     * @param tokenJson Token
     * @return String
     */
    private static String encryptTokenString(JSONObject tokenJson) throws Exception{
        // 为了安全性 加密3次
        // 加密 json
        String encryptJsonStr = AESUtil.encrypt(tokenJson.toJSONString(), ENCRYPT_KEY);
        String encryptJsonStr2 = AESUtil.encrypt(encryptJsonStr, ENCRYPT_KEY);
        return AESUtil.encrypt(encryptJsonStr2, ENCRYPT_KEY);
    }

    /**
     * 解密Token String
     * @param tokenStr Token 文本
     * @return String
     */
    private static String decryptTokenString(String tokenStr) throws Exception{
        // 第1次解密
        String encryptJsonStr = AESUtil.decrypt(tokenStr, ENCRYPT_KEY);
        // 第2次解密
        String encryptJsonStr2 = AESUtil.decrypt(encryptJsonStr, ENCRYPT_KEY);
        // 第3次解密
        return AESUtil.decrypt(encryptJsonStr2, ENCRYPT_KEY);
    }

    /**
     * 验证 Token 有效性
     * @param tokenStr
     * @return
     */
    public static boolean validateToken(String tokenStr){
        String dTokenStr  = "";
        boolean dTokenFlag = false;
        try {
            // 解密token
            dTokenStr = InterfaceUtil.decryptTokenString(tokenStr);
            dTokenFlag = true;
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
        if(!dTokenFlag){
            return false;
        }
        if(dTokenStr != null){
            JSONObject jsonObject = JSONObject.parseObject(dTokenStr);
            if(jsonObject != null){
                // 用户登录名
                String userLoginName = (String) jsonObject.get(INTERFACE_USER_LOGIN_NAME);
                Object objToken = RedisUtil.get(REDIS_PREFIX + userLoginName);
                if(objToken != null){
                    if(tokenStr.equals((String) objToken)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 输出到客户端
     * @param fileName 输出文件名
     */
    public static void write(String cnf, String fileName, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Disposition", "attachment;filename="
                + new String((fileName).getBytes(), "iso-8859-1"));
        try {
            OutputStreamWriter write = new OutputStreamWriter(response.getOutputStream(), "utf-8");
            BufferedWriter writer = new BufferedWriter(write);
            StringBuilder cfnStringBuilder = new StringBuilder();
            cfnStringBuilder.append(cnf);
            writer.write(cfnStringBuilder.toString());
            writer.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /** ----------------------------------------------------------------- **/

    /**
     * 测试 加解密
     * @param args
     */
    public static void main(String[] args) throws Exception{

        String ecnfStr = InterfaceUtil.encryptCnfString("test","000000ww");
        String dcnfStr = InterfaceUtil.decryptCnfString(ecnfStr);
        // 加密
        System.out.println(ecnfStr);
        // 解密
        System.out.println(dcnfStr);
    }

}
