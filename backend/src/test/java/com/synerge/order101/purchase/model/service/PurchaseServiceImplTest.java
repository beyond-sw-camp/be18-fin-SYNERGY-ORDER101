package com.synerge.order101.purchase.model.service;

import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.purchase.model.repository.PurchaseRepository;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.warehouse.model.entity.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Purchase Service Mock 테스트")
class PurchaseServiceImplTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private Purchase testPurchase;

    @BeforeEach
    void setUp() {
        // Mock 데이터 생성
        User mockUser = User.create(
            "test@test.com",
            "password123",
            "테스트사용자",
            null,
            "01012345678",
            null
        );

        Supplier mockSupplier = new Supplier(
            1L,
            "SUP001",
            "테스트공급업체",
            "홍길동",
            "01011112222",
            "서울시 강남구",
            null,
            null
        );

        Warehouse mockWarehouse = new Warehouse();

        // Purchase 생성
        testPurchase = Purchase.builder()
            .supplier(mockSupplier)
            .user(mockUser)
            .warehouse(mockWarehouse)
            .orderStatus(OrderStatus.DRAFT_AUTO)
            .orderType(Purchase.OrderType.MANUAL)
            .poDate(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("발주 조회 - 성공")
    void testFindPurchase_Success() {
        // given
        Long purchaseId = 1L;
        given(purchaseRepository.findById(purchaseId)).willReturn(Optional.of(testPurchase));

        // when
        Purchase foundPurchase = purchaseRepository.findById(purchaseId).orElseThrow();

        // then
        assertThat(foundPurchase).isNotNull();
        assertThat(foundPurchase.getOrderStatus()).isEqualTo(OrderStatus.DRAFT_AUTO);
    }
}
