package org.yao.curator.x.discovery.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@SpringBootApplication
public class CuratorXDiscoveryServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(CuratorXDiscoveryServerApplication.class, args);
  }
}
