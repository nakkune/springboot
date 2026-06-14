package com.nak.backend.erp.sales.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ERP 전자세금계산서 DTO (2026년 한국 국세청 표준 규격 준수)
 */
public class TaxInvoiceDto {
    private String id;
    private String issueId;
    private String writeDate;
    private String supplierRegNo;
    private String supplierSubNo;
    private String supplierName;
    private String supplierCeo;
    private String supplierAddress;
    private String supplierBizType;
    private String supplierBizItem;
    private String supplierEmail;
    private String customerRegNo;
    private String customerSubNo;
    private String customerName;
    private String customerCeo;
    private String customerAddress;
    private String customerBizType;
    private String customerBizItem;
    private String customerEmail1;
    private BigDecimal totalSupplyValue;
    private BigDecimal totalTaxValue;
    private BigDecimal totalAmount;
    private String purpose; // 'charge': 청구, 'receipt': 영수
    private String status;  // 'draft': 작성중, 'issued': 발행완료
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<TaxInvoiceItemDto> items = new ArrayList<>();

    public TaxInvoiceDto() {
        this.purpose = "charge";
        this.status = "draft";
        this.totalSupplyValue = BigDecimal.ZERO;
        this.totalTaxValue = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIssueId() { return issueId; }
    public void setIssueId(String issueId) { this.issueId = issueId; }
    public String getWriteDate() { return writeDate; }
    public void setWriteDate(String writeDate) { this.writeDate = writeDate; }
    public String getSupplierRegNo() { return supplierRegNo; }
    public void setSupplierRegNo(String supplierRegNo) { this.supplierRegNo = supplierRegNo; }
    public String getSupplierSubNo() { return supplierSubNo; }
    public void setSupplierSubNo(String supplierSubNo) { this.supplierSubNo = supplierSubNo; }
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
    public String getSupplierEmail() { return supplierEmail; }
    public void setSupplierEmail(String supplierEmail) { this.supplierEmail = supplierEmail; }
    public String getCustomerRegNo() { return customerRegNo; }
    public void setCustomerRegNo(String customerRegNo) { this.customerRegNo = customerRegNo; }
    public String getCustomerSubNo() { return customerSubNo; }
    public void setCustomerSubNo(String customerSubNo) { this.customerSubNo = customerSubNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerCeo() { return customerCeo; }
    public void setCustomerCeo(String customerCeo) { this.customerCeo = customerCeo; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public String getCustomerBizType() { return customerBizType; }
    public void setCustomerBizType(String customerBizType) { this.customerBizType = customerBizType; }
    public String getCustomerBizItem() { return customerBizItem; }
    public void setCustomerBizItem(String customerBizItem) { this.customerBizItem = customerBizItem; }
    public String getCustomerEmail1() { return customerEmail1; }
    public void setCustomerEmail1(String customerEmail1) { this.customerEmail1 = customerEmail1; }
    public BigDecimal getTotalSupplyValue() { return totalSupplyValue; }
    public void setTotalSupplyValue(BigDecimal totalSupplyValue) { this.totalSupplyValue = totalSupplyValue; }
    public BigDecimal getTotalTaxValue() { return totalTaxValue; }
    public void setTotalTaxValue(BigDecimal totalTaxValue) { this.totalTaxValue = totalTaxValue; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<TaxInvoiceItemDto> getItems() { return items; }
    public void setItems(List<TaxInvoiceItemDto> items) { this.items = items; }

    /**
     * 세금계산서 품목 상세 DTO
     */
    public static class TaxInvoiceItemDto {
        private String id;
        private String taxInvoiceId;
        private String itemDate; // MM-DD
        private String itemName;
        private String spec;
        private Integer qty;
        private BigDecimal unitPrice;
        private BigDecimal supplyValue;
        private BigDecimal taxValue;
        private String remarks;
        private Integer sortOrder;

        public TaxInvoiceItemDto() {
            this.qty = 1;
            this.unitPrice = BigDecimal.ZERO;
            this.supplyValue = BigDecimal.ZERO;
            this.taxValue = BigDecimal.ZERO;
            this.sortOrder = 0;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTaxInvoiceId() { return taxInvoiceId; }
        public void setTaxInvoiceId(String taxInvoiceId) { this.taxInvoiceId = taxInvoiceId; }
        public String getItemDate() { return itemDate; }
        public void setItemDate(String itemDate) { this.itemDate = itemDate; }
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
