package com.example.springbootwebmvc;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.spring.webmvc.RollbarSpringConfigBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@Configuration()
@EnableWebMvc
@ComponentScan({"com.example.springbootwebmvc","com.rollbar.spring.boot.webmvc"})
public class RollbarConfig {

    /**
     * Register a Rollbar bean to configure App with Rollbar.
     */
    @Bean
    public Rollbar rollbar() {
        return new Rollbar(getRollbarConfigs(this.accessToken));
    }

    @Value("${rollbar.access_token}")
    private String accessToken;

    @Value("${rollbar.environment}")
    private String environment;

    @Value("${rollbar.framework}")
    private String framework;

    private Config getRollbarConfigs(String accessToken) {

        // Reference ConfigBuilder.java for all the properties you can set for Rollbar
        return new RollbarSpringConfigBuilder(accessToken)
                .environment("development")
                .build();
    }
}