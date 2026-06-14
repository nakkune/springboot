package com.nak.backend.erp.sales.mapper;

import com.nak.backend.erp.sales.dto.QuotationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuotationMapper {
    List<QuotationDto> findAllQuotations();
    
    QuotationDto findQuotationById(@Param("id") String id);
    
    List<QuotationDto.QuotationItemDto> findQuotationItemsByQuotationId(@Param("quotationId") String quotationId);
    
    void insertQuotation(QuotationDto quotation);
    
    void insertQuotationItem(QuotationDto.QuotationItemDto item);
    
    void updateQuotation(QuotationDto quotation);
    
    void deleteQuotationById(@Param("id") String id);
    
    void deleteQuotationItemsByQuotationId(@Param("quotationId") String quotationId);
    
    // 최종 일련번호(채번) 조회를 위해 오늘 작성된 견적 개수 조회
    int countQuotationsByDate(@Param("quoteDate") String quoteDate);
}
