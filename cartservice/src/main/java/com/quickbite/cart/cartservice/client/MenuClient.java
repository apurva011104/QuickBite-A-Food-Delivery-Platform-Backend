package com.quickbite.cart.cartservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.cart.cartservice.dto.external.MenuItemSnapshotDto;


@FeignClient(name = "MENUSERVICE")
public interface MenuClient {

    @GetMapping("/menu/item/{id}")
    MenuItemSnapshotDto getMenuItemById(@PathVariable("id") Long itemId);
}