package com.nak.backend.erp.sales.service;

import com.nak.backend.erp.sales.dto.TaxInvoiceDto;
import com.nak.backend.erp.sales.mapper.TaxInvoiceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TaxInvoiceService {

    @Autowired
    private TaxInvoiceMapper taxInvoiceMapper;

    public List<TaxInvoiceDto> getAllTaxInvoices() {
        return taxInvoiceMapper.findAllTaxInvoices();
    }

    public TaxInvoiceDto getTaxInvoiceById(String id) {
        TaxInvoiceDto taxInvoice = taxInvoiceMapper.findTaxInvoiceById(id);
        if (taxInvoice != null) {
            taxInvoice.setItems(taxInvoiceMapper.findTaxInvoiceItemsByTaxInvoiceId(id));
        }
        return taxInvoice;
    }

    public TaxInvoiceDto saveTaxInvoice(TaxInvoiceDto dto) {
        boolean isNew = dto.getId() == null || dto.getId().trim().isEmpty();
        
        // 실시간 한국 세법 기준 금액 재연산
        recalculateTotals(dto);

        // SecurityContext에서 현재 로그인한 사용자 ID 주입
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            dto.setCreatedBy((String) authentication.getPrincipal());
        }

        if (isNew) {
            dto.setId(UUID.randomUUID().toString());
            
            // 국세청 전자세금계산서 표준 24자리 승인번호 자동 채번
            // 형식: YYYYMMDD (8자리 작성일자) + -41000000- (10자리 표준구격) + seq (6자리 시퀀스)
            // 예시: 20260604-41000000-000001
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String invoiceDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            int countToday = taxInvoiceMapper.countTaxInvoicesByDate(todayStr);
            String seqPart = String.format("%06d", countToday + 1);
            dto.setIssueId(invoiceDatePart + "-41000000-" + seqPart);
            
            taxInvoiceMapper.insertTaxInvoice(dto);
        } else {
            taxInvoiceMapper.updateTaxInvoice(dto);
            // 상세 품목 초기화
            taxInvoiceMapper.deleteTaxInvoiceItemsByTaxInvoiceId(dto.getId());
        }

        // 상세 품목 적재
        if (dto.getItems() != null) {
            int order = 10;
            for (TaxInvoiceDto.TaxInvoiceItemDto item : dto.getItems()) {
                item.setId(UUID.randomUUID().toString());
                item.setTaxInvoiceId(dto.getId());
                item.setSortOrder(order);
                taxInvoiceMapper.insertTaxInvoiceItem(item);
                order += 10;
            }
        }

        return getTaxInvoiceById(dto.getId());
    }

    public void deleteTaxInvoice(String id) {
        taxInvoiceMapper.deleteTaxInvoiceItemsByTaxInvoiceId(id);
        taxInvoiceMapper.deleteTaxInvoiceById(id);
    }

    /**
     * 부가가치세(10%) 표준 요율 및 단가 연산 모듈
     */
    private void recalculateTotals(TaxInvoiceDto dto) {
        BigDecimal totalSupply = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        if (dto.getItems() != null) {
            for (TaxInvoiceDto.TaxInvoiceItemDto item : dto.getItems()) {
                BigDecimal qty = BigDecimal.valueOf(item.getQty() != null ? item.getQty() : 0);
                BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;

                BigDecimal itemSupply = price.multiply(qty);
                BigDecimal itemTax = itemSupply.multiply(BigDecimal.valueOf(0.1)).setScale(0, BigDecimal.ROUND_DOWN); // 원단위 절사

                item.setSupplyValue(itemSupply);
                item.setTaxValue(itemTax);

                totalSupply = totalSupply.add(itemSupply);
                totalTax = totalTax.add(itemTax);
            }
        }

        dto.setTotalSupplyValue(totalSupply);
        dto.setTotalTaxValue(totalTax);
        dto.setTotalAmount(totalSupply.add(totalTax));
    }
}
