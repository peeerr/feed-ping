package com.feedping.config;

import com.rometools.rome.io.SyndFeedInput;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RssConfig {

    @Bean
    public SyndFeedInput syndFeedInput() {
        return new SyndFeedInput();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
