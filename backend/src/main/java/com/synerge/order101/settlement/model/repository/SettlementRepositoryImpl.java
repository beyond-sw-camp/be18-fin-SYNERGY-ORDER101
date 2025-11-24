package com.synerge.order101.settlement.model.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.synerge.order101.common.enums.SettlementType;
import com.synerge.order101.settlement.model.dto.SettlementSearchCondition;
import com.synerge.order101.settlement.model.entity.Settlement;
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

import static com.synerge.order101.settlement.model.entity.QSettlement.settlement;

@RequiredArgsConstructor
@Repository
public class SettlementRepositoryImpl implements SettlementRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Settlement> search(SettlementSearchCondition cond, Pageable pageable) {
        //데이터 목록 조회
        List<Settlement> content = queryFactory
                .selectFrom(settlement)
                .where(
                    statusIn(cond.getStatuses()),
                    typeIn(cond.getTypes()),
                    searchTextContains(cond.getSearchText()),
                    DateBetween(cond.getFromDate(), cond.getToDate()),
                    supplierIdEq(cond.getVendorId()),
                    storeIdEq(cond.getVendorId())
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
                        DateBetween(cond.getFromDate(), cond.getToDate())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    // --- 동적 쿼리 조건을 생성하는 BooleanExpression 메소드들 ---}
    private BooleanExpression DateBetween(LocalDate startDate, LocalDate endDate) {
        if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate)) {
                return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            return settlement.createdAt.between(startDate.atStartOfDay(), endDate.atStartOfDay());
        } catch (Exception e) {
            return null;
        }
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

    private BooleanExpression supplierIdEq(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return settlement.supplier.supplierId.eq(supplierId);
    }

    private BooleanExpression storeIdEq(Long storeId) {
        if (storeId == null) {
            return null;
        }
        return settlement.store.storeId.eq(storeId);
    }

    private BooleanExpression searchTextContains(String searchText) {
        if (!StringUtils.hasText(searchText)) {
            return null;
        }
        // 검색어(ID 또는 공급사 이름)를 OR 조건으로 처리
        // settlement_no (DB), supplier_id/store_id (DB)를 사용해야 함
        return settlement.settlementNo.contains(searchText)
                .or(settlement.supplier.supplierName.contains(searchText));
        // 관계 엔티티 조회는 복잡할 수 있으므로, 실+제 DB 스키마에 따라 JOIN 필요
    }


}
