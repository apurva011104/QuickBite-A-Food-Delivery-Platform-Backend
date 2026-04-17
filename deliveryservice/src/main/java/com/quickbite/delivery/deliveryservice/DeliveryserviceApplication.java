package com.quickbite.delivery.deliveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class DeliveryserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliveryserviceApplication.class, args);
		log.info("Delivery Service is running...");
	}

}
