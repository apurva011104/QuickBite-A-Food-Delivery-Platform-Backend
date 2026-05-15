package com.quickbite.payment.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:paymentservice;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.config.import=",
		"jwt.secret=super-secret-key-for-payment-service-tests-12345",
		"jwt.expiration=3600000",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"spring.rabbitmq.host=localhost",
		"spring.rabbitmq.port=5672",
		"spring.rabbitmq.username=guest",
		"spring.rabbitmq.password=guest",
		"razorpay.key-id=rzp_test_key",
		"razorpay.key-secret=rzp_test_secret"
})
class PaymentserviceApplicationTests {

	@MockBean
	private RabbitTemplate rabbitTemplate;

	@Test
	void contextLoads() {
	}

}
