package com.synerge.order101.outbound.model.service;

import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.order.model.entity.StoreOrderDetail;
import com.synerge.order101.order.model.repository.StoreOrderRepository;
import com.synerge.order101.outbound.model.dto.OutboundDetailResponseDto;
import com.synerge.order101.outbound.model.dto.OutboundResponseDto;
import com.synerge.order101.outbound.model.dto.OutboundSearchRequestDto;
import com.synerge.order101.outbound.model.entity.Outbound;
import com.synerge.order101.outbound.model.entity.OutboundDetail;
import com.synerge.order101.outbound.model.repository.OutboundDetailRepository;
import com.synerge.order101.outbound.model.repository.OutboundRepository;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import com.synerge.order101.warehouse.model.service.InventoryService;
import com.synerge.order101.product.model.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("OutboundServiceImplTest")
@ExtendWith(MockitoExtension.class)
class OutboundServiceImplTest {

    @InjectMocks
    private OutboundServiceImpl outboundService;

    @Mock
    private OutboundRepository outboundRepository;
    @Mock
    private OutboundDetailRepository outboundDetailRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private StoreOrderRepository storeOrderRepository;

    @Test
    @DisplayName("출고 생성 및 재고 차감 테스트")
    void createOutbound_Success() {
        // given
        // Mock Data: Store, Warehouse, Product
        Store mockStore = Store.builder().storeId(1L).storeName("강남점").build();
        Warehouse mockWarehouse = Warehouse.builder().warehouseId(1L).warehouseName("중앙물류센터").build();
        Product mockProduct = Product.builder().productId(10L).productCode("P001").productName("콜라").build();

        // Mock Data: StoreOrder & Detail
        StoreOrderDetail orderDetail = StoreOrderDetail.builder()
                .product(mockProduct)
                .orderQty(50)
                .build();

        StoreOrder mockOrder = StoreOrder.builder()
                .storeOrderId(100L)
                .store(mockStore)
                .warehouse(mockWarehouse)
                .storeOrderDetails(List.of(orderDetail))
                .build();

        // when
        outboundService.createOutbound(mockOrder);

        // then
        // 1. 출고(Outbound)가 저장되었는지 검증
        verify(outboundRepository, times(1)).save(any(Outbound.class));

        // 2. 출고 상세(OutboundDetail)가 저장되었는지 검증
        verify(outboundDetailRepository, times(1)).save(any());

        // 3. 재고 서비스(InventoryService)가 호출되어 재고가 차감되었는지 검증
        verify(inventoryService, times(1)).decreaseInventory(mockProduct.getProductId(), 50);

        // 4. (Console 확인용) 실제 테스트 실행 시 SLF4J 로그가 콘솔에 찍히는지 확인
        System.out.println(">>> 테스트 성공: createOutbound 로직이 정상 수행되었습니다.");
    }

    @Test
    @DisplayName("출고 목록 조회 테스트 - 정상 케이스")
    void getOutboundList_Success() {
        // given
        int page = 1;
        int size = 10;
        Pageable pageable = PageRequest.of(0, size);

        // Mock Data for Page<Object[]>
        // QueryDSL이나 JPQL Projection 결과는 보통 Object[] 형태입니다.
        // 순서: [Outbound 엔티티, itemCount, totalQty]
        Outbound mockOutbound = Outbound.builder()
                .outboundId(1L)
                .outboundNo("OUT-1234")
                .outboundDatetime(LocalDateTime.now())
                .store(Store.builder().storeName("홍대점").build())
                .build();

        Object[] row = {mockOutbound, 5, 100}; // Outbound, itemCount, totalQty
        Page<Object[]> mockPage = new PageImpl<>(Collections.singletonList(row), pageable, 1);

        given(outboundRepository.findOutboundWithCounts(any(Pageable.class))).willReturn(mockPage);

        // when
        Page<OutboundResponseDto> result = outboundService.getOutboundList(page, size);

        // then
        assertThat(result.getContent()).hasSize(1);
        OutboundResponseDto dto = result.getContent().get(0);

        assertThat(dto.getOutboundNo()).isEqualTo("OUT-1234");
        assertThat(dto.getStoreName()).isEqualTo("홍대점");
        assertThat(dto.getItemCount()).isEqualTo(5);
        assertThat(dto.getTotalShippedQty()).isEqualTo(100);
    }

