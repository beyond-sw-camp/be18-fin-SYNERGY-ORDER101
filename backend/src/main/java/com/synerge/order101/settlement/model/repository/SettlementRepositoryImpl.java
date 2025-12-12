package com.synerge.order101.settlement.model.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.synerge.order101.common.enums.SettlementType;
import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.settlement.model.entity.Settlement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
// implementation is detected by Spring Data JPA by naming convention (SettlementRepositoryImpl)
// no @Repository annotation to avoid it being created as an independent repository bean
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// unused imports removed
import static com.synerge.order101.settlement.model.entity.QSettlement.settlement;
import static com.synerge.order101.store.model.entity.QStore.store;
import static com.synerge.order101.supplier.model.entity.QSupplier.supplier;

@RequiredArgsConstructor
public class SettlementRepositoryImpl implements SettlementRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Settlement> search(TradeSearchCondition cond, Pageable pageable) {
        //데이터 목록 조회 - LEFT JOIN으로 존재하지 않는 연관 엔터티 처리
        List<Settlement> content = queryFactory
                .selectFrom(settlement)
                .leftJoin(settlement.store, store)
                .leftJoin(settlement.supplier, supplier)
                .where(
                    statusIn(cond.getStatuses()),
                    typeIn(cond.getTypes()),
                    searchTextContains(cond.getSearchText()),
                    DateBetween(cond.getFromDate(), cond.getToDate()),
                    vendorIdEq(cond.getVendorId())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(settlement.createdAt.desc())
                .fetch();
        // 전체 카운트 조회
        Long total = queryFactory
                .select(settlement.count())
                .from(settlement)
                .where(
                        statusIn(cond.getStatuses()),
                        typeIn(cond.getTypes()),
                        searchTextContains(cond.getSearchText()),
                        DateBetween(cond.getFromDate(), cond.getToDate()),
                        vendorIdEq(cond.getVendorId())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    // --- 동적 쿼리 조건을 생성하는 BooleanExpression 메소드들 ---
    private BooleanExpression DateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        // 1. startDate는 해당 날짜의 시작 시간 (00:00:00)
        LocalDateTime start = startDate.atStartOfDay();

        // 2. endDate는 해당 날짜의 '다음 날'의 시작 시간 (00:00:00)으로 설정합니다.
        LocalDate nextDay = endDate.plusDays(1);
        LocalDateTime end = nextDay.atStartOfDay();

        return settlement.createdAt.goe(start).and(settlement.createdAt.lt(end));
    }
    private BooleanExpression statusIn(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        List<Settlement.SettlementStatus> enumStatues = statuses.stream()
                .map(Settlement.SettlementStatus::valueOf)
                .toList();
        return settlement.settlementStatus.in(enumStatues);
    }

    private BooleanExpression typeIn(List<String> types) {
        if (types == null || types.isEmpty()) {
            return null;
        }
        List<SettlementType> enumTypes = types.stream()
                .map(SettlementType::valueOf)
                .collect(Collectors.toList());

        return settlement.settlementType.in(enumTypes);
    }

    private BooleanExpression vendorIdEq(Long vendorId) {
        if (vendorId == null) {
            return null;
        }
        // AR(미수금)은 store, AP(미지급금)은 supplier에 연결되므로 OR 조건으로 처리
        return settlement.supplier.supplierId.eq(vendorId)
                .or(settlement.store.storeId.eq(vendorId));
    }

    private BooleanExpression searchTextContains(String searchText) {
        if (!StringUtils.hasText(searchText)) {
            return null;
        }
        // 검색어(ID 또는 공급사/가맹점 이름)를 OR 조건으로 처리
        BooleanExpression searchCondition = settlement.settlementNo.contains(searchText);
        
        // supplier가 null이 아닌 경우 (AP)
        searchCondition = searchCondition.or(
            settlement.supplier.isNotNull()
                .and(settlement.supplier.supplierName.contains(searchText))
        );
        
        // store가 null이 아닌 경우 (AR)
        searchCondition = searchCondition.or(
            settlement.store.isNotNull()
                .and(settlement.store.storeName.contains(searchText))
        );
        
        return searchCondition;
    }


}
