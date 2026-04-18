package com.quickbite.cart.cartservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.quickbite.cart.cartservice.dto.external.MenuItemSnapshotDto;

@Component
public class MenuClient {

    private final RestTemplate restTemplate;

    @Value("${menu.service.base-url:http://localhost:8082}")
    private String menuServiceBaseUrl;

    public MenuClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MenuItemSnapshotDto getMenuItemById(Long itemId) {
        return restTemplate.getForObject(
                menuServiceBaseUrl + "/menu/item/" + itemId,
                MenuItemSnapshotDto.class
        );
    }
}