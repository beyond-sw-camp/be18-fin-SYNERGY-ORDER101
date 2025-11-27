package com.synerge.order101.settlement.model.repository;


import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.settlement.model.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SettlementRepositoryCustom{

    /**
     * 정산 목록을 검색 조건(cond)과 페이징 정보에 따라 조회
     * @param cond     검색 조건
     * @param pageable 페이징 정보
     * @return 페이징된 정산 목록
     */
    Page<Settlement> search(TradeSearchCondition cond, Pageable pageable);


}
