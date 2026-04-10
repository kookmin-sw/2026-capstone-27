package org.example.shield.brief.controller.dto;

import java.util.List;

public record DeliveryListResponse(
        List<DeliveryResponse> deliveries
) {}
