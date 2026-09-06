package com.wilfredchau.synapsepkb;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@MapperScan(basePackages = "com.wilfredchau.synapsepkb", annotationClass = Mapper.class)
public class SynapsePkbApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapsePkbApplication.class, args);
    }
}
