package com.creatorhub.config;

import com.creatorhub.security.filter.LambdaCallbackAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<LambdaCallbackAuthFilter> lambdaCallbackAuthFilter(CallbackProperties props) {
        FilterRegistrationBean<LambdaCallbackAuthFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new LambdaCallbackAuthFilter(props));
        bean.setOrder(0);
        return bean;
    }
}