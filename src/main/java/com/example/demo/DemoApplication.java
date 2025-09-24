package com.example.demo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DemoApplication {
  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  @RestController
  static class HelloController {
    @GetMapping("/hello")
    public java.util.Map<String,String> hello(){
      return java.util.Collections.singletonMap("message","Hello BuildToFlip v5!");
    }
  }
}
