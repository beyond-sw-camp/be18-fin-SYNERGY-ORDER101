package com.synerge.order101.inbound.model.service;

import com.synerge.order101.inbound.model.dto.InboundDetailResponseDto;
import com.synerge.order101.inbound.model.dto.InboundResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InboundService {
    Page<InboundResponseDto> getInboundList(int page, int size);

    InboundDetailResponseDto getInboundDetail(Long inboundId);
}
