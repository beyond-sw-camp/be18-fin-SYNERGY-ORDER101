package com.synerge.order101.order.model.dto;

 import com.synerge.order101.product.model.entity.Product;
 import lombok.*;
 import org.springframework.beans.factory.annotation.Value;

 import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderCreateRequest {
    @NonNull
    private Long storeId;

    // warehouseId는 optional - 미지정 시 store의 defaultWarehouse 사용
    private Long warehouseId;

    @NonNull
    private Long userId;

    private List<Item> items;

    private String remark;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @NonNull
        private Long productId;

        @NonNull
        private Integer orderQty;
    }
}
