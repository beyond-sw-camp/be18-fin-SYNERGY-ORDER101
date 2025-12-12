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
                    o.getStore() != null ? o.getStore().getStoreName() : "삭제된 매장",
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
    public void createOutbound(StoreOrder storeOrder) {
        // 출고 생성
        Outbound outbound = Outbound.create(
                storeOrder.getWarehouse(),
                storeOrder.getStore(),
                generateOutboundNo(storeOrder),
                "SYSTEM"
        );
        outboundRepository.save(outbound);

        // 출고 상세 생성 및 재고 차감
        storeOrder.getStoreOrderDetails().forEach(detail -> {
            // 출고 상세 저장
            OutboundDetail outboundDetail = new OutboundDetail(outbound, detail.getProduct(), detail.getOrderQty());
            outboundDetailRepository.save(outboundDetail);

            // [중요] 재고 차감 (여기서 재고 부족 시 예외 발생하여 트랜잭션 롤백됨)
            inventoryService.decreaseInventory(detail.getProduct().getProductId(), detail.getOrderQty());
        });
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
                    .storeName(ob.getStore() != null ? ob.getStore().getStoreName() : "삭제된 매장")
                    .itemCount(itemCount)
                    .totalShippedQty(totalQty)
                    .build();
        });
    }

    private String generateOutboundNo(StoreOrder storeOrder) {
        return "OUT-" + storeOrder.getStoreOrderId() + "-" + System.currentTimeMillis();
    }

}
