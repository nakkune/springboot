package com.nak.backend.erp.sales.service;

import com.nak.backend.erp.sales.dto.QuotationDto;
import com.nak.backend.erp.sales.mapper.QuotationMapper;
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
public class QuotationService {

    @Autowired
    private QuotationMapper quotationMapper;

    public List<QuotationDto> getAllQuotations() {
        return quotationMapper.findAllQuotations();
    }

    public QuotationDto getQuotationById(String id) {
        QuotationDto quotation = quotationMapper.findQuotationById(id);
        if (quotation != null) {
            quotation.setItems(quotationMapper.findQuotationItemsByQuotationId(id));
        }
        return quotation;
    }

    public QuotationDto saveQuotation(QuotationDto dto) {
        boolean isNew = dto.getId() == null || dto.getId().trim().isEmpty();
        
        // 실시간 공급가액 및 세액 재연산 (신뢰성 확보 목적)
        recalculateTotals(dto);

        // SecurityContext에서 현재 로그인한 사용자 ID 주입
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            dto.setCreatedBy((String) authentication.getPrincipal());
        }

        if (isNew) {
            dto.setId(UUID.randomUUID().toString());
            
            // 견적서 일련번호 자동 생성 (한국 표준: QT-YYYYMMDD-001)
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String quoteNoDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            int countToday = quotationMapper.countQuotationsByDate(todayStr);
            String seqPart = String.format("%03d", countToday + 1);
            dto.setQuotationNo("QT-" + quoteNoDatePart + "-" + seqPart);
            
            quotationMapper.insertQuotation(dto);
        } else {
            quotationMapper.updateQuotation(dto);
            // 품목 갱신을 위해 기존 상세 품목 일괄 초기화
            quotationMapper.deleteQuotationItemsByQuotationId(dto.getId());
        }

        // 상세 품목 적재
        if (dto.getItems() != null) {
            int order = 10;
            for (QuotationDto.QuotationItemDto item : dto.getItems()) {
                item.setId(UUID.randomUUID().toString());
                item.setQuotationId(dto.getId());
                item.setSortOrder(order);
                quotationMapper.insertQuotationItem(item);
                order += 10;
            }
        }

        return getQuotationById(dto.getId());
    }

    public void deleteQuotation(String id) {
        quotationMapper.deleteQuotationItemsByQuotationId(id);
        quotationMapper.deleteQuotationById(id);
    }

    /**
     * 공급가액, 세액(부가세 10%), 합계금액 정밀 연산 모듈
     */
    private void recalculateTotals(QuotationDto dto) {
        BigDecimal totalSupply = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        if (dto.getItems() != null) {
            for (QuotationDto.QuotationItemDto item : dto.getItems()) {
                BigDecimal qty = BigDecimal.valueOf(item.getQty() != null ? item.getQty() : 0);
                BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                
                // 품목별 공급가액 = 수량 * 단가
                BigDecimal itemSupply = price.multiply(qty);
                // 품목별 세액 = 공급가액 * 10% (한국 부가가치세 표준율)
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
