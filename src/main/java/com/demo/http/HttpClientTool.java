package com.demo.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.demo.utils.MyTools;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class HttpClientTool {

    private static final Logger log = LoggerFactory.getLogger(HttpClientTool.class);

    private final Header header = new BasicHeader("Content-type", "application/json");

    private final String URL_PATTERN = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    private String encode = "utf-8";

    private Map<String, String> headers = new HashMap<>();

    private Map<String, String> params = new HashMap<>();

    private String url;

    private String bodyJson;

    private String response;

    private Boolean pool = false;

    private HttpClientTool() {
    }

    public static HttpClientTool builders() {
        return new HttpClientTool();
    }


    public HttpClientTool header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public HttpClientTool headers(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public HttpClientTool params(Map<String, String> params) {
        this.params.putAll(params);
        return this;
    }

    public HttpClientTool param(String key, String value) {
        this.params.put(key, value);
        return this;
    }


    public HttpClientTool encode(String encode) {
        this.encode = encode;
        return this;
    }

    public HttpClientTool url(String url) {
        if (!Pattern.compile(URL_PATTERN).matcher(url).find()) log.error("url格式错误！");
        this.url = url;
        return this;
    }

    public HttpClientTool url(String prefix, String suffix) {
        String url = prefix + suffix;
        if (!Pattern.compile(URL_PATTERN).matcher(url).find()) log.error("url格式错误！");
        this.url = url;
        return this;
    }

    public HttpClientTool bodyJson(Map<String, String> params) {
        this.bodyJson = JSON.toJSONString(params);
        return this;
    }


    public HttpClientTool bodyJson(String bodyJson) {
        this.bodyJson = bodyJson;
        return this;
    }

    public HttpClientTool bodyJson(Object obj) {
        Field[] declaredFields = this.getClass().getDeclaredFields();
        Map<String, String> map = new HashMap<>();
        if (MyTools.MyArrays.isEmpty(declaredFields)) {
            for (Field field : declaredFields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                try {
                    Object o = field.get(this);
                    if (null != o) map.put(fieldName, String.valueOf(o));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        this.bodyJson = JSON.toJSONString(map);
        return this;
    }

    /**
     * 使用httpclient连接池查询
     */
    public HttpClientTool queryByPool() {
        this.pool = true;
        return this;
    }

    public String result() {
        return response;
    }

    public <T> T parse(Type type) {
        if (StringUtils.isNotBlank(response)) return JSON.parseObject(response, type);
        return null;
    }

    public <T> T parse(TypeReference<T> type) {
        if (StringUtils.isNotBlank(response)) return JSON.parseObject(response, type);
        return null;
    }

    /**
     * get请求
     */
    public HttpClientTool doGet() {
        url = perfectUrl(url);
        try {
            URIBuilder builder = new URIBuilder(url);
            if (MyTools.MyMap.isNotEmpty(params))
                params.keySet().forEach(key -> builder.addParameter(key, params.get(key)));
            URI uri = builder.build();
            HttpGet httpGet = new HttpGet(uri);
            getContent(httpGet, encode);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return this;
    }

    /**
     * post请求
     */
    public HttpClientTool doPost() {
        url = perfectUrl(url);
        List<NameValuePair> paramList = Lists.newArrayList();
        HttpPost httpPost = new HttpPost(url);
        if (MyTools.MyMap.isNotEmpty(params))
            paramList = params.keySet().stream().map(key -> new BasicNameValuePair(key, params.get(key))).collect(Collectors.toList());
        // 模拟表单
        UrlEncodedFormEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(paramList, encode);
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        httpPost.setEntity(entity);
        getContent(httpPost, encode);
        return this;
    }


    /**
     * 传输json
     */
    public HttpClientTool doPostJson() {
        url = perfectUrl(url);
        StringEntity entity = new StringEntity(bodyJson, ContentType.APPLICATION_JSON);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        getContent(httpPost, encode);
        return this;

    }

    /**
     * 二进传输请求
     */
    public HttpClientTool doPostRaw() {
        url = perfectUrl(url);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(header);
        StringEntity stringEntity = new StringEntity(bodyJson, encode);
        httpPost.setEntity(stringEntity);
        getContent(httpPost, encode);
        return this;

    }

    /**
     * PUT请求
     */
    public HttpClientTool doPut() {
        url = perfectUrl(url);
        HttpPut httpPut = new HttpPut(url);
        httpPut.setHeader(header);
        StringEntity stringEntity = new StringEntity(bodyJson, encode);
        httpPut.setEntity(stringEntity);
        getContent(httpPut, encode);
        return this;
    }

    /**
     * DELETE请求
     */
    public HttpClientTool doDelete() {
        url = perfectUrl(url);
        HttpDelete httpDelete = new HttpDelete(url);
        getContent(httpDelete, encode);
        return this;
    }


    /**
     * 利用CloseableHttpClient和CloseableHttpResponse实现AutoCloseable接口的特性，使用try-resource来关闭流
     *
     * @param httpBase
     * @param encode
     * @return
     */
    public void getContent(HttpRequestBase httpBase, String encode) {
        if (MyTools.MyMap.isNotEmpty(headers))
            headers.entrySet().forEach(entry -> httpBase.setHeader(entry.getKey(), entry.getValue()));
        log.info("http请求地址:{},请求头{},请求参数{}", url, JSON.toJSONString(headers), params.size() > 0 ? JSON.toJSONString(params) : "" + (bodyJson == null ? "" : bodyJson));
        LocalDateTime start = LocalDateTime.now(Clock.systemDefaultZone());
        if (pool) {//默认不使用连接池
            this.response = HttpClientPool.httpMethod(httpBase, url, encode);
        } else {
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(600000).setConnectTimeout(600000).build();
            httpBase.setConfig(requestConfig);
            try (CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
                 CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpBase)) {
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    response = EntityUtils.toString(httpResponse.getEntity(), encode);
                } else httpBase.abort();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Duration duration = Duration.between(start, LocalDateTime.now(Clock.systemDefaultZone()));
        long runTime = duration.toMillis();
        log.info("http请求执行{}毫秒，返回值{}", runTime, response);
    }

    public static String perfectUrl(String url) {
        if (null != url && !"".equals(url) && (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("ftp://") && !url.startsWith("file://")))
            return "http://" + url;
        return url;
    }

}