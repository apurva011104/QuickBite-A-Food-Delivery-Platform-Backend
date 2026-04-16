package com.quickbite.delivery.deliveryservice.mapper;

import org.springframework.stereotype.Component;

import com.quickbite.delivery.deliveryservice.dto.requestDto.DeliveryAgentRequestDto;
import com.quickbite.delivery.deliveryservice.dto.responseDto.DeliveryAgentResponseDto;
import com.quickbite.delivery.deliveryservice.entity.DeliveryAgent;

@Component
public class DeliveryAgentMapper {

    public DeliveryAgent toEntity(DeliveryAgentRequestDto dto) {
        return new DeliveryAgent(
                dto.getUserId(),
                dto.getFullName(),
                dto.getPhone(),
                dto.getVehicleType(),
                dto.getVehicleNumber(),
                dto.getCurrentLatitude(),
                dto.getCurrentLongitude()
        );
    }

    public DeliveryAgentResponseDto toDto(DeliveryAgent agent) {
        DeliveryAgentResponseDto dto = new DeliveryAgentResponseDto();
        dto.setAgentId(agent.getId());
        dto.setFullName(agent.getFullName());
        dto.setPhone(agent.getPhone());
        dto.setVehicleType(agent.getVehicleType().name());
        dto.setAvailable(agent.isAvailable());
        dto.setVerified(agent.isVerified());
        dto.setAvgRating(agent.getAvgRating());
        return dto;
    }
}