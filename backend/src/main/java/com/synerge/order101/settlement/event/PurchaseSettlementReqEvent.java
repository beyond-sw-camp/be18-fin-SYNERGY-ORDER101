package com.synerge.order101.settlement.event;

import com.synerge.order101.common.enums.SettlementType;
import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.purchase.model.entity.Purchase;
import com.synerge.order101.settlement.model.repository.SettlementRepository;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.supplier.model.entity.Supplier;

import java.math.BigDecimal;

public record PurchaseSettlementReqEvent(Purchase purchase) implements SettlementReqEvent {

    @Override
    public Integer settlementQty() {
        return purchase.getPurchaseDetails().stream()
                .mapToInt(detail -> detail.getOrderQty() != null ? detail.getOrderQty() : 0)
                .sum();
    }

    @Override
    public BigDecimal settlementAmount() {
        return purchase.getPurchaseDetails().stream()
                .map(detail -> {
                    BigDecimal unitPrice = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
                    Integer orderQty = detail.getOrderQty() != null ? detail.getOrderQty() : 0;
                    return unitPrice.multiply(BigDecimal.valueOf(orderQty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean existSettlement(SettlementRepository settlementRepository) {
        return settlementRepository.existsByPurchase_PurchaseId(purchase.getPurchaseId());
    }

    @Override
    public Purchase purchase() {
        return purchase;
    }

    @Override
    public Supplier supplier() {
        return purchase.getSupplier();
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






}


