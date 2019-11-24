package com.personal.mock.service;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * author: zhaoxu
 * date: 2019/4/13 下午10:19
 */
public interface RequestService {
    JSONObject getRequestBody(HttpServletRequest httpServletRequest);
    JSONObject getRequestHeader(HttpServletRequest httpServletRequest);
    String getRequestURI(HttpServletRequest httpServletRequest);
    String getQueryString(HttpServletRequest httpServletRequest);
    String getMethod(HttpServletRequest httpServletRequest);
}
