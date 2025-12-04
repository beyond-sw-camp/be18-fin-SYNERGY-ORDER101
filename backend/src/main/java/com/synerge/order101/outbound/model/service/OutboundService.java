package com.synerge.order101.outbound.model.service;

import com.synerge.order101.order.model.entity.StoreOrder;
import com.synerge.order101.outbound.model.dto.OutboundDetailResponseDto;
import com.synerge.order101.outbound.model.dto.OutboundResponseDto;
import com.synerge.order101.outbound.model.dto.OutboundSearchRequestDto;
import com.synerge.order101.shipment.event.ShipmentInTransitEvent;
import com.synerge.order101.shipment.model.entity.Shipment;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OutboundService {
    Page<OutboundResponseDto> getOutboundList(int page, int size);

    OutboundDetailResponseDto getOutboundDetail(Long outboundId);

    void createOutbound(StoreOrder storeOrder);

    Page<OutboundResponseDto> searchOutboundList(OutboundSearchRequestDto request);
}
