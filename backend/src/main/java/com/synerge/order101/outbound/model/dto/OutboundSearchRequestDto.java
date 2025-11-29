package com.synerge.order101.outbound.model.dto;

import java.time.LocalDate;

public record OutboundSearchRequestDto(
        Long storeId,
        LocalDate startDate,
        LocalDate endDate,
        Integer page,
        Integer numOfRows
) {}
