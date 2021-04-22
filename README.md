> 很多码友在处理Java后端接口API上，对于安全认证却是一种很头疼的事

### 博客地址 

> https://www.bedebug.com/archives/interfaceapi

### 为什么要授权认证
1.防止未授权的用户，非法获得不该他所能看到的数据
2.数据的安全性，防止被同行或者有心人士，通过接口爬取重要数据
3.防止接口大批量灌水，如果提前设置好Token失效时间，即使拿到了认证密文也只是短时间内起效（况且密文能不能解析还是一回事）

### 逻辑思维导图
![Java后端接口API认证授权](https://www.bedebug.com/upload/2020/05/Java后端接口API认证授权-b8959a14ff3e4250899556de412a1b80.jpg)

### 接口认证效果
![接口1](https://www.bedebug.com/upload/2020/05/接口1-bc4f23122de8497bb09b6ad30c70ad58.jpg)

![接口2](https://www.bedebug.com/upload/2020/05/接口2-43f0e0d629d54491a6bf76e3a64f01c0.jpg)

++如果后端 通过认证文件调用API接口，则每次都会去取Token，即使Token失效也会重新生成++

### 核心代码解析

#### API提供服务端 - HTTP协议 - 其他语言也可以调用

**工具类**
```
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

   

}

```

**生产者**
```
package com.parker.api.producer.service;

import com.alibaba.fastjson.JSONObject;
import com.parker.api.common.InterfaceUtil;
import com.parker.api.common.result.Result;
import com.parker.api.producer.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 生产者 Service
 *
 * @author Parker
 */
@Slf4j
@Service
public class InterfaceService {

    @Autowired
    private SystemService systemService;

    /**
     * 生成 CNF
     * @param userLoginName
     * @param userPassword
     * @return
     */
    public Result createCNF(String userLoginName, String userPassword){
        Result result = new Result();
        if(StringUtils.isEmpty(userLoginName) || StringUtils.isEmpty(userPassword)){
            result.setMsg("用户登录名或密码不可为空！");
            result.setSuccess(false);
            return result;
        }

        String cnf = InterfaceUtil.encryptCnfString(userLoginName, userPassword);
        if(StringUtils.isNotEmpty(cnf)){
            result.put("cnf",cnf);
        }else{
            result.setSuccess(false);
            result.setMsg("cnf文件生成失败！");
        }
        return result;
    }

    /**
     * 获得 Token
     * @param cnf
     * @return
     */
    public Result getToken(String cnf){
        Result result = new Result();
        if(StringUtils.isEmpty(cnf)){
            result.setMsg("cnf 不可为空！");
            result.setSuccess(false);
            return result;
        }

        String dcnfStr = "";
        boolean dcnfFlag = false;
        try {
            // 解密token
            dcnfStr = InterfaceUtil.decryptCnfString(cnf);
            dcnfFlag = true;
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
        if(!dcnfFlag){
            return  Result.error("解析失败");
        }
        JSONObject jsonObject = JSONObject.parseObject(dcnfStr);
        String userLoginName = (String) jsonObject.get(InterfaceUtil.INTERFACE_USER_LOGIN_NAME);
        String userPassword = (String) jsonObject.get(InterfaceUtil.INTERFACE_USER_PASSWORD);
        // 获得用户
        User currUser = systemService.getByLoginName(userLoginName);
        if(currUser != null){
            if(StringUtils.isNotEmpty(userPassword) && StringUtils.isNotEmpty(currUser.getPassword())){
                // 密码匹配
                boolean flag = SystemService.validatePassword(userPassword,currUser.getPassword());
                if(flag){
                    return InterfaceUtil.getToken(cnf);
                }
            }
        }

        result.setMsg("Token 获取失败！");
        result.setSuccess(false);
        return result;
    }


    /**
     * 验证 Token
     * @param token
     * @return
     */
    public Result validateToken(String token){
        Result result = new Result();
        if(StringUtils.isEmpty(token)){
            result.setMsg("token 不可为空！");
            result.setSuccess(false);
            return result;
        }

        boolean flag = InterfaceUtil.validateToken(token);

        if(flag){
            result.setMsg("Token 生效中！");
        }else{
            result.setSuccess(false);
            result.setMsg("Token 已失效！");
        }
        return result;
    }

}

```

**验证用户**
```
package com.parker.api.producer.service;

import com.google.common.collect.Maps;
import com.parker.api.common.util.Digests;
import com.parker.api.common.util.Encodes;
import com.parker.api.common.util.RedisUtil;
import com.parker.api.producer.entity.User;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created Date by 2020/5/15 0015.
 *
 * 模拟 从缓存里查找用户 如果找不到 就去数据库找
 * @author Parker
 */
@Service
public class SystemService {

    public static final String HASH_ALGORITHM = "SHA-1";
    public static final int HASH_INTERATIONS = 1024;
    public static final int SALT_SIZE = 8;
    /** Redis前缀 */
    private static final String REDIS_PREFIX = "LOGIN_USER_";
    /** token 过期时间（秒） 6000s = 1h */
    private static final int TOKEN_EXPIRE_TIME = 6000;

    /** 模拟数据库 用户当前数据 */
    private static final Map<String, User> userMap = Maps.newHashMap();

    /**
     * 测试数据
     */
    static{
        User currUser1 = new User()
                .setUserName("周一")
                .setUserLoginName("parker")
                .setPassword(entryptPassword("123456"));
        User currUser2 = new User()
                .setUserName("崔二")
                .setUserLoginName("test")
                .setPassword(entryptPassword("123456"));
        User currUser3 = new User()
                .setUserName("张三")
                .setUserLoginName("tes1")
                .setPassword(entryptPassword("884848"));
        User currUser4 = new User()
                .setUserName("李四")
                .setUserLoginName("tes2")
                .setPassword(entryptPassword("ashdu12"));
        User currUser5 = new User()
                .setUserName("王五")
                .setUserLoginName("tes3")
                .setPassword(entryptPassword("gdf232"));
        User currUser6 = new User()
                .setUserName("赵六")
                .setUserLoginName("tes4")
                .setPassword(entryptPassword("4v782"));

        userMap.put(currUser1.getUserLoginName(),currUser1);
        userMap.put(currUser2.getUserLoginName(),currUser2);
        userMap.put(currUser3.getUserLoginName(),currUser3);
        userMap.put(currUser4.getUserLoginName(),currUser4);
        userMap.put(currUser5.getUserLoginName(),currUser5);
        userMap.put(currUser6.getUserLoginName(),currUser6);

    }


    /**
     * 根据登录名获取用户
     *
     * 模拟数据库查询，查到就加入缓存 防止恶意刷接口 导致 穿透效应
     *
     * @param loginName 登录名
     * @return 取不到返回 null
     */
    public User getByLoginName(String loginName) {

        // 非法判断
        if (StringUtils.isEmpty(loginName)) {
            return null;
        }

        User user = null;
        user = (User) RedisUtil.hget(REDIS_PREFIX + loginName,loginName);
        if (null == user){
            user = userMap.get(loginName);
            if (null == user){
                return null;
            }
            // 设置随机过期时间 防止雪崩
            int randomNum = RandomUtils.nextInt(600, 1200);
            RedisUtil.hset(REDIS_PREFIX + loginName,loginName,user,randomNum);
        }

        return user;
    }


    /**
     * 生成安全的密码，生成随机的16位salt并经过1024次 sha-1 hash
     */
    public static String entryptPassword(String plainPassword) {
        byte[] salt = Digests.generateSalt(SALT_SIZE);
        byte[] hashPassword = Digests.sha1(plainPassword.getBytes(), salt, HASH_INTERATIONS);
        return Encodes.encodeHex(salt)+ Encodes.encodeHex(hashPassword);
    }

    /**
     * 验证密码
     * @param plainPassword 明文密码
     * @param password 密文密码
     * @return 验证成功返回true
     */
    public static boolean validatePassword(String plainPassword, String password) {
        byte[] salt = Encodes.decodeHex(password.substring(0,16));
        byte[] hashPassword = Digests.sha1(plainPassword.getBytes(), salt, HASH_INTERATIONS);
        return password.equals(Encodes.encodeHex(salt)+ Encodes.encodeHex(hashPassword));
    }

}

```

#### 第三方接口调用端

**工具类 - 后端服务发起接口调用**
```
package com.parker.api.consumer.util;

import lombok.extern.slf4j.Slf4j;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.parker.api.common.result.JsonMapper;
import com.parker.api.common.result.Result;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

/**
 * Created Date by 2020/5/15 0015.
 *
 * @author Parker
 */
@Slf4j
public final class ApiUtils {

    private static final String URL = "http://127.0.0.1:8000";

    public static final Map<String,String> map = Maps.newHashMap();
    static {
        //凭证保存
        map.put("getToken","/interface/do/getToken");
    }

    /**
     * 私有化构造函数
     */
    private ApiUtils(){}

    /**
     * 获得token
     * @param uri
     * @param cnf
     * @return
     */
    public static Result getToken(String uri,String cnf){
        Result result = null;
        try {
            Map<String,Object> argsMap = Maps.newHashMap();
            argsMap.put("cnf",cnf);
            String resultStr = sendPost(uri, argsMap);
            // 获得结果
            result = (Result) JsonMapper.fromJsonString(resultStr, Result.class);
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }

        if(null == result){
            return Result.error("接口请求失败");
        }
        return result;
    }


    /**
     * 发起 接口API 请求
     * @param uri
     * @param argsMap
     * @return
     */
    public static Result httpRequest(String uri,Map<String,Object> argsMap){
        Result result = null;
        try {
            String resultStr = sendPost(uri, argsMap);
            // 获得结果
            result = (Result) JsonMapper.fromJsonString(resultStr, Result.class);
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }

        if(null == result){
            return Result.error("接口请求失败");
        }
        return result;
    }

    /** Common */
    /** ------------------------------------------------------------------------------------------------------------ **/

    /**
     * 发起 post 请求
     * @param url
     * @param argsMap
     * @return
     */
    public static String sendPost(String url, Map<String,Object> argsMap) {
        DataOutputStream out = null;
        BufferedReader reader = null;
        HttpURLConnection conn = null;
        StringBuffer result = new StringBuffer();
        try {
            URL realUrl = new URL(URL.concat(url));

            // 打开和URL之间的连接
            conn = (HttpURLConnection) realUrl.openConnection();
            if (!conn.getDoOutput()) {
                conn.setDoOutput(true);
            }
            // 设置为POST请求方式
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Content-Type", "application/json");

            // 获取URLConnection对象对应的输出流
            out = new DataOutputStream(
                    conn.getOutputStream());

            // 处理请求参数
            UUID uuid = UUID.randomUUID();
            int hashCode = uuid.toString().hashCode();
            argsMap.put("format", 1);
            argsMap.put("useragent", "ApiClient");
            argsMap.put("rid", hashCode);
            argsMap.put("timestamp",  System.currentTimeMillis());
            argsMap.put("v", "1.0");

            JSONObject jObj = new JSONObject();
            for (String key : argsMap.keySet()) {
                jObj.put(key,argsMap.get(key).toString());
            }

            // 发送请求参数
            out.writeBytes(chinaToUnicode(jObj.toString()));
            // flush输出流的缓冲
            out.flush();

            // 定义BufferedReader输入流来读取URL的响应
            reader = new BufferedReader(new InputStreamReader(
                    conn.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                String sResult = new String(line.getBytes(), "utf-8");
                result.append(sResult);
            }

        } catch (Exception e) {
            log.error("发送 POST 请求出现异常！ Message:{}",e.getMessage(),e);
        }
        //使用finally块来关闭输出流、输入流、链接
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(reader!=null){
                    reader.close();
                }
                if(conn != null){
                    conn.disconnect();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result.toString();
    }

    /**
     * 把中文转成Unicode码
     *
     * @param str
     * @return
     */
    public static String chinaToUnicode(String str) {
        String result = "";
        for (int i = 0; i < str.length(); i++) {
            int chr1 = (char) str.charAt(i);
            if (chr1 >= 19968 && chr1 <= 171941) {// 汉字范围 \u4e00-\u9fa5 (中文)
                result += "\\u" + Integer.toHexString(chr1);
            } else {
                result += str.charAt(i);
            }
        }
        return result;
    }
}


```

**解析cnf文件 调用接口**
```
     /**
     * 实例
     *
     * 模拟 真实 第三方服务调用
     * 将cnf 文件下发给 第三方服务 ， 第三方服务通过后端服务器调用接口 所以不用担心 cnf 被丢失暴力破解
     *
     * 即使丢失 只要修改 cnf授权 用户密码 即可将原有的cnf文件失效
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "example")
    public Result example(){
        Result result = null;
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        String  RESOURCES_PATHS = "/tokenFile/";
        BufferedInputStream bis = null;
        try {
            int fileSize = 0;
            try {
                Resource resource = resourceLoader.getResource(RESOURCES_PATHS+"test.cnf");
                fileSize =(int) resource.getFile().length();
                bis = new BufferedInputStream(resource.getInputStream());
            } catch (IOException ex) {
                // TODO
                log.error(ex.getMessage(),ex);
            }

            byte[] buf = new byte[fileSize];
            int length = 0;
            String str = null;
            if((length = bis.read(buf))!=-1){
                str=new String(buf,"utf-8");
            }


            // 获得Token
            Result getToken = ApiUtils.getToken(ApiUtils.map.get("getToken"), str);
            if(getToken.isSuccess()){
                // 请求 API 接口
                Map<String,String> argsMap = Maps.newHashMap();
                argsMap.put("token",(String) getToken.get("token"));
                argsMap.put("code","1010110");
                result = ApiUtils.httpRequest("/interface/do/test", argsMap);
            }else{
                result = getToken;
            }

        }catch (Exception e){
            // TODO
        }finally {
            IOUtils.closeQuietly(bis);
        }

        if(result == null){
            return Result.error("请求失败！");
        }
        return result;
    } 
```

### 结语

**开源敲码不易，如果感觉对你有帮助可以点击下方支持，请作者喝一杯咖啡！**
