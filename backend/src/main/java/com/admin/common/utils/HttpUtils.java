package com.admin.common.utils;

import com.admin.common.dto.GostDto;
import com.admin.config.RestTemplateConfig;
import com.alibaba.fastjson.JSONObject;
import lombok.SneakyThrows;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP请求工具类
 * 支持GET和POST请求，支持表单和JSON格式的请求体
 */
public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    /**
     * 自定义错误处理器，不抛出异常
     */
    private static class NoOpResponseErrorHandler implements ResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return false;
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
        }
    }

    @SneakyThrows
    public static GostDto post(String url, Object requestBody, String secret){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String auth = secret + ":" + secret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate(RestTemplateConfig.generateHttpRequestFactory());
        restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
        try {
            ResponseEntity<GostDto> response = restTemplate.postForEntity(url, entity, GostDto.class);
            GostDto body = response.getBody();
            if (body.getMsg() != null && body.getMsg().contains("exists")) {
                body.setMsg("OK");
            }
            return body;
        } catch (Exception e) {
            GostDto gostDto = new GostDto();
            gostDto.setCode(500);
            gostDto.setMsg("请求失败");
            return gostDto;
        }
    }

    @SneakyThrows
    public static GostDto put(String url, Object requestBody, String secret){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String auth = secret + ":" + secret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate(RestTemplateConfig.generateHttpRequestFactory());
        restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
        try {
            ResponseEntity<GostDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    GostDto.class
            );
            GostDto body = response.getBody();
            return body;
        } catch (Exception e) {
            GostDto gostDto = new GostDto();
            gostDto.setCode(500);
            gostDto.setMsg("请求失败");
            return gostDto;
        }
    }

    @SneakyThrows
    public static GostDto delete(String url, String secret) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // Basic Auth
        String auth = secret + ":" + secret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate(RestTemplateConfig.generateHttpRequestFactory());
        restTemplate.setErrorHandler(new NoOpResponseErrorHandler());

        try {
            ResponseEntity<GostDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    GostDto.class
            );
            GostDto body = response.getBody();
            if (body != null && body.getMsg() != null && body.getMsg().contains("not found")) {
                body.setMsg("OK");
            }
            return body;
        } catch (Exception e) {
            GostDto gostDto = new GostDto();
            gostDto.setCode(500);
            gostDto.setMsg("请求失败");
            return gostDto;
        }
    }

    @SneakyThrows
    public static GostDto delete(String url, JSONObject data, String secret) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // Basic Auth
        String auth = secret + ":" + secret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<JSONObject> entity = new HttpEntity<>(data, headers);
        RestTemplate restTemplate = new RestTemplate(RestTemplateConfig.generateHttpRequestFactory());
        restTemplate.setErrorHandler(new NoOpResponseErrorHandler());

        try {
            ResponseEntity<GostDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    GostDto.class
            );
            GostDto body = response.getBody();
            if (body != null && body.getMsg() != null && body.getMsg().contains("not found")) {
                body.setMsg("OK");
            }
            return body;
        } catch (Exception e) {
            GostDto gostDto = new GostDto();
            gostDto.setCode(500);
            gostDto.setMsg("请求失败");
            return gostDto;
        }
    }
} 