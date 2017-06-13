package questtest;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.time.StopWatch;

import java.io.IOException;

/**
 * Created by ${kang} on 2016/6/20.
 */

public class Json {
    public static String ask(String query) throws HttpException {
        JSONObject json = new JSONObject();
        json.put("protocolId", 5);
        json.put("robotHashCode", "ndnsh");
        json.put("platformConnType", "1");
        json.put("userId", "10031");
        json.put("talkerId", "10021");
        json.put("receiverId", "20021");
        json.put("appKey", "ac5d5452");
        json.put("sendTime", System.currentTimeMillis());
        json.put("type", "text");
        json.put("query", query);
        json.put("msgID", "asdfg123");
        json.put("isQuestionQuery", 0);
        System.out.println(json);
        String url = "http://yun.njuelectronics.com:8088/CSRBroker/queryAction";

        String retStr = doPostQuery(url, json.toString());
        System.out.println("005");
        System.out.println(retStr);
        return retStr;
    }

    public static String doPostQuery(String url, String query) throws HttpException {
        String result = null;
        HttpClient client = new HttpClient();           //创建HTTP客户端实例
        PostMethod method = new PostMethod(url);        //
        method.setRequestHeader("Connection", "close");
        method.setRequestHeader("Content-type", "application/json;");
        client.getHttpConnectionManager().getParams().setConnectionTimeout(3000000);
        method.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, 3000000);
        try {
            RequestEntity requestEntity = new ByteArrayRequestEntity(query.getBytes("UTF-8"),"UTF-8");
            method.setRequestEntity(requestEntity);
        } catch (Exception e) {
        }

        //发出请求
        int stateCode = 0;
        //System.out.println("001");
        System.out.println(stateCode);
        StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            //System.out.println("0055");

            stateCode = client.executeMethod(method);
            //System.out.println("002");
            System.out.println(stateCode);

        } catch (HttpException e) {
            System.out.println("0011");
        } catch (IOException e) {
            System.out.println("0022");
            System.out.println(e.toString());
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("0033");
            System.out.println(e.toString());
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            stopWatch.stop();
            if(stateCode== HttpStatus.SC_OK) {
                try {
                    result = method.getResponseBodyAsString();
                    System.out.println("003");
                } catch (IOException e) {
                }
            }
            System.out.println("0044");
            method.abort();
            method.releaseConnection();
            ((SimpleHttpConnectionManager)client.getHttpConnectionManager()).shutdown();
        }
        System.out.println("004");
        return result;
    }
}
