package com.synerge.order101.inbound.controller;

import com.synerge.order101.common.dto.BaseResponseDto;
import com.synerge.order101.common.dto.ItemsResponseDto;
import com.synerge.order101.inbound.model.dto.InboundDetailResponseDto;
import com.synerge.order101.inbound.model.dto.InboundResponseDto;
import com.synerge.order101.inbound.model.service.InboundService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ItemsResponseDto<InboundResponseDto>> getInbounds(@RequestParam(defaultValue = "1") int page,
                                                                            @RequestParam(defaultValue = "5") int size) {

        List<InboundResponseDto> inboundList = inboundService.getInboundList(page, size);
        int totalCount = inboundList.size();

        return ResponseEntity.ok(new ItemsResponseDto<>(HttpStatus.OK, inboundList, page, totalCount));
    }

    // 입고 상세 조회
    @GetMapping("/{inboundId}")
    public ResponseEntity<BaseResponseDto<InboundDetailResponseDto>> getInboundDetail(@PathVariable Long inboundId) {

        InboundDetailResponseDto inboundDetail = inboundService.getInboundDetail(inboundId);

        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, inboundDetail));
    }
}
