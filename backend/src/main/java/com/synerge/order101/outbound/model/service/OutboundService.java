package com.synerge.order101.outbound.model.service;

import com.synerge.order101.outbound.model.dto.OutboundDetailResponseDto;
import com.synerge.order101.outbound.model.dto.OutboundResponseDto;
import com.synerge.order101.shipment.event.ShipmentInTransitEvent;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OutboundService {
    Page<OutboundResponseDto> getOutboundList(int page, int size);

    OutboundDetailResponseDto getOutboundDetail(Long outboundId);

    void createOutboundFromShipment(ShipmentInTransitEvent event);
}
