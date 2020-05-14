package com.example.springwebmvc;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.notifier.Rollbar;
import com.rollbar.spring.webmvc.RollbarExceptionResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.context.annotation.ComponentScan;

@Controller
@Configuration()
@ComponentScan({"com.example.springbootwebmvc","com.rollbar.spring.webmvc"})
public class HelloController {
    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String hello(@RequestParam(value="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "hello";
    }

    @RequestMapping(value = "/exception", method = RequestMethod.GET)
    public void exception() {
        int x = 1 / 0;
    }

    @Bean
    public HandlerExceptionResolver rollbarExceptionResolver() {
        return new RollbarExceptionResolver(Rollbar.init(withAccessToken("<TOKEN>").build()));
    }
}
