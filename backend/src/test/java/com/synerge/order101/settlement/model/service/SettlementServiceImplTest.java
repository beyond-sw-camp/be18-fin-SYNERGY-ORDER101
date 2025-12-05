package com.synerge.order101.settlement.model.service;

import com.synerge.order101.common.dto.TradeSearchCondition;
import com.synerge.order101.common.enums.SettlementType;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.settlement.event.SettlementReqEvent;
import com.synerge.order101.settlement.model.dto.SettlementSummaryDto;
import com.synerge.order101.settlement.model.entity.Settlement;
import com.synerge.order101.settlement.model.repository.SettlementRepository;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.supplier.model.entity.Supplier;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementServiceImplTest")
class SettlementServiceImplTest {

    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @Test
    @DisplayName("정산 목록 조회 - 성공")
    void getSettlements_Success() {
        // given
        TradeSearchCondition cond = new TradeSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);

        Settlement settlement1 = Settlement.builder()
                .settlementId(1L)
                .settlementNo("SETL-20231201-1111")
                .settlementType(SettlementType.AP)
                .settlementStatus(Settlement.SettlementStatus.DRAFT)
                .productsAmount(new BigDecimal("100000"))
                .productsQty(50)
                .createdAt(LocalDateTime.now())
                .build();

        Settlement settlement2 = Settlement.builder()
                .settlementId(2L)
                .settlementNo("SETL-20231202-2222")
                .settlementType(SettlementType.AR)
                .settlementStatus(Settlement.SettlementStatus.ISSUED)
                .productsAmount(new BigDecimal("200000"))
                .productsQty(100)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Settlement> settlementPage = new PageImpl<>(List.of(settlement1, settlement2));
        given(settlementRepository.search(cond, pageable)).willReturn(settlementPage);

        // when
        Page<SettlementSummaryDto> result = settlementService.getSettlements(cond, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(settlementRepository, times(1)).search(cond, pageable);
    }

