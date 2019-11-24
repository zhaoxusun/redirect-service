package com.personal.mock.filter;

import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.personal.mock.po.MockApp;
import com.personal.mock.route.ProxyZuulRoute;
import com.personal.mock.route.ProxyZuulRouteLocator;
import com.personal.mock.service.MockAppService;
import com.personal.mock.service.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static com.personal.mock.common.Constant.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * author: zhaoxu
 * date: 2019/4/30 上午10:07
 */
@Component
public class RedirectPreFilter extends ZuulFilter {

    private static Logger logger = LoggerFactory.getLogger(RedirectPreFilter.class);

    @Value(value = "${zuul.routes.redirect.path}")
    private String path;

    @Value(value = "${mock.request.address}")
    private String mockRequestAddress;
    @Value(value = "${mock.response.address}")
    private String mockResponseAddress;
    @Value(value = "${proxy.address}")
    private String proxyAddress;
    @Value(value = "${redirect.address}")
    private String redirectAddress;

    @Autowired
    MockAppService mockAppService;

    @Autowired
    ProxyZuulRouteLocator mockZuulRouteLocator;

    @Autowired
    RequestService requestService;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 4;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest httpServletRequest = ctx.getRequest();
        Integer mockAppId = Integer.parseInt(httpServletRequest.getHeader(MOCK_APP_ID));
        MockApp mockApp = mockAppService.searchMockAppById(mockAppId);
        String redirectUrl = mockApp.getRedirectUrl();
        String redirectQuery = mockApp.getRedirectQuery();
        String redirectBody = mockApp.getRedirectBody();
        String redirectHeader = mockApp.getRedirectHeader();
        String redirectType = mockApp.getRedirectType();

        System.out.println(ctx.getRouteHost());
        System.out.println(ctx.getRequestQueryParams());
        System.out.println(ctx.getRequest());
        System.out.println(ctx.getZuulRequestHeaders());
        ProxyZuulRoute mockZuulRoute = new ProxyZuulRoute(mockAppId.toString(),path,redirectUrl);


//        ctx.setRequest(httpServletRequest);

        //proxy服务转发请求，如果再次转向mock服务，不删掉mock相关header；否则删除mock相关header
        if (!(redirectUrl.equals(mockRequestAddress) || redirectUrl.equals(mockResponseAddress) || redirectUrl.equals(proxyAddress) || redirectUrl.equals(redirectAddress))){
            Set<String> sensitiveHeaders = new HashSet<>();
            sensitiveHeaders.add(MOCK_STRATEGY_ID);
            sensitiveHeaders.add(MOCK_APP_ID);
            sensitiveHeaders.add(REQUEST_URI);
            mockZuulRoute.setSensitiveHeaders(sensitiveHeaders);
        }
        mockZuulRouteLocator.updateRoutes(mockZuulRoute);


        Enumeration<String> header = httpServletRequest.getHeaderNames();
        if (!header.hasMoreElements()) {
            return null;
        }
        JSONObject headerJson = new JSONObject();
        while (header.hasMoreElements()) {
            String key = header.nextElement();
            headerJson.put(key,httpServletRequest.getHeader(key));
        }

        logger.info("【请求命中了mock数据，具体如下】");
        logger.info("* 命中策略为：将原请求转发到真实服务上，真实服务地址通过数据库mock_app.proxy_url_perfix字段配置");
        logger.info("* request uri:"+mockApp.getRequestUri());
        logger.info("* 命中mock_app.id="+mockAppId);
        logger.info("【请求转发，具体如下】");
        logger.info("* request_method:"+httpServletRequest.getMethod());
        logger.info("* request_url:"+mockApp.getProxyUrlPerfix());
        logger.info("* request_query:"+httpServletRequest.getQueryString());
        logger.info("* request_header:"+headerJson);
        logger.info("* request_body:"+requestService.getRequestBody(httpServletRequest));
        return null;
    }
}
