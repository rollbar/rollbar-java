package com.example.springwebmvc;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.notifier.Rollbar;
import com.rollbar.spring.webmvc.RollbarExceptionResolver;
import com.rollbar.web.provider.RequestProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration()
@ComponentScan({"com.example.springbootwebmvc","com.rollbar.spring.webmvc"})
public class RollbarConfig {

    /**
     * Register a Rollbar bean to configure App with Rollbar.
     */
    @Bean(name = "rollbar")
    public Rollbar rollbar() {
        return Rollbar.init(withAccessToken("<TOKEN>")
                .request(new RequestProvider.Builder().build())
                .build());
    }

    @Bean
    public HandlerExceptionResolver rollbarExceptionResolver() {
        return new RollbarExceptionResolver(rollbar());
    }

}