    @Test
    @DisplayName("정산 목록 조회 - 빈 결과")
    void getSettlements_EmptyResult() {
        // given
        TradeSearchCondition cond = new TradeSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Settlement> emptyPage = new PageImpl<>(List.of());

        given(settlementRepository.search(cond, pageable)).willReturn(emptyPage);

        // when
        Page<SettlementSummaryDto> result = settlementService.getSettlements(cond, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(settlementRepository, times(1)).search(cond, pageable);
    }

    @Test
    @DisplayName("정산 이벤트 처리 - 공급사 정산(AP) 성공")
    void handleSettlementRequired_SupplierAP_Success() {
        // given
        Supplier supplier = Supplier.builder()
                .supplierId(1L)
                .supplierName("테스트공급사")
                .build();

        Purchase purchase = Purchase.builder()
                .purchaseId(100L)
                .build();

        SettlementReqEvent event = new SettlementReqEvent() {
            @Override
            public boolean existSettlement(SettlementRepository repo) {
                return false; // 정산이 존재하지 않음
            }

            @Override
            public Purchase purchase() {
                return purchase;
            }

            @Override
            public Supplier supplier() {
                return supplier;
            }

            @Override
            public Store store() {
                return null;
            }

            @Override
            public StoreOrder storeOrder() {
                return null;
            }

            @Override
            public SettlementType settlementType() {
                return SettlementType.AP;
            }

            @Override
            public BigDecimal settlementAmount() {
                return new BigDecimal("500000");
            }

            @Override
            public Integer settlementQty() {
                return 100;
            }
        };

        given(settlementRepository.save(any(Settlement.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        settlementService.handleSettlementRequired(event);

        // then
        verify(settlementRepository, times(1)).save(any(Settlement.class));
    }

    @Test
    @DisplayName("정산 이벤트 처리 - 가맹점 정산(AR) 성공")
    void handleSettlementRequired_StoreAR_Success() {
        // given
        Store store = Store.builder()
                .storeId(1L)
                .storeName("테스트매장")
                .build();

        StoreOrder storeOrder = StoreOrder.builder()
                .storeOrderId(200L)
                .build();

        SettlementReqEvent event = new SettlementReqEvent() {
            @Override
            public boolean existSettlement(SettlementRepository repo) {
                return false;
            }

            @Override
            public Purchase purchase() {
                return null;
            }

            @Override
            public Supplier supplier() {
                return null;
            }

            @Override
            public Store store() {
                return store;
            }

            @Override
            public StoreOrder storeOrder() {
                return storeOrder;
            }

            @Override
            public SettlementType settlementType() {
                return SettlementType.AR;
            }

            @Override
            public BigDecimal settlementAmount() {
                return new BigDecimal("300000");
            }

            @Override
            public Integer settlementQty() {
                return 50;
            }
        };

        given(settlementRepository.save(any(Settlement.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        settlementService.handleSettlementRequired(event);

        // then
        verify(settlementRepository, times(1)).save(any(Settlement.class));
    }

    @Test
    @DisplayName("정산 이벤트 처리 - 이미 정산이 존재하면 예외 발생")
    void handleSettlementRequired_AlreadyExists_ThrowsException() {
        // given
        SettlementReqEvent event = new SettlementReqEvent() {
            @Override
            public boolean existSettlement(SettlementRepository repo) {
                return true; // 이미 정산이 존재함
            }

            @Override
            public Purchase purchase() {
                return null;
            }

            @Override
            public Supplier supplier() {
                return null;
            }

            @Override
            public Store store() {
                return null;
            }

            @Override
            public StoreOrder storeOrder() {
                return null;
            }

            @Override
            public SettlementType settlementType() {
                return SettlementType.AP;
            }

            @Override
            public BigDecimal settlementAmount() {
                return new BigDecimal("100000");
            }

            @Override
            public Integer settlementQty() {
                return 10;
            }
        };

        // when & then
        assertThatThrownBy(() -> settlementService.handleSettlementRequired(event))
                .isInstanceOf(CustomException.class);

        verify(settlementRepository, never()).save(any(Settlement.class));
    }

    @Test
    @DisplayName("정산 이벤트 처리 - Repository 저장 호출 검증")
    void handleSettlementRequired_VerifyRepositorySave() {
        // given
        SettlementReqEvent event = new SettlementReqEvent() {
            @Override
            public boolean existSettlement(SettlementRepository repo) {
                return false;
            }

            @Override
            public Purchase purchase() {
                return Purchase.builder().purchaseId(1L).build();
            }

            @Override
            public Supplier supplier() {
                return Supplier.builder().supplierId(1L).build();
            }

            @Override
            public Store store() {
                return null;
            }

            @Override
            public StoreOrder storeOrder() {
                return null;
            }

            @Override
            public SettlementType settlementType() {
                return SettlementType.AP;
            }

            @Override
            public BigDecimal settlementAmount() {
                return new BigDecimal("999999");
            }

            @Override
            public Integer settlementQty() {
                return 888;
            }
        };

        // when
        settlementService.handleSettlementRequired(event);

        // then
        verify(settlementRepository).save(argThat(settlement ->
                settlement.getSettlementType() == SettlementType.AP &&
                settlement.getProductsAmount().compareTo(new BigDecimal("999999")) == 0 &&
                settlement.getProductsQty() == 888 &&
                settlement.getSupplier() != null &&
                settlement.getPurchase() != null
        ));
    }

    @Test
    @DisplayName("정산 목록 조회 - 페이징 처리 검증")
    void getSettlements_PagingVerification() {
        // given
        TradeSearchCondition cond = new TradeSearchCondition();
        Pageable pageable = PageRequest.of(2, 20); // 3페이지, 20개씩

        Settlement settlement = Settlement.builder()
                .settlementId(1L)
                .settlementType(SettlementType.AP)
                .settlementStatus(Settlement.SettlementStatus.DRAFT)
                .productsAmount(new BigDecimal("100000"))
                .createdAt(LocalDateTime.now())
                .build();

        Page<Settlement> settlementPage = new PageImpl<>(List.of(settlement), pageable, 100);
        given(settlementRepository.search(cond, pageable)).willReturn(settlementPage);

        // when
        Page<SettlementSummaryDto> result = settlementService.getSettlements(cond, pageable);

        // then
        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(100);
        verify(settlementRepository, times(1)).search(cond, pageable);
    }

    @Test
    @DisplayName("정산 이벤트 처리 - 금액 0원 정산")
    void handleSettlementRequired_ZeroAmount() {
        // given
        SettlementReqEvent event = new SettlementReqEvent() {
            @Override
            public boolean existSettlement(SettlementRepository repo) {
                return false;
            }

            @Override
            public Purchase purchase() {
                return Purchase.builder().purchaseId(1L).build();
            }

            @Override
            public Supplier supplier() {
                return Supplier.builder().supplierId(1L).build();
            }

            @Override
            public Store store() {
                return null;
            }

            @Override
            public StoreOrder storeOrder() {
                return null;
            }

            @Override
            public SettlementType settlementType() {
                return SettlementType.AP;
            }

            @Override
            public BigDecimal settlementAmount() {
                return BigDecimal.ZERO;
            }

            @Override
            public Integer settlementQty() {
                return 0;
            }
        };

        given(settlementRepository.save(any(Settlement.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        settlementService.handleSettlementRequired(event);

        // then
        verify(settlementRepository, times(1)).save(any(Settlement.class));
    }
}
