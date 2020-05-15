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
