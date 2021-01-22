package com.demo.http;

import com.alibaba.fastjson.JSON;
import com.demo.utils.MyTools;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.pool.PoolStats;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * httpClient适用于对于单个地址的大批次访问，不适合少量的不同的连接
 */
public class HttpClientPool {

    public static Logger log = LoggerFactory.getLogger(HttpClientPool.class);

    private static final int connectPoolTimeout = 20000;// 设定从连接池获取可用连接的时间

    private static final int connectTimeout = 50000;// 建立连接超时时间

    private static final int socketTimeout = 50000;// 设置等待数据超时时间5秒钟 根据业务调整

    private static final int closeConnTimeout = 50000;

    private static final int maxTotal = 100;// 连接池最大连接数

    private static final int maxPerRoute = 10;// 每个主机的并发

    private static final int maxRoute = 50;// 目标主机的最大连接数

    private static CloseableHttpClient httpClient = null;

    private final static Object syncLock = new Object();// 相当于线程锁,用于线程安全

    private static PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = null;

    private static ScheduledExecutorService monitorExecutor;

    private static boolean isShowUsePoolLog = true;

    // HTTP://WWW.BAIDU.COM
    public static CloseableHttpClient getHttpClient2(String url) {
        String hostname = url.split("/")[2];
        int port = 80;
        if (hostname.contains(":")) {
            String[] arr = hostname.split(":");
            hostname = arr[0];
            port = Integer.parseInt(arr[1]);
        }
        // 双重校验锁
        if (httpClient == null) synchronized (syncLock) {
            if (httpClient == null) httpClient = createHttpClient2(maxTotal, maxPerRoute, maxRoute, hostname, port);
        }
        return httpClient;
    }


