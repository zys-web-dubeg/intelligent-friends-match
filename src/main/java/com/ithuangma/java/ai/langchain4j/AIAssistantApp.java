package com.ithuangma.java.ai.langchain4j;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.ithuangma.java.ai.langchain4j.mapper")
@EnableScheduling
public class AIAssistantApp {
    public static void main(String[] args) {
        SpringApplication.run(AIAssistantApp.class, args);
    }
}