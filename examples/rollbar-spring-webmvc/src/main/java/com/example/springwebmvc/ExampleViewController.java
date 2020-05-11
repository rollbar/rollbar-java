package com.example.springwebmvc;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleViewController {

  @RequestMapping("/")
  public void index() {
    int x = 1 / 0;
  }
}