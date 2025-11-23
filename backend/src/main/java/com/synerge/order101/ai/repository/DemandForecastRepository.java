package com.synerge.order101.ai.repository;

import com.synerge.order101.ai.model.entity.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {
    List<DemandForecast> findByTargetWeek(LocalDate targetWeek);
    List<DemandForecast> findDistinctBySnapshotAtIsNotNullOrderBySnapshotAtDesc();
    List<DemandForecast> findByTargetWeekBetween(LocalDate from, LocalDate to);
}

