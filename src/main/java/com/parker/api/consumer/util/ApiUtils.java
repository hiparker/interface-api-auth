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
