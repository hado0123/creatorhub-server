package com.creatorhub.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "callback")
public record CallbackProperties(
        String secret,
        long allowedSkewMillis
) {}
