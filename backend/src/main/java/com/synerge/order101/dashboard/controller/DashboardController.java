package com.synerge.order101.dashboard.controller;

import com.synerge.order101.dashboard.model.dto.response.DashboardSummaryResponseDto;
import com.synerge.order101.dashboard.model.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hq/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponseDto summary() {
        return dashboardService.getSummary();
    }
}
