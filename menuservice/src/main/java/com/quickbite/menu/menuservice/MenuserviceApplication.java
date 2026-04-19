package com.quickbite.menu.menuservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@EnableFeignClients
public class MenuserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MenuserviceApplication.class, args);
		log.info("Menu Service is running...");
	}

}
