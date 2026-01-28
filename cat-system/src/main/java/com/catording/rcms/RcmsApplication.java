package com.catording.rcms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@MapperScan("com.catording.rcms.**.mapper")
@ConfigurationPropertiesScan("com.catording.rcms")
public class RcmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(RcmsApplication.class, args);
    }
}

