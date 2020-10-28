package com.demo.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class DownLoadUtils {


    public static void download(String url, String path, String name) throws Exception {
        String[] split1 = url.split("\\?");
        url = split1[split1.length - 1];
        HttpGet httpGET = HttpClientUtils.getHttpGET(url);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpResponse httpResponse = httpClient.execute(httpGET);
        HttpEntity entity = httpResponse.getEntity();
        String[] split = url.split("/");
        if (StringUtils.isBlank(name)) {
            name = split[split.length - 1];
        }
        if (entity != null) {
            InputStream inputStream = entity.getContent();
            try {
                File file = new File(path, name);
                FileOutputStream fout = new FileOutputStream(file);
                int len = -1;
                byte[] tmp = new byte[1024];
                while ((len = inputStream.read(tmp)) != -1) {
                    fout.write(tmp, 0, len);
                }
                fout.flush();
                fout.close();
            } finally {
                inputStream.close();
            }
        }
        EntityUtils.consume(entity);
        httpClient.close();
    }


}
