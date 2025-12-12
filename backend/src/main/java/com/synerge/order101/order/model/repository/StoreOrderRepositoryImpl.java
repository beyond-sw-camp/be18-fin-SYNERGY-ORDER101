package com.synerge.order101.order.model.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.enums.SettlementType;
import com.synerge.order101.order.model.entity.QStoreOrder;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.settlement.model.entity.Settlement;
import com.synerge.order101.store.model.entity.QStore;
import com.synerge.order101.supplier.model.entity.QSupplier;
import com.synerge.order101.user.model.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static com.synerge.order101.order.model.entity.QStoreOrder.storeOrder;

@Repository
@RequiredArgsConstructor
public class StoreOrderRepositoryImpl implements StoreOrderRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<StoreOrder> search(TradeSearchCondition cond, Pageable pageable) {

        //데이터 목록 조회
        List<StoreOrder> content = queryFactory
                .selectFrom(storeOrder)
                .where(
                        statusIn(cond.getStatuses()),
                        searchTextContains(cond.getSearchText()),
                        DateBetween(cond.getFromDate(), cond.getToDate()),
                        storeIdEq(cond.getVendorId())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(storeOrder.createdAt.desc())
                .fetch();
        // 전체 카운트 조회
        Long total = queryFactory
                .select(storeOrder.count())
                .from(storeOrder)
                .where(
                        statusIn(cond.getStatuses()),
                        searchTextContains(cond.getSearchText()),
                        DateBetween(cond.getFromDate(), cond.getToDate()),
                        storeIdEq(cond.getVendorId())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    // --- 동적 쿼리 조건을 생성하는 BooleanExpression 메소드들 ---}
    private BooleanExpression DateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        // 1. startDate는 해당 날짜의 시작 시간 (00:00:00)
        LocalDateTime start = startDate.atStartOfDay();

        // 2. endDate는 해당 날짜의 '다음 날'의 시작 시간 (00:00:00)으로 설정합니다.
        LocalDate nextDay = endDate.plusDays(1);
        LocalDateTime end = nextDay.atStartOfDay();

        return storeOrder.createdAt.goe(start).and(storeOrder.createdAt.lt(end));
    }
    private BooleanExpression statusIn(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        List<OrderStatus> enumStatues = statuses.stream()
                .map(OrderStatus::valueOf)
                .collect(Collectors.toList());

        return storeOrder.orderStatus.in(enumStatues);
    }

    private BooleanExpression storeIdEq(Long storeId) {
        if (storeId == null) {
            return null;
        }
        return storeOrder.store.storeId.eq(storeId);
    }

    private BooleanExpression searchTextContains(String searchText) {
        if (!StringUtils.hasText(searchText)) {
            return null;
        }
        // 검색어(ID 또는 공급사 이름)를 OR 조건으로 처리
        // settlement_no (DB), supplier_id/store_id (DB)를 사용해야 함
        return storeOrder.orderNo.contains(searchText)
                .or(storeOrder.store.storeName.contains(searchText));
        // 관계 엔티티 조회는 복잡할 수 있으므로, 실+제 DB 스키마에 따라 JOIN 필요
    }

}
