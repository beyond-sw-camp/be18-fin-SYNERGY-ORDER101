package com.synerge.order101.inbound.controller;

import com.synerge.order101.common.dto.BaseResponseDto;
import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.inbound.model.dto.InboundDetailResponseDto;
import com.synerge.order101.inbound.model.dto.InboundResponseDto;
import com.synerge.order101.inbound.model.dto.InboundSearchRequestDto;
import com.synerge.order101.inbound.model.service.InboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inbounds")
@RequiredArgsConstructor
public class InboundController {
    private final InboundService inboundService;

    // 전체 입고 조회
    @GetMapping
    public ResponseEntity<ItemsResponseDto<InboundResponseDto>> getInbounds(@RequestParam int page,
                                                                            @RequestParam int numOfRows) {

        Page<InboundResponseDto> inbound = inboundService.getInboundList(page, numOfRows);
        int totalCount = (int) inbound.getTotalElements();

        return ResponseEntity.ok(new ItemsResponseDto<>(HttpStatus.OK, inbound.getContent(), page, totalCount));
    }

    // 입고 상세 조회
    @GetMapping("/{inboundId}")
    public ResponseEntity<BaseResponseDto<InboundDetailResponseDto>> getInboundDetail(@PathVariable Long inboundId) {

        InboundDetailResponseDto inboundDetail = inboundService.getInboundDetail(inboundId);

        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, inboundDetail));
    }

    // 검색 필터링
    @PostMapping("/search")
    public ResponseEntity<ItemsResponseDto<InboundResponseDto>> searchInboundList(
            @RequestBody InboundSearchRequestDto request
    ) {
        Page<InboundResponseDto> inboundList = inboundService.searchInboundList(request);
        int totalCount = (int) inboundList.getTotalElements();

        return ResponseEntity.ok(new ItemsResponseDto<>(HttpStatus.OK, inboundList.getContent(), inboundList.getNumber() + 1, totalCount));
    }
}
