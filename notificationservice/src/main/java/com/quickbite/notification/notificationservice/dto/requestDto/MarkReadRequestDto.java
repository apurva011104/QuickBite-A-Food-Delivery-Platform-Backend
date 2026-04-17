package com.quickbite.notification.notificationservice.dto.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkReadRequestDto {

    @NotNull(message = "Read status is required")
    private Boolean isRead;
}