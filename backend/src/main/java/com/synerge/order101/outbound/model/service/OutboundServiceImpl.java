package com.synerge.order101.outbound.model.service;

import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.outbound.model.dto.OutboundDetailResponseDto;
import com.synerge.order101.outbound.model.dto.OutboundResponseDto;
import com.synerge.order101.outbound.model.dto.OutboundSearchRequestDto;
import com.synerge.order101.outbound.model.entity.Outbound;
import com.synerge.order101.outbound.model.entity.OutboundDetail;
import com.synerge.order101.outbound.model.repository.OutboundDetailRepository;
import com.synerge.order101.outbound.model.repository.OutboundRepository;
import com.synerge.order101.shipment.event.ShipmentInTransitEvent;
import com.synerge.order101.warehouse.model.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundServiceImpl implements OutboundService {
    private final OutboundRepository outboundRepository;
    private final OutboundDetailRepository outboundDetailRepository;
    private final InventoryService inventoryService;
    private final StoreOrderRepository storeOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OutboundResponseDto> getOutboundList(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Object[]> result = outboundRepository.findOutboundWithCounts(pageable);

        return result.map(row -> {
            Outbound o = (Outbound) row[0];
            Integer itemCount = ((Number) row[1]).intValue();
            Integer totalQty = ((Number) row[2]).intValue();

            return new OutboundResponseDto(
                    o.getOutboundId(),
                    o.getOutboundNo(),
                    o.getOutboundDatetime(),
                    o.getStore().getStoreName(),
                    itemCount,
                    totalQty
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public OutboundDetailResponseDto getOutboundDetail(Long outboundId) {
        Outbound outbound = outboundRepository.findById(outboundId)
                .orElseThrow(() -> new RuntimeException("출고 정보를 찾을 수 없습니다."));

        List<OutboundDetail> details = outboundDetailRepository.findByOutbound(outboundId);

        List<OutboundDetailResponseDto.Item> items = details.stream()
                .map(d -> OutboundDetailResponseDto.Item.builder()
                        .productCode(d.getProduct().getProductCode())
                        .productName(d.getProduct().getProductName())
                        .shippedQty(d.getOutboundQty())
                        .build())
                .toList();

        return OutboundDetailResponseDto.builder()
                .outboundNo(outbound.getOutboundNo())
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public void createOutboundFromShipment(ShipmentInTransitEvent event) {
        // 주문 조회
        StoreOrder storeOrder = storeOrderRepository.findById(event.storeOrderId())
                .orElseThrow(() -> new IllegalStateException("해당 주문을 찾을 수 없습니다: id=" + event.storeOrderId()));

        // 출고 생성
        Outbound outbound = Outbound.create(
                storeOrder.getWarehouse(),  // 주문에 포함된 창고
                storeOrder.getStore(),      // 주문한 가맹점
                generateOutboundNo(storeOrder),
                "SYSTEM"                    // 작성자
        );
        outboundRepository.save(outbound);

        // 주문 상세 기반 출고 상세 생성
        storeOrder.getStoreOrderDetails().forEach(detail -> {
            OutboundDetail outboundDetail = new OutboundDetail(outbound, detail.getProduct(), detail.getOrderQty().intValue());
            outboundDetailRepository.save(outboundDetail);
        });

        // 주문 상세 기반 재고 차감
        storeOrder.getStoreOrderDetails().forEach(detail ->
                inventoryService.decreaseInventory(detail.getProduct().getProductId(), detail.getOrderQty().intValue())
        );

        // 로그
        log.info("출고 생성 및 재고 차감 완료: shipmentId={}, storeOrderId={}",
                event.shipmentId(), event.storeOrderId());
    }

    @Override
    @Transactional
    public Page<OutboundResponseDto> searchOutboundList(OutboundSearchRequestDto request) {

        int page = request.page();
        int numOfRows = request.numOfRows();

        Pageable pageable = PageRequest.of(page - 1, numOfRows, Sort.by("outboundId").descending());

        LocalDateTime start = request.startDate() != null ? request.startDate().atStartOfDay() : null;
        LocalDateTime end = request.endDate() != null ? request.endDate().atTime(23, 59, 59) : null;

        Page<Object[]> result = outboundRepository.searchOutbounds(
                request.storeId(),
                start,
                end,
                pageable
        );

        return result.map(row -> {
            Outbound ob = (Outbound) row[0];
            Integer itemCount = ((Number) row[1]).intValue();
            Integer totalQty = ((Number) row[2]).intValue();

            return OutboundResponseDto.builder()
                    .outboundId(ob.getOutboundId())
                    .outboundNo(ob.getOutboundNo())
                    .outboundDatetime(ob.getOutboundDatetime())
                    .storeName(ob.getStore().getStoreName())
                    .itemCount(itemCount)
                    .totalShippedQty(totalQty)
                    .build();
        });
    }

    // 수정
    private String generateOutboundNo(StoreOrder storeOrder) {
        return "OUT-" + storeOrder.getStoreOrderId() + "-" + System.currentTimeMillis();
    }

}
