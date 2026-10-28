package com.mia.mini12306;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Mini12306Application {

    public static void main(String[] args) {
        SpringApplication.run(Mini12306Application.class, args);
    }

}
