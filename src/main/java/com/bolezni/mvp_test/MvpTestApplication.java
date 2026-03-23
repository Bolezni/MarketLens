package com.bolezni.mvp_test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MvpTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(MvpTestApplication.class, args);
    }

}
