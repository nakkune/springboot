package com.nak.backend.erp.sales.controller;

import com.nak.backend.erp.sales.dto.QuotationDto;
import com.nak.backend.erp.sales.service.QuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "견적서 (Quotation)", description = "ERP 매출 관리 - 견적서 생성, 조회, 삭제 API를 제공합니다.")
@RestController
@RequestMapping("/erp/sales/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    @Operation(summary = "견적서 목록 조회", description = "시스템에 등록된 전체 견적서 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<QuotationDto>> getAllQuotations() {
        return ResponseEntity.ok(quotationService.getAllQuotations());
    }

    @Operation(summary = "견적서 단건 상세 조회", description = "견적서 ID를 기준으로 상세 정보(공급자 및 품목 세부내역 포함)를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<QuotationDto> getQuotationById(@PathVariable String id) {
        QuotationDto quotation = quotationService.getQuotationById(id);
        if (quotation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(quotation);
    }

    @Operation(summary = "견적서 신규 등록/수정 저장", description = "견적서 기본 정보와 세부 품목 목록을 생성하거나 기존 견적서를 업데이트합니다.")
    @PostMapping
    public ResponseEntity<QuotationDto> saveQuotation(@RequestBody QuotationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quotationService.saveQuotation(dto));
    }

    @Operation(summary = "견적서 삭제", description = "견적서 ID를 기준으로 해당 견적서 정보를 완전히 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuotation(@PathVariable String id) {
        quotationService.deleteQuotation(id);
        return ResponseEntity.noContent().build();
    }
}
