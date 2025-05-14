package com.atme.ssyx.home;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
//会主动连接数据库，加上exclude不用主动连接数据库
//通过远程调用解决问题，不需要连接数据库
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ServiceHomeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceHomeApplication.class,args);
    }
}
