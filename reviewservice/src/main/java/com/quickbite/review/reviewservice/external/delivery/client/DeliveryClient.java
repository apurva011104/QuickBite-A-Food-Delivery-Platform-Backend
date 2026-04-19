package com.quickbite.review.reviewservice.external.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.quickbite.review.reviewservice.external.delivery.dto.DeliveryRatingUpdateRequestDto;

@FeignClient(name = "DELIVERYSERVICE")
public interface DeliveryClient {

    @PutMapping("/api/v1/agents/{agentId}/rating")
    void updateDeliveryRating(@PathVariable("agentId") Long agentId,
                              @RequestBody DeliveryRatingUpdateRequestDto requestDto);
}