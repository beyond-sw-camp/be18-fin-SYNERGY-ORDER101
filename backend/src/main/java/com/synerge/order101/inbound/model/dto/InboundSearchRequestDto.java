package com.synerge.order101.inbound.model.dto;

import java.time.LocalDate;
import java.util.List;

public record InboundSearchRequestDto(
    Long supplierId,
    LocalDate startDate,
    LocalDate endDate,
    List<String> status,
    Integer page,
    Integer numOfRows
) {}