    @Test
    @DisplayName("출고 상세 조회 성공: 출고 정보와 상품 상세 목록이 올바르게 반환된다")
    void getOutboundDetail_Success() {
        // given
        Long outboundId = 1L;
        String outboundNo = "OUT-TEST-001";

        // 1. Mock Outbound (출고 헤더)
        Outbound mockOutbound = Outbound.builder()
                .outboundId(outboundId)
                .outboundNo(outboundNo)
                .build();

        // 2. Mock Product & Detail (출고 상세 상품)
        Product mockProduct1 = Product.builder().productCode("P001").productName("사과").build();
        Product mockProduct2 = Product.builder().productCode("P002").productName("배").build();

        // OutboundDetail은 엔티티 내부 구조에 따라 생성자나 빌더 사용
        // 여기서는 Mock 객체로 가정하여 getter 호출 시 동작을 정의합니다.
        OutboundDetail detail1 = mock(OutboundDetail.class);
        given(detail1.getProduct()).willReturn(mockProduct1);
        given(detail1.getOutboundQty()).willReturn(10);

        OutboundDetail detail2 = mock(OutboundDetail.class);
        given(detail2.getProduct()).willReturn(mockProduct2);
        given(detail2.getOutboundQty()).willReturn(5);

        List<OutboundDetail> mockDetails = List.of(detail1, detail2);

        // 3. Stubbing Repository
        given(outboundRepository.findById(outboundId)).willReturn(Optional.of(mockOutbound));
        given(outboundDetailRepository.findByOutbound(outboundId)).willReturn(mockDetails);

        // when
        OutboundDetailResponseDto result = outboundService.getOutboundDetail(outboundId);

        // then
        // 헤더 정보 검증
        assertThat(result.getOutboundNo()).isEqualTo(outboundNo);

        // 상세 리스트 검증
        assertThat(result.getItems()).hasSize(2);

        // 첫 번째 아이템(사과) 검증
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("사과");
        assertThat(result.getItems().get(0).getShippedQty()).isEqualTo(10);

        // 두 번째 아이템(배) 검증
        assertThat(result.getItems().get(1).getProductName()).isEqualTo("배");
        assertThat(result.getItems().get(1).getShippedQty()).isEqualTo(5);
    }

    @Test
    @DisplayName("출고 상세 조회 실패: 존재하지 않는 출고 ID 조회 시 예외가 발생한다")
    void getOutboundDetail_NotFound() {
        // given
        Long invalidId = 999L;

        // Stubbing: findById가 빈 Optional을 반환하도록 설정
        given(outboundRepository.findById(invalidId)).willReturn(Optional.empty());

        // when & then
        // 예외 타입과 메시지 검증
        assertThatThrownBy(() -> outboundService.getOutboundDetail(invalidId))
                .isInstanceOf(RuntimeException.class) // 서비스 코드에서 RuntimeException 던짐
                .hasMessage("출고 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("출고 검색 테스트 - 날짜 및 조건")
    void searchOutboundList_Success() {
        // given
        OutboundSearchRequestDto request = new OutboundSearchRequestDto(
                1L, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), 1, 10
        );

        // Mock Data
        Outbound mockOutbound = Outbound.builder()
                .outboundId(2L)
                .outboundNo("OUT-9999")
                .outboundDatetime(LocalDateTime.now())
                .store(Store.builder().storeName("부산점").build())
                .build();

        Object[] row = {mockOutbound, 3, 30};
        Page<Object[]> mockPage = new PageImpl<>(Collections.singletonList(row));

        given(outboundRepository.searchOutbounds(any(), any(), any(), any())).willReturn(mockPage);

        // when
        Page<OutboundResponseDto> result = outboundService.searchOutboundList(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStoreName()).isEqualTo("부산점");

        verify(outboundRepository).searchOutbounds(any(), any(), any(), any());
    }
}