package com.synerge.order101.inbound.model.service;

import com.synerge.order101.inbound.model.dto.InboundDetailResponseDto;
import com.synerge.order101.inbound.model.dto.InboundResponseDto;
import com.synerge.order101.inbound.model.dto.InboundSearchRequestDto;
import com.synerge.order101.inbound.model.entity.Inbound;
import com.synerge.order101.inbound.model.entity.InboundDetail;
import com.synerge.order101.inbound.model.repository.InboundDetailRepository;
import com.synerge.order101.inbound.model.repository.InboundRepository;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.warehouse.model.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InboundServiceImpl implements InboundService {
    private final InboundRepository inboundRepository;
    private final InboundDetailRepository inboundDetailRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public Page<InboundResponseDto> getInboundList(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Object[]> result = inboundRepository.findInboundWithCounts(pageable);

        return result.map(row -> {
            Inbound i = (Inbound) row[0];
            Integer itemCount = ((Number) row[1]).intValue();
            Integer totalQty = ((Number) row[2]).intValue();

            return new InboundResponseDto(
                    i.getInboundId(),
                    i.getInboundNo(),
                    i.getInboundDatetime(),
                    i.getSupplier().getSupplierName(),
                    itemCount,
                    totalQty
            );
        });
    }

    @Override
    @Transactional
    public InboundDetailResponseDto getInboundDetail(Long inboundId) {
        Inbound inbound = inboundRepository.findById(inboundId)
                .orElseThrow(() -> new RuntimeException("입고 정보를 찾을 수 없습니다."));

        List<InboundDetail> details = inboundDetailRepository.findByInbound(inboundId);

        List<InboundDetailResponseDto.Item> items = details.stream()
                .map(d -> InboundDetailResponseDto.Item.builder()
                        .productCode(d.getProduct().getProductCode())
                        .productName(d.getProduct().getProductName())
                        .receivedQty(d.getReceivedQty())
                        .build())
                .toList();

        return InboundDetailResponseDto.builder()
                .inboundNo(inbound.getInboundNo())
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public Page<InboundResponseDto> searchInboundList(InboundSearchRequestDto request) {

        int page = request.page();
        int numOfRows = request.numOfRows();

        Pageable pageable = PageRequest.of(page - 1, numOfRows, Sort.by("inboundId").descending());

        LocalDateTime start = request.startDate() != null ? request.startDate().atStartOfDay() : null;
        LocalDateTime end = request.endDate() != null ? request.endDate().atTime(23, 59, 59) : null;

        Page<Object[]> result = inboundRepository.searchInbounds(
                request.supplierId(),
                start,
                end,
                pageable
        );

        return result.map(row -> {
            Inbound inbound = (Inbound) row[0];
            Integer itemCount = ((Number) row[1]).intValue();
            Integer totalQty = ((Number) row[2]).intValue();

            return InboundResponseDto.builder()
                    .inboundId(inbound.getInboundId())
                    .inboundNo(inbound.getInboundNo())
                    .inboundDatetime(inbound.getInboundDatetime())
                    .supplierName(inbound.getSupplier().getSupplierName())
                    .itemCount(itemCount)
                    .totalReceivedQty(totalQty)
                    .build();
        });
    }

    @Override
    @Transactional
    public void createInbound(Purchase purchase) {
        // 1. 입고(Inbound) 엔티티 생성 및 저장
        Inbound inbound = Inbound.builder()
                .warehouse(purchase.getWarehouse())
                .supplier(purchase.getSupplier())
                .inboundNo(generateInboundNo(purchase))
                .build();
        inboundRepository.save(inbound);

        // 2. 입고 상세(InboundDetail) 생성 및 재고 증가
        purchase.getPurchaseDetails().forEach(pd -> {
            // 입고 상세 저장
            InboundDetail inboundDetail = InboundDetail.builder()
                    .inbound(inbound)
                    .product(pd.getProduct())
                    .receivedQty(pd.getOrderQty())
                    .build();
            inboundDetailRepository.save(inboundDetail);

            // [중요] 재고 증가 처리 (InventoryService 호출)
            inventoryService.increaseInventory(pd.getProduct().getProductId(), pd.getOrderQty());
        });
    }

    private String generateInboundNo(Purchase purchase) {
        return "IN-" + purchase.getPurchaseId() + System.currentTimeMillis();
    }
}
