package com.synerge.order101.inbound.model.service;

import com.synerge.order101.inbound.model.dto.InboundDetailResponseDto;
import com.synerge.order101.inbound.model.dto.InboundResponseDto;
import com.synerge.order101.inbound.model.dto.InboundSearchRequestDto;
import com.synerge.order101.inbound.model.entity.Inbound;
import com.synerge.order101.inbound.model.entity.InboundDetail;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.inbound.model.repository.InboundDetailRepository;
import com.synerge.order101.inbound.model.repository.InboundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("InboundServiceImplTest")
@ExtendWith(MockitoExtension.class)
class InboundServiceImplTest {

    private static final Logger log = LoggerFactory.getLogger(InboundServiceImplTest.class);

    @InjectMocks
    private InboundServiceImpl inboundService;

    @Mock
    private InboundRepository inboundRepository;

    @Mock
    private InboundDetailRepository inboundDetailRepository;

    @Test
    @DisplayName("입고 목록 조회 테스트 - 정상 케이스")
    void getInboundListTest() {
        log.info("============== [TEST START] getInboundListTest ==============");

        // given
        int page = 1;
        int size = 10;
        Pageable pageable = PageRequest.of(0, size);

        // Mock 데이터 생성
        Supplier supplier = new Supplier();
        // Setter가 없으면 Builder 사용 등 상황에 맞게 조정 필요
        setPrivateField(supplier, "supplierName", "테스트 공급사");

        Inbound inbound = new Inbound();
        setPrivateField(inbound, "inboundId", 1L);
        setPrivateField(inbound, "inboundNo", "INB-20231201-001");
        setPrivateField(inbound, "inboundDatetime", LocalDateTime.now());
        setPrivateField(inbound, "supplier", supplier);

        // Repository 반환값: Object[] { Inbound, itemCount, totalQty }
        Object[] row = { inbound, 3, 100 };
        List<Object[]> content = Collections.singletonList(row);
        Page<Object[]> pageResult = new PageImpl<>(content, pageable, 1);

        when(inboundRepository.findInboundWithCounts(any(Pageable.class))).thenReturn(pageResult);

        // when
        log.info("Calling inboundService.getInboundList(page={}, size={})", page, size);
        Page<InboundResponseDto> result = inboundService.getInboundList(page, size);

        // then
        log.info("Result Size: {}", result.getTotalElements());
        log.info("First Item Supplier: {}", result.getContent().get(0).getSupplierName());

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("테스트 공급사", result.getContent().get(0).getSupplierName());
        assertEquals(3, result.getContent().get(0).getItemCount());
        assertEquals(100, result.getContent().get(0).getTotalReceivedQty());

        verify(inboundRepository, times(1)).findInboundWithCounts(any(Pageable.class));

        log.info("============== [TEST END] getInboundListTest ==============");
    }

    @Test
    @DisplayName("입고 상세 조회 테스트 - 정상 케이스")
    void getInboundDetailTest() {
        log.info("============== [TEST START] getInboundDetailTest ==============");

        // given
        Long inboundId = 1L;

        // Inbound Mock
        Supplier supplier = new Supplier();
        Inbound inbound = new Inbound();
        setPrivateField(inbound, "inboundId", inboundId);
        setPrivateField(inbound, "inboundNo", "INB-DETAIL-TEST");
        setPrivateField(inbound, "supplier", supplier);

        // Detail Mock
        Product product = new Product();
        setPrivateField(product, "productCode", "P001");
        setPrivateField(product, "productName", "테스트 상품");

        InboundDetail detail = new InboundDetail();
        setPrivateField(detail, "product", product);
        setPrivateField(detail, "receivedQty", 50);

        when(inboundRepository.findById(inboundId)).thenReturn(Optional.of(inbound));
        when(inboundDetailRepository.findByInbound(inboundId)).thenReturn(List.of(detail));

        // when
        log.info("Calling inboundService.getInboundDetail(inboundId={})", inboundId);
        InboundDetailResponseDto result = inboundService.getInboundDetail(inboundId);

        // then
        log.info("Result InboundNo: {}", result.getInboundNo());
        log.info("Result Item Count: {}", result.getItems().size());
        log.info("First Item Name: {}", result.getItems().get(0).getProductName());

        assertNotNull(result);
        assertEquals("INB-DETAIL-TEST", result.getInboundNo());
        assertEquals(1, result.getItems().size());
        assertEquals("테스트 상품", result.getItems().get(0).getProductName());
        assertEquals(50, result.getItems().get(0).getReceivedQty());

        log.info("============== [TEST END] getInboundDetailTest ==============");
    }

    @Test
    @DisplayName("입고 상세 조회 테스트 - 실패(존재하지 않는 ID)")
    void getInboundDetailNotFoundTest() {
        log.info("============== [TEST START] getInboundDetailNotFoundTest ==============");

        // given
        Long invalidId = 999L;
        when(inboundRepository.findById(invalidId)).thenReturn(Optional.empty());

        // when & then
        log.info("Expecting RuntimeException for ID: {}", invalidId);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inboundService.getInboundDetail(invalidId);
        });

        log.info("Exception Message: {}", exception.getMessage());
        assertEquals("입고 정보를 찾을 수 없습니다.", exception.getMessage());

        log.info("============== [TEST END] getInboundDetailNotFoundTest ==============");
    }

    @Test
    @DisplayName("입고 검색 테스트 - 날짜 및 조건")
    void searchInboundListTest() {
        log.info("============== [TEST START] searchInboundListTest ==============");

        // given
        InboundSearchRequestDto request = new InboundSearchRequestDto(
                100L, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), null, 1, 10);

        Supplier supplier = new Supplier();
        setPrivateField(supplier, "supplierName", "검색된 공급사");

        Inbound inbound = new Inbound();
        setPrivateField(inbound, "inboundId", 5L);
        setPrivateField(inbound, "inboundNo", "INB-SEARCH-001");
        setPrivateField(inbound, "inboundDatetime", LocalDateTime.now());
        setPrivateField(inbound, "supplier", supplier);

        // Mock Page Result
        Object[] row = { inbound, 5, 200 };
        List<Object[]> content = Collections.singletonList(row);
        Page<Object[]> pageResult = new PageImpl<>(content);

        when(inboundRepository.searchInbounds(
                eq(100L), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class))
        ).thenReturn(pageResult);

        // when
        log.info("Calling searchInboundList with request: {}", request);
        Page<InboundResponseDto> result = inboundService.searchInboundList(request);

        // then
        log.info("Search Result Count: {}", result.getTotalElements());

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("검색된 공급사", result.getContent().get(0).getSupplierName());
        assertEquals(200, result.getContent().get(0).getTotalReceivedQty());

        verify(inboundRepository).searchInbounds(any(), any(), any(), any());

        log.info("============== [TEST END] searchInboundListTest ==============");
    }

    // 리플렉션을 사용하여 Private 필드 값을 설정하는 유틸리티 메서드
    // (엔티티에 Setter나 Builder가 없다고 가정했을 때 테스트 용이성을 위함)
    private void setPrivateField(Object object, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 부모 클래스에 필드가 있는 경우 등을 대비해 간단히 처리하거나 무시
            try {
                // 상속받은 필드일 경우 슈퍼클래스 탐색 (간단 구현)
                java.lang.reflect.Field field = object.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(object, value);
            } catch (Exception ex) {
                log.warn("Failed to set field via reflection: " + fieldName);
            }
        }
    }
}