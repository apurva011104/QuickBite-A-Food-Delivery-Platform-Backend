package com.quickbite.menu.menuservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;

import com.quickbite.menu.menuservice.client.RestaurantOwnerClient;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"jwt.secret=12345678901234567890123456789012",
		"spring.cloud.discovery.enabled=false"
})
class MenuserviceApplicationTests {

	@MockBean
	private RestaurantOwnerClient restaurantOwnerClient;

	@Test
	void contextLoads() {
	}

}
