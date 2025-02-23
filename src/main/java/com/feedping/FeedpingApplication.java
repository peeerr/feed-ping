package com.feedping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class FeedpingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedpingApplication.class, args);
    }

}
