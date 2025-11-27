package com.synerge.order101.ai.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSeriesPointResponseDto {
    private String date;
    private Integer forecast;
    private Integer actual;
}
