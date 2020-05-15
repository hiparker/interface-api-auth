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
