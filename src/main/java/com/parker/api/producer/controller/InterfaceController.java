package com.parker.api.producer.controller;


import com.parker.api.common.InterfaceUtil;
import com.parker.api.common.result.Result;
import com.parker.api.common.util.DateUtils;
import com.parker.api.producer.service.InterfaceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * 接口Controller
 *
 * @author Parker
 * @date 2020年5月15日11:30:15
 *
 */
@Slf4j
@Controller
@RequestMapping(value = "/interface/do")
public class InterfaceController {

    @Autowired
    private InterfaceService interfaceService;

    /**
     * 生成 CNF
     * @param  userLoginName
     * @param  userPassword
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "createCNF")
    public Result createCNF(@RequestParam("userLoginName")String userLoginName,@RequestParam("userPassword")String userPassword,  HttpServletResponse response){
        Result result = interfaceService.createCNF(userLoginName,userPassword);
        String cnf =(String) result.get("cnf");
        String fileName = "授权文件"+ DateUtils.getDate("yyyyMMddHHmmss")+".cnf";
        try {
            InterfaceUtil.write(cnf,fileName,response);
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
        return result;
    }

    /**
     * 生成 CNF 数据
     * @param args
     *   userLoginName
     *   userPassword
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "createCNFByDate")
    public Result createCNFByDate(@RequestBody Map<String,String> args, HttpServletResponse response){
        String userLoginName = "";
        String userPassword = "";
        if(args != null){
            userLoginName = args.get("userLoginName");
            userPassword = args.get("userPassword");
        }

        Result result = interfaceService.createCNF(userLoginName,userPassword);
        String cnf =(String) result.get("cnf");
        result.put("cnf",cnf);
        return result;
    }

    /**
     * 获得 Token
     * @param args
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "getToken")
    public Result getToken(@RequestBody Map<String,String> args){
        String cnf = "";
        if(args != null){
            cnf = args.get("cnf");
        }
        return interfaceService.getToken(cnf);
    }

    /**
     * 验证 Token
     * @param args
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "validateToken")
    public Result validateToken(@RequestBody Map<String,String> args){
        String token = "";
        if(args != null){
            token = args.get("token");
        }
        return interfaceService.validateToken(token);
    }

    /**
     * 测试接口
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "test")
    public Result test(@RequestBody Map<String,String> args){
        String token = "";
        String code = "";
        if(args != null){
            token = args.get("token");
            code = args.get("code");
        }

        Result result = interfaceService.validateToken(token);
        if(!result.isSuccess()){
            return result;
        }

        Result res = new Result();
        res.put("code",code);
        res.put("data",System.currentTimeMillis());
        res.setMsg("远程接口API返回");
        return res;
    }
}
