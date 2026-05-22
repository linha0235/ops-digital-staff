package com.itheima.ops.digital.staff;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.itheima.ops.digital.staff.mapper")
public class OpsDigitalStaffApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpsDigitalStaffApplication.class, args);
    }
}
