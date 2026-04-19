package com.quickbite.review.reviewservice.dto.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewModerationRequestDto {

    @NotNull(message = "Verified flag is required")
    private Boolean verified;
}