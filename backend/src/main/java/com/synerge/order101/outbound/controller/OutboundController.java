package com.synerge.order101.outbound.controller;

import com.synerge.order101.common.dto.BaseResponseDto;
import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.outbound.model.dto.OutboundDetailResponseDto;
import com.synerge.order101.outbound.model.dto.OutboundResponseDto;
import com.synerge.order101.outbound.model.dto.OutboundSearchRequestDto;
import com.synerge.order101.outbound.model.service.OutboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/outbounds")
@RequiredArgsConstructor
public class OutboundController {
    private final OutboundService outboundService;

    // 전체 출고 조회
    @GetMapping
    public ResponseEntity<ItemsResponseDto<OutboundResponseDto>> getOutbounds(@RequestParam int page,
                                                                              @RequestParam int numOfRows) {

        Page<OutboundResponseDto> outbound = outboundService.getOutboundList(page, numOfRows);
        int totalCount = (int) outbound.getTotalElements();

        return ResponseEntity.ok(new ItemsResponseDto<>(HttpStatus.OK, outbound.getContent(), page, totalCount));
    }

    // 출고 상세 조회
    @GetMapping("/{outboundId}")
    public ResponseEntity<BaseResponseDto<OutboundDetailResponseDto>> getOutboundDetail(@PathVariable Long outboundId) {

        OutboundDetailResponseDto outboundDetail = outboundService.getOutboundDetail(outboundId);

        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, outboundDetail));
    }

    @PostMapping("/search")
    public ResponseEntity<ItemsResponseDto<OutboundResponseDto>> searchOutboundList(
            @RequestBody OutboundSearchRequestDto request
    ) {
        Page<OutboundResponseDto> outboundList = outboundService.searchOutboundList(request);
        int totalCount = (int) outboundList.getTotalElements();

        return ResponseEntity.ok(new ItemsResponseDto<>(HttpStatus.OK, outboundList.getContent(), outboundList.getNumber() + 1, totalCount));
    }
}
