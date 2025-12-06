package com.synerge.order101.dashboard.controller;

import com.synerge.order101.dashboard.model.dto.response.StoreDashboardSummaryResponseDto;
import com.synerge.order101.dashboard.model.service.StoreDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/store/dashboard")
@RequiredArgsConstructor
public class StoreDashboardController {

    private final StoreDashboardService storeDashboardService;

    @GetMapping("/summary")
    public StoreDashboardSummaryResponseDto getSummary() {

        Long storeId = 1L;

        return storeDashboardService.getSummary(storeId);
    }
}
