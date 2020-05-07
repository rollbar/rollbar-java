package com.example.springwebmvc;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class ExampleViewController {

    @RequestMapping("/")
    public String index() {
        throw new NullPointerException("New Exception Sent to Rollbar");
    }

}