package com.spayker.crypto.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CryptoAnalysisTechProviderApp {

  public static void main(String[] args) {
    SpringApplication.run(CryptoAnalysisTechProviderApp.class);
  }

}