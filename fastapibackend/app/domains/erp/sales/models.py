import uuid
from sqlalchemy import Column, String, DateTime, ForeignKey, Integer, Numeric, Date, Text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.db.base_class import Base

class Quotation(Base):
    __tablename__ = "erp_quotations"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    quotation_no = Column(String(50), nullable=False, unique=True)
    title = Column(String(200), nullable=False)
    quote_date = Column(Date, nullable=False)
    valid_date = Column(Date, nullable=True)
    supplier_reg_no = Column(String(50), nullable=False)
    supplier_name = Column(String(100), nullable=False)
    supplier_ceo = Column(String(50), nullable=False)
    supplier_address = Column(String(255), nullable=True)
    supplier_biz_type = Column(String(100), nullable=True)
    supplier_biz_item = Column(String(100), nullable=True)
    customer_name = Column(String(100), nullable=False)
    customer_ceo = Column(String(50), nullable=True)
    total_supply_value = Column(Numeric(15, 2), default=0.00)
    total_tax_value = Column(Numeric(15, 2), default=0.00)
    total_amount = Column(Numeric(15, 2), default=0.00)
    remarks = Column(Text, nullable=True)
    status = Column(String(20), default="draft")
    created_by = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    creator = relationship("User", foreign_keys=[created_by])
    items = relationship("QuotationItem", back_populates="quotation", cascade="all, delete-orphan")


class QuotationItem(Base):
    __tablename__ = "erp_quotation_items"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    quotation_id = Column(UUID(as_uuid=True), ForeignKey("erp_quotations.id", ondelete="CASCADE"), nullable=False)
    item_name = Column(String(200), nullable=False)
    spec = Column(String(100), nullable=True)
    qty = Column(Integer, default=1)
    unit_price = Column(Numeric(15, 2), default=0.00)
    supply_value = Column(Numeric(15, 2), default=0.00)
    tax_value = Column(Numeric(15, 2), default=0.00)
    remarks = Column(String(255), nullable=True)
    sort_order = Column(Integer, default=0)

    quotation = relationship("Quotation", back_populates="items")


class TaxInvoice(Base):
    __tablename__ = "erp_tax_invoices"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    issue_id = Column(String(50), nullable=False, unique=True)
    write_date = Column(Date, nullable=False)
    supplier_reg_no = Column(String(50), nullable=False)
    supplier_sub_no = Column(String(4), nullable=True)
    supplier_name = Column(String(100), nullable=False)
    supplier_ceo = Column(String(50), nullable=False)
    supplier_address = Column(String(255), nullable=True)
    supplier_biz_type = Column(String(100), nullable=True)
    supplier_biz_item = Column(String(100), nullable=True)
    supplier_email = Column(String(100), nullable=True)
    customer_reg_no = Column(String(50), nullable=False)
    customer_sub_no = Column(String(4), nullable=True)
    customer_name = Column(String(100), nullable=False)
    customer_ceo = Column(String(50), nullable=True)
    customer_address = Column(String(255), nullable=True)
    customer_biz_type = Column(String(100), nullable=True)
    customer_biz_item = Column(String(100), nullable=True)
    customer_email1 = Column(String(100), nullable=True)
    total_supply_value = Column(Numeric(15, 2), default=0.00)
    total_tax_value = Column(Numeric(15, 2), default=0.00)
    total_amount = Column(Numeric(15, 2), default=0.00)
    purpose = Column(String(10), default="charge")
    status = Column(String(20), default="draft")
    created_by = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    creator = relationship("User", foreign_keys=[created_by])
    items = relationship("TaxInvoiceItem", back_populates="tax_invoice", cascade="all, delete-orphan")


class TaxInvoiceItem(Base):
    __tablename__ = "erp_tax_invoice_items"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    tax_invoice_id = Column(UUID(as_uuid=True), ForeignKey("erp_tax_invoices.id", ondelete="CASCADE"), nullable=False)
    item_date = Column(String(5), nullable=True)  # MM-DD
    item_name = Column(String(200), nullable=False)
    spec = Column(String(100), nullable=True)
    qty = Column(Integer, default=1)
    unit_price = Column(Numeric(15, 2), default=0.00)
    supply_value = Column(Numeric(15, 2), default=0.00)
    tax_value = Column(Numeric(15, 2), default=0.00)
    remarks = Column(String(255), nullable=True)
    sort_order = Column(Integer, default=0)

    tax_invoice = relationship("TaxInvoice", back_populates="items")
