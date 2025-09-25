package br.com.nfe.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NfeProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(NfeProcessorApplication.class, args);
    }
}
