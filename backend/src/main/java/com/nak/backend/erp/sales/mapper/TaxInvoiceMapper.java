package com.nak.backend.erp.sales.mapper;

import com.nak.backend.erp.sales.dto.TaxInvoiceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaxInvoiceMapper {
    List<TaxInvoiceDto> findAllTaxInvoices();
    
    TaxInvoiceDto findTaxInvoiceById(@Param("id") String id);
    
    List<TaxInvoiceDto.TaxInvoiceItemDto> findTaxInvoiceItemsByTaxInvoiceId(@Param("taxInvoiceId") String taxInvoiceId);
    
    void insertTaxInvoice(TaxInvoiceDto taxInvoice);
    
    void insertTaxInvoiceItem(TaxInvoiceDto.TaxInvoiceItemDto item);
    
    void updateTaxInvoice(TaxInvoiceDto taxInvoice);
    
    void deleteTaxInvoiceById(@Param("id") String id);
    
    void deleteTaxInvoiceItemsByTaxInvoiceId(@Param("taxInvoiceId") String taxInvoiceId);
    
    // 특정 작성 일자(YYYY-MM-DD) 기준 홈택스 승인번호 채번용 카운팅
    int countTaxInvoicesByDate(@Param("writeDate") String writeDate);
}
