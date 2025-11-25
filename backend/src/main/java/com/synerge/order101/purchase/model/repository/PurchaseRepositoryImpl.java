package com.synerge.order101.purchase.model.repository;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.purchase.model.dto.PurchaseSummaryResponseDto;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.entity.QPurchase;
import com.synerge.order101.supplier.model.entity.QSupplier;
import com.synerge.order101.user.model.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.synerge.order101.settlement.model.entity.QSettlement.settlement;

@Repository
@RequiredArgsConstructor
public class PurchaseRepositoryImpl implements PurchaseRepositoryCustom {

    private final QPurchase purchase = QPurchase.purchase;
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Purchase> findByDynamicSearch(
            String keyword,
            OrderStatus status,
            Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();

        // 1. 상태 필터 조건 (status 있으면)
        if (status != null && !status.equals("전체")) {
            builder.and(purchase.orderStatus.stringValue().equalsIgnoreCase(status.toString())); // Enum인 경우 stringValue() 사용
        }

        // 2. 키워드 검색 조건 (검색어 있으면)
        if (keyword != null && !keyword.isEmpty()) {
            BooleanExpression keywordCondition =
                    purchase.poNo.containsIgnoreCase(keyword) // PO 번호
                            .or(purchase.user.name.containsIgnoreCase(keyword)) // 요청자 이름
                            .or(purchase.supplier.supplierName.containsIgnoreCase(keyword)); // 공급업체
            builder.and(keywordCondition);
        }

        Long totalCount = queryFactory
                .select(purchase.count())
                .from(purchase)
                .leftJoin(purchase.user)
                .leftJoin(purchase.supplier)
                .where(builder)
                .fetchOne();

        if (totalCount == null || totalCount == 0) {
            return new PageImpl<>(List.of(), pageable, 40);
        }

        // 3. 쿼리 생성 및 실행 (Querydsl Pageable 처리 생략, 간단한 예시)
        List<Purchase> content = queryFactory
                .selectFrom(purchase)
                .join(purchase.user, QUser.user).fetchJoin()        // User 엔티티를 함께 로드
                .join(purchase.supplier, QSupplier.supplier).fetchJoin() // Supplier 엔티티를 함께 로드
                .where(builder) // 💡 동적 WHERE 절 적용
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(purchase.createdAt.desc())
                .fetch();

        return new PageImpl<>(content,pageable,totalCount);
    }
}
