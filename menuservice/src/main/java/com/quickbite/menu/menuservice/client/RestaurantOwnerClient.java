package com.quickbite.menu.menuservice.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.quickbite.menu.menuservice.dto.external.RestaurantSummaryDto;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RestaurantOwnerClient {

    private final RestTemplate restTemplate;

    @Value("${restaurant.service.base-url}")
    private String restaurantServiceBaseUrl;

    public RestaurantOwnerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isRestaurantOwnedByCurrentOwner(Long restaurantId) {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<RestaurantSummaryDto> restaurants = restTemplate.exchange(
                restaurantServiceBaseUrl + "/restaurants/owner/my",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<RestaurantSummaryDto>>() {}
        ).getBody();

        return restaurants != null && restaurants.stream()
                .anyMatch(r -> r.getRestaurantId().equals(restaurantId));
    }
}