    public static CloseableHttpClient createHttpClient2(int maxTotal, int maxPerRoute, int maxRoute, String hostname, int port) {
        //用于创建普通（未加密）套接字的默认类
        ConnectionSocketFactory connectionSocketFactory = PlainConnectionSocketFactory.getSocketFactory();
        //创建加密的套接字
        LayeredConnectionSocketFactory layeredConnectionSocketFactory = SSLConnectionSocketFactory.getSocketFactory();

        Registry registry = RegistryBuilder.create().register("http", connectionSocketFactory).register("https", layeredConnectionSocketFactory).build();

        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(registry);

        // 最大连接数
        poolingHttpClientConnectionManager.setMaxTotal(maxTotal);
        // 路由最大连接数
        //路由:包含的数据有目标主机、本地地址、代理链、是否tunnulled、是否layered、是否是安全路由
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxPerRoute);
        // 某个站点或者某个ip的最大连接个数
        poolingHttpClientConnectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(hostname, port)), maxRoute);

        HttpRequestRetryHandler httpRequestRetryHandler = (exception, executionCount, context) -> {
            // 重试3次
            if (executionCount >= 3) return false;
            // 如果服务器丢掉了连接，那么就重试
            if (exception instanceof NoHttpResponseException) return true;
            // 不要重试SSL握手异常
            if (exception instanceof SSLHandshakeException) return false;
            // 超时
            if (exception instanceof InterruptedIOException) return false;
            // 目标服务器不可达
            if (exception instanceof UnknownHostException) return false;
            // 连接被拒绝
            if (exception instanceof ConnectTimeoutException) return false;
            // SSL握手异常
            if (exception instanceof SSLException) return false;
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            // 如果请求是幂等的，就再次尝试
            if (!(request instanceof HttpEntityEnclosingRequest)) return true;
            return false;
        };
        CloseableHttpClient httpClient = HttpClients
                .custom()
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setRetryHandler(httpRequestRetryHandler)
                .build();
        return httpClient;
    }


    public static CloseableHttpClient getHttpClient(final String url) {
        String hostname = url.split("/")[2];
        int port = 80;
        if (hostname.contains(":")) {
            final String[] arr = hostname.split(":");
            hostname = arr[0];
            port = Integer.parseInt(arr[1]);
        }
        if (httpClient == null) {
            System.out.println("1****第一次创建httpClient");
            // 多线程下多个线程同时调用getHttpClient容易导致重复创建httpClient对象的问题,所以加上了同步锁
            synchronized (syncLock) {
                if (httpClient == null) {
                    System.out.println("2****第一次创建httpClient -->" + maxTotal);
                    // 开启监控线程,对异常和空闲线程进行关闭
                    monitorExecutor = Executors.newScheduledThreadPool(1);
                    monitorExecutor.scheduleAtFixedRate(new TimerTask() {

                        @Override
                        public void run() {
                            // 关闭异常连接
                            poolingHttpClientConnectionManager.closeExpiredConnections();
                            // 关闭空闲的连接
                            poolingHttpClientConnectionManager.closeIdleConnections(closeConnTimeout, TimeUnit.MILLISECONDS);
                            final PoolStats poolStats = poolingHttpClientConnectionManager.getTotalStats();
                            final int usePoolNum = poolStats.getAvailable() + poolStats.getLeased() + poolStats.getPending();
                            if (isShowUsePoolLog)
                                log.info("***********》关闭异常+空闲连接！ 空闲连接:" + poolStats.getAvailable() + " 持久连接:" + poolStats.getLeased() + " 最大连接数:" + poolStats.getMax() + " 阻塞连接数:" + poolStats.getPending());
                            if (usePoolNum == 0) {
                                isShowUsePoolLog = false;
                            } else {
                                isShowUsePoolLog = true;
                            }
                        }
                    }, closeConnTimeout, closeConnTimeout, TimeUnit.MILLISECONDS);
                    httpClient = createHttpClient(maxTotal, maxPerRoute, maxRoute, hostname, port);
                }
            }
        } else {
            System.out.println("3****获取已有的httpClient");
        }
        return httpClient;
    }

    private static CloseableHttpClient createHttpClient(int maxTotal, int maxPerRoute, int maxRoute, String hostname, int port) {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", plainsf).register("https", sslsf).build();
        poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(registry);
        // 将最大连接数增加
        poolingHttpClientConnectionManager.setMaxTotal(maxTotal);
        // 将每个路由基础的连接增加
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxPerRoute);
        HttpHost httpHost = new HttpHost(hostname, port);
        // 将目标主机的最大连接数增加
        poolingHttpClientConnectionManager.setMaxPerRoute(new HttpRoute(httpHost), maxRoute);
        // 请求重试处理
        HttpRequestRetryHandler httpRequestRetryHandler = (exception, executionCount, context) -> {
            if (executionCount >= 3) {// 如果已经重试了2次，就放弃
                log.info("*******》重试了3次，就放弃");
                return false;
            }
            if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                log.info("*******》服务器丢掉连接，重试");
                return true;
            }
            if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                log.info("*******》不要重试SSL握手异常");
                return false;
            }
            if (exception instanceof InterruptedIOException) {// 超时
                log.info("*******》 中断");
                return false;
            }
            if (exception instanceof UnknownHostException) {// 目标服务器不可达
                log.info("*******》目标服务器不可达");
                return false;
            }
            if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                log.info("*******》连接超时被拒绝");
                return false;
            }
            if (exception instanceof SSLException) {// SSL握手异常
                log.info("*******》SSL握手异常");
                return false;
            }

            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            // 如果请求是幂等的，就再次尝试
            if (!(request instanceof HttpEntityEnclosingRequest)) return true;
            return false;
        };
        CloseableHttpClient httpClient = HttpClients
                .custom()
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setRetryHandler(httpRequestRetryHandler)
                .setConnectionManagerShared(true)
                .build();
        return httpClient;
    }

    private static void poolConfig(HttpRequestBase httpRequestBase) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(connectPoolTimeout)// 设定从连接池获取可用连接的时间
                .setConnectTimeout(connectTimeout)// 设定连接服务器超时时间
                .setSocketTimeout(socketTimeout)// 设定获取数据的超时时间
                .build();
        httpRequestBase.setConfig(requestConfig);
    }

    public static void setParams(HttpRequestBase httpRequestBase, Map<String, String> params, String encode) {
        try {
            if (MyTools.MyMap.isNotEmpty(params)) {
                List<BasicNameValuePair> nameValuePairs = new ArrayList<>();
                Set<String> keySet = params.keySet();
                for (String key : keySet) nameValuePairs.add(new BasicNameValuePair(key, params.get(key).toString()));
                String param = EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs, encode));
                httpRequestBase.setURI(new URI(httpRequestBase.getURI().toString() + "?" + param));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setHeaders(HttpRequestBase httpRequestBase, Map<String, String> headers) {
        if (MyTools.MyMap.isNotEmpty(headers))
            headers.entrySet().stream().forEach(vo -> httpRequestBase.setHeader(vo.getKey(), vo.getValue()));
    }

    /**
     * 原理及注意事项
     * 连接池中连接都是在发起请求的时候建立，并且都是长连接
     * EntityUtils.consume(entity);作用就是将用完的连接释放，下次请求可以复用，使用response.close();结果就是连接会被关闭，并且不能被复用，这样就失去了采用连接池的意义。
     * 连接池释放连接的时候，并不会直接对TCP连接的状态有任何改变，只是维护了两个Set，leased和avaliabled，leased代表被占用的连接集合，avaliabled代表可用的连接的集合，释放连接的时候仅仅是将连接从leased中remove掉了，并把连接放到avaliabled集合中
     */
    public static String httpMethod(HttpRequestBase httpBase, String url, String encode) {
        CloseableHttpResponse response = null;
        HttpEntity entity = null;
        try {
            poolConfig(httpBase);
            response = getHttpClient2(url).execute(httpBase, HttpClientContext.create());
            entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(entity, encode);
            } else httpBase.abort();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // 关闭HttpEntity的流，如果手动关闭了InputStream in = entity.getContent();这个流，也可以不调用这个方法
                EntityUtils.consume(entity);
                if (response != null) response.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public static String httpPost(String url, Map<String, String> headers, Map<String, String> params) {
        HttpPost httpPost = new HttpPost(url);
        setHeaders(httpPost, headers);
        setParams(httpPost, params, "utf-8");
        return httpMethod(httpPost, url, "utf-8");
    }

    public static String httpPostJson(String url, Map<String, String> headers, Map<String, String> params) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(JSON.toJSONString(params), ContentType.APPLICATION_JSON));
        setHeaders(httpPost, headers);
        setParams(httpPost, params, "utf-8");
        return httpMethod(httpPost, url, "utf-8");
    }


    public static String httpGet(String url, Map<String, String> headers, Map<String, String> params) {
        HttpGet httpGet = new HttpGet(url);
        setHeaders(httpGet, headers);
        setParams(httpGet, params, "utf-8");
        return httpMethod(httpGet, url, "utf-8");
    }

    public static void main(String[] args) {
        String s = httpPostJson("https://open-pro.hikyun.com/artemis/api/eits/v1/trans/device/page/by/userId", MyTools.MyMap.<String, String>builder().of("access_token",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6WyJzY29wZSJdLCJleHAiOjE1ODQzNzAyNjQsImp0aSI6ImZlNjVlYThmLWI4MTQtNDZlNC1hM2I2LTQzNmRlZGE1NDYwZSIsImNsaWVudF9pZCI6IjI2NDA5NjIwIn0.kde1-8Bw3fGd_l9MPvoxbkSaN_VSabVd84wOidmGnfg").build(),
                MyTools.MyMap.<String, String>builder().of("pageNo", "1").of("pageSize", "100").of("userId", "396395598708832").build());
        System.out.println(s);
    }

}
