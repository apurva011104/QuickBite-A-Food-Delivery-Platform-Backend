package com.quickbite.delivery.deliveryservice.mapper;

import org.springframework.stereotype.Component;

import com.quickbite.delivery.deliveryservice.dto.requestDto.DeliveryAgentRequestDto;
import com.quickbite.delivery.deliveryservice.dto.responseDto.DeliveryAgentResponseDto;
import com.quickbite.delivery.deliveryservice.entity.DeliveryAgent;

@Component
public class DeliveryAgentMapper {

    public DeliveryAgent toEntity(DeliveryAgentRequestDto dto) {
        if (dto == null) {
            return null;
        }

        DeliveryAgent agent = new DeliveryAgent();
        agent.setUserId(dto.getUserId());
        agent.setFullName(dto.getFullName());
        agent.setPhone(dto.getPhone());
        agent.setVehicleType(dto.getVehicleType());
        agent.setVehicleNumber(dto.getVehicleNumber());

        return agent;
    }

    public DeliveryAgentResponseDto toResponseDto(DeliveryAgent agent) {
        if (agent == null) {
            return null;
        }

        DeliveryAgentResponseDto dto = new DeliveryAgentResponseDto();
        dto.setAgentId(agent.getAgentId());
        dto.setUserId(agent.getUserId());
        dto.setFullName(agent.getFullName());
        dto.setPhone(agent.getPhone());
        dto.setVehicleType(agent.getVehicleType());
        dto.setVehicleNumber(agent.getVehicleNumber());
        dto.setCurrentLatitude(agent.getCurrentLatitude());
        dto.setCurrentLongitude(agent.getCurrentLongitude());
        dto.setAvailable(agent.isAvailable());
        dto.setVerified(agent.isVerified());
        dto.setAvgRating(agent.getAvgRating());
        dto.setTotalDeliveries(agent.getTotalDeliveries());

        return dto;
    }
}