package com.quickbite.delivery.deliveryservice.event;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent implements Serializable {

    private String eventType;
    private Long recipientId;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
}