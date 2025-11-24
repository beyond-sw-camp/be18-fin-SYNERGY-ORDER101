package com.synerge.order101.ai.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrainResultResponseDto {
    private String jobType;
    private String status;
    private Double mae;
    private Double mape;
    private Double smape;
    private Integer bestIteration;
    private Boolean forecastGenerated;

    private String message;           // 파이썬에서 내려주는 로그/메시지
}
