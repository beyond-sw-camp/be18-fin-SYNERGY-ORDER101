package com.synerge.order101.ai.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class CategoryMetricResponseDto {
    private String category;
    private Double metric;
}