/*
 * Copyright 2001-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.learn.httpclientdemo.utils;

import com.learn.httpclientdemo.domain.HttpResult;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p> Title: </p>
 *
 * <p> Description: </p>
 *
 * @author: Guo Weifeng
 * @version: 1.0
 * @create: 2020/1/8 9:19
 */
@Component
public class HttpClientUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private RequestConfig config;

    private static final int CACHE = 10 * 1024;

    /**
     * 不带参数的get请求，如果返回码不为指定返回码，则返回null
     * @param url 请求地址
     * @return
     * @throws Exception
     */
    public String doGet(String url) throws Exception {
        // httpGet请求
        HttpGet httpGet = new HttpGet(url);

        // 设置请求配置信息
        httpGet.setConfig(config);

        // 发起请求，获取返回
        CloseableHttpResponse response = this.httpClient.execute(httpGet);

        // 判断返回码是否为指定返回码
        if (response.getStatusLine().getStatusCode() == 200) {
            return EntityUtils.toString(response.getEntity(), "Utf-8");
        }

        return null;
    }

    /**
     * 带参数的get请求，如果不是指定的状态码，则返回null
     * @param url
     * @param params
     * @return
     * @throws Exception
     */
    public String doGet(String url, Map<String, Object> params) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(url);

        if (params != null) {
            // 遍历，拼接请求参数
            for (Map.Entry<String, Object> entry: params.entrySet()) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue().toString());
            }
        }

        // 调用不带参数的doGet请求
        return doGet(uriBuilder.build().toString());
    }

    /**
     * 带参数的post请求
     * @param url
     * @param params
     * @return
     * @throws Exception
     */
    public HttpResult doPost(String url, Map<String, Object> params) throws Exception {
        // 声明httpPost请求
        HttpPost httpPost = new HttpPost(url);

        // 设置请求配置
        httpPost.setConfig(config);

        // 判断是否为空，遍历，封装form表单对象
        if (params != null) {
            List<NameValuePair> list = new ArrayList<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }

            // 构造form表单对象
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(list, "UTF-8");

            // 把表单对象注入到post里
            httpPost.setEntity(urlEncodedFormEntity);
        }

        // 发起请求
        CloseableHttpResponse response = this.httpClient.execute(httpPost);
        return new HttpResult(response.getStatusLine().getStatusCode(),
            EntityUtils.toString(response.getEntity(), "UTF-8"));
    }

    /**
     * 不带参数的post请求
     * @param url
     * @return
     * @throws Exception
     */
    public HttpResult doPost(String url) throws Exception {
        return this.doPost(url, null);
    }

    public String download(String url, RequestMethod method) throws Exception {
        CloseableHttpResponse response = null;

        if (RequestMethod.POST == method) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(config);
            response = this.httpClient.execute(httpPost);
        } else {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(config);
            response = this.httpClient.execute(httpGet);
        }

        HttpEntity entity = response.getEntity();
        // 内容输入流
        InputStream in = entity.getContent();

        // FIXME: 这里需要指定自己的文件保存内容
        String filePath = "/xxx/xxx/xxx.json";

        File file = new File(filePath);
        // 文件的目录不存在，那么兴建目录
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        // 文件存在，删除旧文件
        if (file.exists()) {
            file.delete();
        }

        /**
         * 根据实际情况，设置缓冲区大小
         */
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            int ch;
            byte[] buffer = new byte[CACHE];
            while ((ch = in.read(buffer)) != -1) {
                out.write(buffer, 0, ch);
            }
            // 写入磁盘
            out.flush();
        } catch (FileNotFoundException e) {
            logger.error("未找到文件-" + file.getName());
        } finally {
            out.close();
            in.close();
        }

        return filePath;
    }
}