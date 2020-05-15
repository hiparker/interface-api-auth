package com.parker.api.consumer.controller;


import com.google.common.collect.Maps;
import com.parker.api.common.result.Result;
import com.parker.api.consumer.util.ApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.BufferedInputStream;
import java.io.IOException;
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
@RequestMapping(value = "/consumer/")
public class ConsumerController {


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
}
