package com.synerge.order101.ai.model.repository;

import com.synerge.order101.ai.model.entity.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {
    List<DemandForecast> findByTargetWeek(LocalDate targetWeek);
    List<DemandForecast> findDistinctBySnapshotAtIsNotNullOrderBySnapshotAtDesc();
    List<DemandForecast> findByTargetWeekBetween(LocalDate from, LocalDate to);

    @Query("""
    select df
    from DemandForecast df
    where df.snapshotAt = (
        select max(df2.snapshotAt)
        from DemandForecast df2
    )
    """)
    List<DemandForecast> findLatestSnapshotForecasts();

    @Query("""
    SELECT df 
    FROM DemandForecast df
    JOIN FETCH df.product p
    JOIN FETCH p.productCategory pc
    LEFT JOIN FETCH pc.parent parent
    WHERE df.targetWeek BETWEEN :from AND :to
""")
    List<DemandForecast> findWithProductAndCategoryByTargetWeekBetween(
            LocalDate from, LocalDate to
    );

    @Query("""
        SELECT df
        FROM DemandForecast df
        JOIN FETCH df.product p
        JOIN FETCH p.productCategory pc
        LEFT JOIN FETCH pc.parent parent
        WHERE df.targetWeek BETWEEN :from AND :to
    """)
    List<DemandForecast> findLatestWithProductAndCategory(LocalDate from, LocalDate to);

}

