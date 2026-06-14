package com.nak.backend.erp.sales.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ERP 견적서 DTO (2026년 한국 실무 규격 준수)
 */
public class QuotationDto {
    private String id;
    private String quotationNo;
    private String title;
    private String quoteDate;
    private String validDate;
    private String supplierRegNo;
    private String supplierName;
    private String supplierCeo;
    private String supplierAddress;
    private String supplierBizType;
    private String supplierBizItem;
    private String customerName;
    private String customerCeo;
    private BigDecimal totalSupplyValue;
    private BigDecimal totalTaxValue;
    private BigDecimal totalAmount;
    private String remarks;
    private String status;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<QuotationItemDto> items = new ArrayList<>();

    public QuotationDto() {
        this.status = "draft";
        this.totalSupplyValue = BigDecimal.ZERO;
        this.totalTaxValue = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuotationNo() { return quotationNo; }
    public void setQuotationNo(String quotationNo) { this.quotationNo = quotationNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getQuoteDate() { return quoteDate; }
    public void setQuoteDate(String quoteDate) { this.quoteDate = quoteDate; }
    public String getValidDate() { return validDate; }
    public void setValidDate(String validDate) { this.validDate = validDate; }
    public String getSupplierRegNo() { return supplierRegNo; }
    public void setSupplierRegNo(String supplierRegNo) { this.supplierRegNo = supplierRegNo; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getSupplierCeo() { return supplierCeo; }
    public void setSupplierCeo(String supplierCeo) { this.supplierCeo = supplierCeo; }
    public String getSupplierAddress() { return supplierAddress; }
    public void setSupplierAddress(String supplierAddress) { this.supplierAddress = supplierAddress; }
    public String getSupplierBizType() { return supplierBizType; }
    public void setSupplierBizType(String supplierBizType) { this.supplierBizType = supplierBizType; }
    public String getSupplierBizItem() { return supplierBizItem; }
    public void setSupplierBizItem(String supplierBizItem) { this.supplierBizItem = supplierBizItem; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerCeo() { return customerCeo; }
    public void setCustomerCeo(String customerCeo) { this.customerCeo = customerCeo; }
    public BigDecimal getTotalSupplyValue() { return totalSupplyValue; }
    public void setTotalSupplyValue(BigDecimal totalSupplyValue) { this.totalSupplyValue = totalSupplyValue; }
    public BigDecimal getTotalTaxValue() { return totalTaxValue; }
    public void setTotalTaxValue(BigDecimal totalTaxValue) { this.totalTaxValue = totalTaxValue; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<QuotationItemDto> getItems() { return items; }
    public void setItems(List<QuotationItemDto> items) { this.items = items; }

    /**
     * 견적서 품목 상세 DTO
     */
    public static class QuotationItemDto {
        private String id;
        private String quotationId;
        private String itemName;
        private String spec;
        private Integer qty;
        private BigDecimal unitPrice;
        private BigDecimal supplyValue;
        private BigDecimal taxValue;
        private String remarks;
        private Integer sortOrder;

        public QuotationItemDto() {
            this.qty = 1;
            this.unitPrice = BigDecimal.ZERO;
            this.supplyValue = BigDecimal.ZERO;
            this.taxValue = BigDecimal.ZERO;
            this.sortOrder = 0;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getQuotationId() { return quotationId; }
        public void setQuotationId(String quotationId) { this.quotationId = quotationId; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getSpec() { return spec; }
        public void setSpec(String spec) { this.spec = spec; }
        public Integer getQty() { return qty; }
        public void setQty(Integer qty) { this.qty = qty; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getSupplyValue() { return supplyValue; }
        public void setSupplyValue(BigDecimal supplyValue) { this.supplyValue = supplyValue; }
        public BigDecimal getTaxValue() { return taxValue; }
        public void setTaxValue(BigDecimal taxValue) { this.taxValue = taxValue; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}
