package com.catording.rcms.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rcms.jwt")
public class JwtProperties {
    private String issuer;
    private String secret;
    private long expireMinutes;
}

