from typing import Optional, List
from uuid import UUID
from datetime import date, datetime
from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel
from decimal import Decimal

class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True
    )

# --- Quotation Item Schemas ---
class QuotationItemBase(CamelModel):
    item_name: str
    spec: Optional[str] = None
    qty: Optional[int] = 1
    unit_price: Optional[Decimal] = Decimal("0.00")
    supply_value: Optional[Decimal] = Decimal("0.00")
    tax_value: Optional[Decimal] = Decimal("0.00")
    remarks: Optional[str] = None
    sort_order: Optional[int] = 0

class QuotationItemCreate(QuotationItemBase):
    pass

class QuotationItem(QuotationItemBase):
    id: UUID
    quotation_id: UUID


# --- Quotation Schemas ---
class QuotationBase(CamelModel):
    quotation_no: Optional[str] = None
    title: str
    quote_date: date
    valid_date: Optional[date] = None
    supplier_reg_no: str
    supplier_name: str
    supplier_ceo: str
    supplier_address: Optional[str] = None
    supplier_biz_type: Optional[str] = None
    supplier_biz_item: Optional[str] = None
    customer_name: str
    customer_ceo: Optional[str] = None
    total_supply_value: Optional[Decimal] = Decimal("0.00")
    total_tax_value: Optional[Decimal] = Decimal("0.00")
    total_amount: Optional[Decimal] = Decimal("0.00")
    remarks: Optional[str] = None
    status: Optional[str] = "draft"

class QuotationCreate(QuotationBase):
    items: Optional[List[QuotationItemCreate]] = []

class QuotationUpdate(CamelModel):
    title: Optional[str] = None
    quote_date: Optional[date] = None
    valid_date: Optional[date] = None
    supplier_reg_no: Optional[str] = None
    supplier_name: Optional[str] = None
    supplier_ceo: Optional[str] = None
    supplier_address: Optional[str] = None
    supplier_biz_type: Optional[str] = None
    supplier_biz_item: Optional[str] = None
    customer_name: Optional[str] = None
    customer_ceo: Optional[str] = None
    remarks: Optional[str] = None
    status: Optional[str] = None
    items: Optional[List[QuotationItemCreate]] = None

class Quotation(QuotationBase):
    id: UUID
    created_by: Optional[UUID] = None
    created_at: datetime
    updated_at: datetime
    items: Optional[List[QuotationItem]] = []


# --- Tax Invoice Item Schemas ---
class TaxInvoiceItemBase(CamelModel):
    item_date: Optional[str] = None  # MM-DD
    item_name: str
    spec: Optional[str] = None
    qty: Optional[int] = 1
    unit_price: Optional[Decimal] = Decimal("0.00")
    supply_value: Optional[Decimal] = Decimal("0.00")
    tax_value: Optional[Decimal] = Decimal("0.00")
    remarks: Optional[str] = None
    sort_order: Optional[int] = 0

class TaxInvoiceItemCreate(TaxInvoiceItemBase):
    pass

class TaxInvoiceItem(TaxInvoiceItemBase):
    id: UUID
    tax_invoice_id: UUID


# --- Tax Invoice Schemas ---
class TaxInvoiceBase(CamelModel):
    issue_id: Optional[str] = None
    write_date: date
    supplier_reg_no: str
    supplier_sub_no: Optional[str] = None
    supplier_name: str
    supplier_ceo: str
    supplier_address: Optional[str] = None
    supplier_biz_type: Optional[str] = None
    supplier_biz_item: Optional[str] = None
    supplier_email: Optional[str] = None
    customer_reg_no: str
    customer_sub_no: Optional[str] = None
    customer_name: str
    customer_ceo: Optional[str] = None
    customer_address: Optional[str] = None
    customer_biz_type: Optional[str] = None
    customer_biz_item: Optional[str] = None
    customer_email1: Optional[str] = None
    total_supply_value: Optional[Decimal] = Decimal("0.00")
    total_tax_value: Optional[Decimal] = Decimal("0.00")
    total_amount: Optional[Decimal] = Decimal("0.00")
    purpose: Optional[str] = "charge"
    status: Optional[str] = "draft"

class TaxInvoiceCreate(TaxInvoiceBase):
    items: Optional[List[TaxInvoiceItemCreate]] = []

class TaxInvoiceUpdate(CamelModel):
    write_date: Optional[date] = None
    supplier_reg_no: Optional[str] = None
    supplier_sub_no: Optional[str] = None
    supplier_name: Optional[str] = None
    supplier_ceo: Optional[str] = None
    supplier_address: Optional[str] = None
    supplier_biz_type: Optional[str] = None
    supplier_biz_item: Optional[str] = None
    supplier_email: Optional[str] = None
    customer_reg_no: Optional[str] = None
    customer_sub_no: Optional[str] = None
    customer_name: Optional[str] = None
    customer_ceo: Optional[str] = None
    customer_address: Optional[str] = None
    customer_biz_type: Optional[str] = None
    customer_biz_item: Optional[str] = None
    customer_email1: Optional[str] = None
    purpose: Optional[str] = None
    status: Optional[str] = None
    items: Optional[List[TaxInvoiceItemCreate]] = None

class TaxInvoice(TaxInvoiceBase):
    id: UUID
    created_by: Optional[UUID] = None
    created_at: datetime
    updated_at: datetime
    items: Optional[List[TaxInvoiceItem]] = []
