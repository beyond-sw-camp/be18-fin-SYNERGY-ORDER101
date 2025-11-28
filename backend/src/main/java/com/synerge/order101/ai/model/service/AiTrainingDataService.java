package com.synerge.order101.ai.model.service;
import java.time.LocalDate;

public interface AiTrainingDataService {
    void sendMonthlyActualSales(LocalDate startDate, LocalDate endDate);
}