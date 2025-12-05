package com.synerge.order101.smartorder.model.service;

import com.synerge.order101.ai.exception.AiErrorCode;
import com.synerge.order101.ai.model.dto.request.SmartOrderUpdateRequestDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderDashboardResponseDto;
import com.synerge.order101.ai.model.dto.response.SmartOrderResponseDto;
import com.synerge.order101.ai.model.entity.DemandForecast;
import com.synerge.order101.ai.model.entity.SmartOrder;
import com.synerge.order101.ai.model.repository.DemandForecastRepository;
import com.synerge.order101.ai.model.repository.SmartOrderRepository;
import com.synerge.order101.ai.model.service.SmartOrderServiceImpl;
import com.synerge.order101.common.enums.OrderStatus;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.notification.model.service.NotificationService;
import com.synerge.order101.product.model.entity.Product;
import com.synerge.order101.product.model.entity.ProductSupplier;
import com.synerge.order101.product.model.repository.ProductSupplierRepository;
import com.synerge.order101.purchase.model.repository.PurchaseDetailRepository;
import com.synerge.order101.supplier.model.entity.Supplier;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import com.synerge.order101.warehouse.model.entity.WarehouseInventory;
import com.synerge.order101.warehouse.model.repository.WarehouseInventoryRepository;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
@DisplayName("SmartOrderServiceImplTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SmartOrderServiceImplTest {
    @InjectMocks
    private SmartOrderServiceImpl smartOrderService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SmartOrderRepository smartOrderRepository;

    @Mock
    private DemandForecastRepository demandForecastRepository;

    @Mock
    private ProductSupplierRepository productSupplierRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WarehouseInventoryRepository warehouseInventoryRepository;

    @Mock
    private PurchaseDetailRepository purchaseDetailRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Order(1)
    @Epic("SmartOrder")
    @Feature("Generate")
    @Story("정상 케이스 - 재고/리드타임/안전재고 반영")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generateSmartOrders - 정상 케이스")
    //정상 케이스 (재고/리드타임/안전재고 반영)
    void generateSmartOrders_success() {
        // given
        LocalDate targetWeek = LocalDate.of(2025, 1, 6);

        // 이미 생성된 스마트 발주 없음
        given(smartOrderRepository.findByTargetWeek(targetWeek))
                .willReturn(List.of());

        // SYSTEM 유저
        User systemUser = mock(User.class);
        given(userRepository.findByRole(Role.SYSTEM))
                .willReturn(List.of(systemUser));

        // 수요예측 1건
        Product product = mock(Product.class);
        given(product.getProductId()).willReturn(1000L);

        DemandForecast df = DemandForecast.builder()
                .demandForecastId(1L)
                .product(product)
                .targetWeek(targetWeek)
                .yPred(100)
                .build();

        given(demandForecastRepository.findByTargetWeek(targetWeek))
                .willReturn(List.of(df));

        // Supplier mock
        Supplier supplier = mock(Supplier.class);
        given(supplier.getSupplierId()).willReturn(100L);


        // 상품-공급사 매핑
        ProductSupplier mapping = mock(ProductSupplier.class);
        given(mapping.getLeadTimeDays()).willReturn(7);
        given(mapping.getSupplier()).willReturn(supplier);
        given(productSupplierRepository.findByProduct(product))
                .willReturn(Optional.of(mapping));

        given(productSupplierRepository.findTop1ByProduct(product))
                .willReturn(Optional.of(mapping));

        // 창고 재고 + 안전재고
        WarehouseInventory inv = mock(WarehouseInventory.class);
        given(inv.getOnHandQuantity()).willReturn(10);
        given(inv.getSafetyQuantity()).willReturn(20);
        given(warehouseInventoryRepository.findByProduct(product))
                .willReturn(Optional.of(inv));

        // 입고 예정
        given(purchaseDetailRepository.sumOpenOrderQtyByProduct(product))
                .willReturn(5L);

        // save 호출 시 저장된 엔티티 그대로 리턴
        given(smartOrderRepository.save(any(SmartOrder.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = smartOrderService.generateSmartOrders(targetWeek);

        // then
        assertThat(result).hasSize(1);

        SmartOrderResponseDto dto = result.get(0);

        assertThat(dto.getForecastQty()).isEqualTo(100);
        assertThat(dto.getRecommendedOrderQty()).isEqualTo(205);
        assertThat(dto.getSmartOrderStatus()).isEqualTo(OrderStatus.DRAFT_AUTO);

        verify(smartOrderRepository, times(1)).save(any(SmartOrder.class));
    }


    @Test
    @Order(2)
    @Step("스마트발주 생성 호출")
    @DisplayName("generateSmartOrders")
    //이미 같은 주차 스마트발주 존재 시 예외
    void generateSmartOrders_alreadyExists() {
        // given
        LocalDate targetWeek = LocalDate.of(2025, 1, 6);
        given(smartOrderRepository.findByTargetWeek(targetWeek))
                .willReturn(List.of(mock(SmartOrder.class)));

        // when
        Throwable thrown = catchThrowable(() ->
                smartOrderService.generateSmartOrders(targetWeek));

        // then
        assertThat(thrown)
                .isInstanceOf(CustomException.class)
                .extracting(t -> ((CustomException) t).getErrorCode())
                .isEqualTo(AiErrorCode.SMART_ORDER_ALREADY_EXISTS);
    }

    @Test
    @Order(3)
    @DisplayName("generateSmartOrders")
    //수요예측이 없으면 예외
    void generateSmartOrders_noForecast() {
        // given
        LocalDate targetWeek = LocalDate.of(2025, 1, 6);

        given(smartOrderRepository.findByTargetWeek(targetWeek))
                .willReturn(List.of());

        given(userRepository.findByRole(Role.SYSTEM))
                .willReturn(List.of(mock(User.class)));

        given(demandForecastRepository.findByTargetWeek(targetWeek))
                .willReturn(List.of());

        // when
        Throwable thrown = catchThrowable(() ->
                smartOrderService.generateSmartOrders(targetWeek));

        // then
        assertThat(thrown)
                .isInstanceOf(CustomException.class)
                .extracting(t -> ((CustomException) t).getErrorCode())
                .isEqualTo(AiErrorCode.FORECAST_NOT_FOUND);
    }

    @Test
    @Order(4)
    @DisplayName("submitSmartOrder")
    //DRAFT_AUTO 상태에서 수정 + 제출 성공
    void submitSmartOrder_success() {
        // given
        Long smartOrderId = 1L;

        SmartOrder entity = SmartOrder.builder()
                .smartOrderId(smartOrderId)
                .supplier(mock(com.synerge.order101.supplier.model.entity.Supplier.class))
                .product(mock(Product.class))
                .demandForecast(mock(DemandForecast.class))
                .targetWeek(LocalDate.now())
                .forecastQty(100)
                .recommendedOrderQty(100)
                .smartOrderStatus(OrderStatus.DRAFT_AUTO)
                .build();

        given(smartOrderRepository.findById(smartOrderId))
                .willReturn(Optional.of(entity));

        // request DTO는 mock 으로 recommendedOrderQty만 세팅
        SmartOrderUpdateRequestDto request = mock(SmartOrderUpdateRequestDto.class);
        given(request.getRecommendedOrderQty()).willReturn(120);

        // SecurityContext에 로그인 유저 세팅
        User principalUser = mock(User.class);
        Authentication auth = mock(Authentication.class);
        SecurityContext context = mock(SecurityContext.class);

        given(auth.isAuthenticated()).willReturn(true);
        given(auth.getPrincipal()).willReturn(principalUser);
        given(context.getAuthentication()).willReturn(auth);
        SecurityContextHolder.setContext(context);

        // when
        SmartOrderResponseDto dto =
                smartOrderService.submitSmartOrder(smartOrderId, request);

        // then
        assertThat(dto.getRecommendedOrderQty()).isEqualTo(120);
        assertThat(dto.getSmartOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
    }

    @Test
    @Order(5)
    @DisplayName("submitSmartOrder")
    //DRAFT_AUTO가 아니면 제출 실패 예외
    void submitSmartOrder_notDraft() {
        // given
        Long smartOrderId = 1L;

        SmartOrder entity = SmartOrder.builder()
                .smartOrderId(smartOrderId)
                .supplier(mock(com.synerge.order101.supplier.model.entity.Supplier.class))
                .product(mock(Product.class))
                .demandForecast(mock(DemandForecast.class))
                .targetWeek(LocalDate.now())
                .forecastQty(100)
                .recommendedOrderQty(100)
                .smartOrderStatus(OrderStatus.SUBMITTED)
                .build();

        given(smartOrderRepository.findById(smartOrderId))
                .willReturn(Optional.of(entity));

        SmartOrderUpdateRequestDto request = mock(SmartOrderUpdateRequestDto.class);
        given(request.getRecommendedOrderQty()).willReturn(150);

        // when
        Throwable thrown = catchThrowable(() ->
                smartOrderService.submitSmartOrder(smartOrderId, request));

        // then
        assertThat(thrown)
                .isInstanceOf(CustomException.class)
                .extracting(t -> ((CustomException) t).getErrorCode())
                .isEqualTo(AiErrorCode.SMART_ORDER_UPDATE_FAILED);
    }

    @Test
    @Order(6)
    @DisplayName("getSmartOrderSummary")
    //합계/카운트 계산
    void getSmartOrderSummary_success() {
        // given
        LocalDate targetWeek = LocalDate.of(2025, 1, 6);

        SmartOrder so1 = SmartOrder.builder()
                .smartOrderId(1L)
                .supplier(mock(com.synerge.order101.supplier.model.entity.Supplier.class))
                .product(mock(Product.class))
                .demandForecast(mock(DemandForecast.class))
                .targetWeek(targetWeek)
                .forecastQty(100)
                .recommendedOrderQty(90)
                .smartOrderStatus(OrderStatus.DRAFT_AUTO)
                .build();

        SmartOrder so2 = SmartOrder.builder()
                .smartOrderId(2L)
                .supplier(mock(com.synerge.order101.supplier.model.entity.Supplier.class))
                .product(mock(Product.class))
                .demandForecast(mock(DemandForecast.class))
                .targetWeek(targetWeek)
                .forecastQty(50)
                .recommendedOrderQty(40)
                .smartOrderStatus(OrderStatus.SUBMITTED)
                .build();

        given(smartOrderRepository.findByTargetWeek(targetWeek))
                .willReturn(List.of(so1, so2));

        // when
        SmartOrderDashboardResponseDto summary =
                smartOrderService.getSmartOrderSummary(targetWeek);

        // then
        assertThat(summary.getTargetWeek()).isEqualTo(targetWeek);
        assertThat(summary.getTotalRecommendedQty()).isEqualTo(90 + 40);
        assertThat(summary.getTotalForecastQty()).isEqualTo(100 + 50);
        assertThat(summary.getDraftCount()).isEqualTo(1);
        assertThat(summary.getSubmittedCount()).isEqualTo(1);
    }
}
