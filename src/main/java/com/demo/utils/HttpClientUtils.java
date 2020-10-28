package com.demo.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class HttpClientUtils {

    public static HttpPost getHttpPost(String url) {
        HttpPost httpPost = new HttpPost(url);
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:69.0) Gecko/20100101 Firefox/69.0";
        httpPost.setHeader(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2"));
        httpPost.setHeader(new BasicHeader("User-Agent", userAgent));
        return httpPost;
    }

    public static HttpGet getHttpGET(String url) {
        HttpGet httpGet = new HttpGet(url);
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:69.0) Gecko/20100101 Firefox/69.0";
        httpGet.setHeader(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2"));
        httpGet.setHeader(new BasicHeader("User-Agent", userAgent));
        return httpGet;
    }




    public static String getContent(HttpPost httpPost) throws IOException {
        String context = "";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            context = EntityUtils.toString(entity, "utf-8");
        }
        EntityUtils.consume(entity);
        httpClient.close();
        return context;
    }
}
