package com.synerge.order101.purchase.model.repository;


import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.entity.QPurchase;
import com.synerge.order101.supplier.model.entity.QSupplier;
import com.synerge.order101.user.model.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class PurchaseRepositoryImpl implements PurchaseRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QPurchase purchase = QPurchase.purchase;

    @Override
    public Page<Purchase> findByDynamicSearch(
            String keyword,
            OrderStatus status,
            Pageable pageable) {

        // 데이터 목록 조회
        List<Purchase> content = queryFactory
                .selectFrom(purchase)
                .leftJoin(purchase.user, QUser.user).fetchJoin()
                .leftJoin(purchase.supplier, QSupplier.supplier).fetchJoin()
                .where(
                        statusEq(status),
                        keywordContains(keyword)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(purchase.createdAt.desc())
                .fetch();

        // 전체 카운트 조회
        Long total = queryFactory
                .select(purchase.count())
                .from(purchase)
                .leftJoin(purchase.user)
                .leftJoin(purchase.supplier)
                .where(
                        statusEq(status),
                        keywordContains(keyword)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Purchase> search(TradeSearchCondition cond, Pageable pageable) {
        // 조건 매핑
        String searchText = cond.getSearchText();
        List<String> statusStrings = cond.getStatuses();
        Long vendorId = cond.getVendorId();

        // 목록
        List<Purchase> content = queryFactory
                .selectFrom(purchase)
                .leftJoin(purchase.user, QUser.user).fetchJoin()
                .leftJoin(purchase.supplier, QSupplier.supplier).fetchJoin()
                .where(
                        statusIn(statusStrings),
                        searchTextContains(searchText),
                        vendorIdEq(vendorId)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(purchase.createdAt.desc())
                .fetch();

        // total
        Long total = queryFactory
                .select(purchase.count())
                .from(purchase)
                .leftJoin(purchase.user)
                .leftJoin(purchase.supplier)
                .where(
                        statusIn(statusStrings),
                        searchTextContains(searchText),
                        vendorIdEq(vendorId)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    // --- 헬퍼 메서드들 ---
    private BooleanExpression statusEq(OrderStatus status) {
        if (status == null) return null;
        return purchase.orderStatus.eq(status);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        return purchase.poNo.containsIgnoreCase(keyword)
                .or(purchase.user.name.containsIgnoreCase(keyword))
                .or(purchase.supplier.supplierName.containsIgnoreCase(keyword));
    }

    private BooleanExpression statusIn(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) return null;
        return purchase.orderStatus.in(statuses.stream().map(OrderStatus::valueOf).toList());
    }

    private BooleanExpression searchTextContains(String searchText) {
        if (!StringUtils.hasText(searchText)) return null;
        return purchase.poNo.containsIgnoreCase(searchText)
                .or(purchase.user.name.containsIgnoreCase(searchText))
                .or(purchase.supplier.supplierName.containsIgnoreCase(searchText));
    }

    private BooleanExpression vendorIdEq(Long vendorId) {
        if (vendorId == null) return null;
        return purchase.supplier.supplierId.eq(vendorId);
    }

}
