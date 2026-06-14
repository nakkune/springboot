package com.nak.backend.erp.sales.controller;

import com.nak.backend.erp.sales.dto.TaxInvoiceDto;
import com.nak.backend.erp.sales.service.TaxInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "세금계산서 (Tax Invoice)", description = "ERP 매출 관리 - 세금계산서 발행, 조회, 삭제 API를 제공합니다.")
@RestController
@RequestMapping("/erp/sales/tax-invoices")
@RequiredArgsConstructor
public class TaxInvoiceController {

    private final TaxInvoiceService taxInvoiceService;

    @Operation(summary = "세금계산서 목록 조회", description = "시스템에 등록된 전체 세금계산서 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<TaxInvoiceDto>> getAllTaxInvoices() {
        return ResponseEntity.ok(taxInvoiceService.getAllTaxInvoices());
    }

    @Operation(summary = "세금계산서 단건 상세 조회", description = "세금계산서 ID를 기준으로 국세청 양식 규격에 맞춘 상세 데이터를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<TaxInvoiceDto> getTaxInvoiceById(@PathVariable String id) {
        TaxInvoiceDto taxInvoice = taxInvoiceService.getTaxInvoiceById(id);
        if (taxInvoice == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(taxInvoice);
    }

    @Operation(summary = "세금계산서 신규 등록/수정 저장", description = "세금계산서 정보를 신규 발행하거나 기존 발행 데이터를 수정 저장합니다.")
    @PostMapping
    public ResponseEntity<TaxInvoiceDto> saveTaxInvoice(@RequestBody TaxInvoiceDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taxInvoiceService.saveTaxInvoice(dto));
    }

    @Operation(summary = "세금계산서 삭제", description = "세금계산서 ID를 기준으로 해당 세금계산서 정보를 완전히 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTaxInvoice(@PathVariable String id) {
        taxInvoiceService.deleteTaxInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
