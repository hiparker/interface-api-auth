package com.parker.api.consumer.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.parker.api.common.result.JsonMapper;
import com.parker.api.common.result.Result;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
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
            Map<String,String> argsMap = Maps.newHashMap();
            argsMap.put("cnf",cnf);
            HttpURLConnection connection = initUrlConn(uri, argsMap);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            /*System.out.println(" ============================= ");
            System.out.println(" Contents of post request ");
            System.out.println(" ============================= ");*/
            StringBuffer stringBuffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                String sResult = new String(line.getBytes(), "utf-8");
                stringBuffer.append(sResult);
                //System.out.println(sResult);
            }

            // 获得结果
            result = (Result) JsonMapper.fromJsonString(stringBuffer.toString(), Result.class);

            /*System.out.println(" ============================= ");
            System.out.println(" Contents of post request ends ");
            System.out.println(" ============================= ");*/
            reader.close();
            connection.disconnect();

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
    public static Result httpRequest(String uri,Map<String,String> argsMap){
        Result result = null;
        try {
            HttpURLConnection connection = initUrlConn(uri, argsMap);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            /*System.out.println(" ============================= ");
            System.out.println(" Contents of post request ");
            System.out.println(" ============================= ");*/
            StringBuffer stringBuffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                String sResult = new String(line.getBytes(), "utf-8");
                stringBuffer.append(sResult);
                //System.out.println(sResult);
            }

            // 获得结果
            result = (Result) JsonMapper.fromJsonString(stringBuffer.toString(), Result.class);

            /*System.out.println(" ============================= ");
            System.out.println(" Contents of post request ends ");
            System.out.println(" ============================= ");*/
            reader.close();
            connection.disconnect();

        }catch (Exception e){
            log.error(e.getMessage(),e);
        }

        if(null == result){
            return Result.error("接口请求失败");
        }
        return result;
    }

    // HttpURLConnection
    public static HttpURLConnection initUrlConn(String uri,Map<String,String> argsMap)
            throws Exception {
        URL postUrl = new URL(URL.concat(uri));
        HttpURLConnection connection = (HttpURLConnection) postUrl
                .openConnection();
        if (!connection.getDoOutput()) {
            connection.setDoOutput(true);
        }
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Content-Type", "application/json");
        DataOutputStream out = new DataOutputStream(
                connection.getOutputStream());

        UUID uuid = UUID.randomUUID();
        int hashCode = uuid.toString().hashCode();


        JSONObject jObj = new JSONObject();
        jObj.put("format", 1);
        jObj.put("useragent", "ApiClient");
        jObj.put("rid", hashCode);
        jObj.put("timestamp",  System.currentTimeMillis());
        jObj.put("v", "1.0");
        for (String key : argsMap.keySet()) {
            jObj.put(key,chinaToUnicode(argsMap.get(key)));
        }

        out.writeBytes(jObj.toString());
        out.flush();
        out.close();

        return connection;
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
