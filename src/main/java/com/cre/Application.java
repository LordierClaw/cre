package com.cre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
    basePackages = "com.cre",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASPECTJ,
        pattern = "com.cre.fixtures.*"
    )
)
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
