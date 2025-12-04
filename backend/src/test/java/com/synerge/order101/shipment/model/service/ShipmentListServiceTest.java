package com.synerge.order101.shipment.model.service;


import com.synerge.order101.common.enums.ShipmentStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.shipment.model.dto.response.ShipmentResponseDto;
import com.synerge.order101.shipment.model.repository.ShipmentListRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("ShipmentListServiceTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class ShipmentListServiceTest {

    @InjectMocks
    private ShipmentListService shipmentListService;

    @Mock
    private ShipmentListRepository shipmentListRepository;

    @Test @Order(1)
    void GetShipmentListSuccess() {
        // given
        var dto = new ShipmentResponseDto(1L, "ORDER-1", "가맹점A", "본사 창고", BigDecimal.valueOf(10),ShipmentStatus.WAITING, LocalDateTime.now());
        var page = new PageImpl<>(List.of(dto), PageRequest.of(0,20), 1);
        given(shipmentListRepository.findPage(any(), any(), any(), any(), any(), any())).willReturn(page);

        // when
        var result = shipmentListService.findShipments(null, 1L, null, null, null, PageRequest.of(0,20));

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent().get(0).getOrderNo()).isEqualTo("ORDER-1");
        verify(shipmentListRepository, times(1)).findPage(any(), any(), any(), any(), any(), any());
    }

    @Test
    @Order(2)
    void GetShipmentListNoneException() {
        // given
        given(shipmentListRepository.findPage(any(), any(), any(), any(), any(), any()))
                .willReturn(Page.empty());

        // when
        Throwable thrown = catchThrowable(() ->
                shipmentListService.findShipments(null, 1L, null, null, null, PageRequest.of(0, 20)));

        // then
        assertThat(thrown)
                .isInstanceOf(CustomException.class)
                .extracting(t -> ((CustomException) t).getErrorCode().getCode())
                .isEqualTo("SHIPMENT_NOT_FOUND");

        assertThat(thrown).hasMessage("해당 배송 정보를 찾을 수 없습니다.");
    }
}
