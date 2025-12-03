package com.synerge.order101.inbound.model.service;

import com.synerge.order101.inbound.model.dto.InboundDetailResponseDto;
import com.synerge.order101.inbound.model.dto.InboundResponseDto;
import com.synerge.order101.inbound.model.dto.InboundSearchRequestDto;
import com.synerge.order101.purchase.model.entity.Purchase;
import org.springframework.data.domain.Page;

public interface InboundService {
    Page<InboundResponseDto> getInboundList(int page, int size);

    InboundDetailResponseDto getInboundDetail(Long inboundId);

    Page<InboundResponseDto> searchInboundList(InboundSearchRequestDto request);

    void createInbound(Purchase purchase);
}
