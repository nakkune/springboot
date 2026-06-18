from typing import List, Optional, Any
from uuid import UUID, uuid4
from decimal import Decimal
from datetime import date
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy import func, delete
from sqlalchemy.orm import selectinload

from app.domains.erp.sales.models import Quotation, QuotationItem, TaxInvoice, TaxInvoiceItem
from app.domains.erp.sales import schemas

# =============================================================
# ── 1. 견적서 (Quotation) 서비스                             ──
# =============================================================

async def get_all_quotations(db: AsyncSession) -> List[Quotation]:
    query = select(Quotation).options(selectinload(Quotation.items)).order_by(Quotation.created_at.desc())
    result = await db.execute(query)
    return list(result.scalars().all())

async def get_quotation_by_id(db: AsyncSession, id: UUID) -> Optional[Quotation]:
    query = select(Quotation).filter(Quotation.id == id).options(selectinload(Quotation.items))
    result = await db.execute(query)
    return result.scalars().first()

def recalculate_quotation_totals(quotation_in: Any) -> None:
    total_supply = Decimal("0.00")
    total_tax = Decimal("0.00")
    
    if quotation_in.items:
        for item in quotation_in.items:
            qty = Decimal(str(item.qty or 0))
            price = Decimal(str(item.unit_price or 0.00))
            
            # 품목별 공급가액 = 수량 * 단가
            item_supply = price * qty
            # 품목별 세액 = 공급가액 * 10% (원단위 절사)
            item_tax = (item_supply * Decimal("0.1")) // Decimal("1")
            
            item.supply_value = item_supply
            item.tax_value = item_tax
            
            total_supply += item_supply
            total_tax += item_tax
            
    quotation_in.total_supply_value = total_supply
    quotation_in.total_tax_value = total_tax
    quotation_in.total_amount = total_supply + total_tax

async def save_quotation(db: AsyncSession, quotation_in: Any, creator_id: UUID) -> Quotation:
    # 실시간 공급가액 및 세액 재연산
    recalculate_quotation_totals(quotation_in)
    
    is_new = getattr(quotation_in, "id", None) is None
    
    if is_new:
        quotation_id = uuid4()
        
        # 견적서 일련번호 자동 생성 (QT-YYYYMMDD-001)
        today = date.today()
        today_str = today.strftime("%Y-%m-%d")
        quote_no_date_part = today.strftime("%Y%m%d")
        
        # 오늘 날짜 견적서 카운트
        count_query = select(func.count(Quotation.id)).filter(Quotation.quote_date == today)
        count_res = await db.execute(count_query)
        count_today = count_res.scalar() or 0
        seq_part = f"{count_today + 1:03d}"
        quotation_no = f"QT-{quote_no_date_part}-{seq_part}"
        
        db_quotation = Quotation(
            id=quotation_id,
            quotation_no=quotation_no,
            title=quotation_in.title,
            quote_date=quotation_in.quote_date,
            valid_date=quotation_in.valid_date,
            supplier_reg_no=quotation_in.supplier_reg_no,
            supplier_name=quotation_in.supplier_name,
            supplier_ceo=quotation_in.supplier_ceo,
            supplier_address=quotation_in.supplier_address,
            supplier_biz_type=quotation_in.supplier_biz_type,
            supplier_biz_item=quotation_in.supplier_biz_item,
            customer_name=quotation_in.customer_name,
            customer_ceo=quotation_in.customer_ceo,
            total_supply_value=quotation_in.total_supply_value,
            total_tax_value=quotation_in.total_tax_value,
            total_amount=quotation_in.total_amount,
            remarks=quotation_in.remarks,
            status=quotation_in.status or "draft",
            created_by=creator_id
        )
        db.add(db_quotation)
    else:
        quotation_id = quotation_in.id
        query = select(Quotation).filter(Quotation.id == quotation_id)
        res = await db.execute(query)
        db_quotation = res.scalars().first()
        if not db_quotation:
            raise ValueError("Quotation not found")
            
        # 기본 정보 필드 업데이트
        db_quotation.title = quotation_in.title
        db_quotation.quote_date = quotation_in.quote_date
        db_quotation.valid_date = quotation_in.valid_date
        db_quotation.supplier_reg_no = quotation_in.supplier_reg_no
        db_quotation.supplier_name = quotation_in.supplier_name
        db_quotation.supplier_ceo = quotation_in.supplier_ceo
        db_quotation.supplier_address = quotation_in.supplier_address
        db_quotation.supplier_biz_type = quotation_in.supplier_biz_type
        db_quotation.supplier_biz_item = quotation_in.supplier_biz_item
        db_quotation.customer_name = quotation_in.customer_name
        db_quotation.customer_ceo = quotation_in.customer_ceo
        db_quotation.total_supply_value = quotation_in.total_supply_value
        db_quotation.total_tax_value = quotation_in.total_tax_value
        db_quotation.total_amount = quotation_in.total_amount
        db_quotation.remarks = quotation_in.remarks
        db_quotation.status = quotation_in.status
        db.add(db_quotation)
        
        # 기존 품목 삭제
        await db.execute(delete(QuotationItem).filter(QuotationItem.quotation_id == quotation_id))
        
    # 세부 품목 저장
    if quotation_in.items:
        order = 10
        for item_in in quotation_in.items:
            db_item = QuotationItem(
                id=uuid4(),
                quotation_id=quotation_id,
                item_name=item_in.item_name,
                spec=item_in.spec,
                qty=item_in.qty,
                unit_price=item_in.unit_price,
                supply_value=item_in.supply_value,
                tax_value=item_in.tax_value,
                remarks=item_in.remarks,
                sort_order=order
            )
            db.add(db_item)
            order += 10
            
    await db.commit()
    return await get_quotation_by_id(db, quotation_id)

async def delete_quotation(db: AsyncSession, id: UUID) -> bool:
    db_obj = await get_quotation_by_id(db, id)
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True


# =============================================================
# ── 2. 세금계산서 (Tax Invoice) 서비스                        ──
# =============================================================

async def get_all_tax_invoices(db: AsyncSession) -> List[TaxInvoice]:
    query = select(TaxInvoice).options(selectinload(TaxInvoice.items)).order_by(TaxInvoice.created_at.desc())
    result = await db.execute(query)
    return list(result.scalars().all())

async def get_tax_invoice_by_id(db: AsyncSession, id: UUID) -> Optional[TaxInvoice]:
    query = select(TaxInvoice).filter(TaxInvoice.id == id).options(selectinload(TaxInvoice.items))
    result = await db.execute(query)
    return result.scalars().first()

def recalculate_tax_invoice_totals(tax_invoice_in: Any) -> None:
    total_supply = Decimal("0.00")
    total_tax = Decimal("0.00")
    
    if tax_invoice_in.items:
        for item in tax_invoice_in.items:
            qty = Decimal(str(item.qty or 0))
            price = Decimal(str(item.unit_price or 0.00))
            
            # 품목별 공급가액 = 수량 * 단가
            item_supply = price * qty
            # 품목별 세액 = 공급가액 * 10% (원단위 절사)
            item_tax = (item_supply * Decimal("0.1")) // Decimal("1")
            
            item.supply_value = item_supply
            item.tax_value = item_tax
            
            total_supply += item_supply
            total_tax += item_tax
            
    tax_invoice_in.total_supply_value = total_supply
    tax_invoice_in.total_tax_value = total_tax
    tax_invoice_in.total_amount = total_supply + total_tax

async def save_tax_invoice(db: AsyncSession, tax_invoice_in: Any, creator_id: UUID) -> TaxInvoice:
    # 실시간 공급가액 및 세액 재연산
    recalculate_tax_invoice_totals(tax_invoice_in)
    
    is_new = getattr(tax_invoice_in, "id", None) is None
    
    if is_new:
        tax_invoice_id = uuid4()
        
        # 국세청 표준 승인번호 자동 채번 (YYYYMMDD-41000000-000001)
        today = date.today()
        today_str = today.strftime("%Y-%m-%d")
        invoice_date_part = today.strftime("%Y%m%d")
        
        # 오늘 날짜 세금계산서 카운트
        count_query = select(func.count(TaxInvoice.id)).filter(TaxInvoice.write_date == today)
        count_res = await db.execute(count_query)
        count_today = count_res.scalar() or 0
        seq_part = f"{count_today + 1:06d}"
        issue_id = f"{invoice_date_part}-41000000-{seq_part}"
        
        db_invoice = TaxInvoice(
            id=tax_invoice_id,
            issue_id=issue_id,
            write_date=tax_invoice_in.write_date,
            supplier_reg_no=tax_invoice_in.supplier_reg_no,
            supplier_sub_no=tax_invoice_in.supplier_sub_no,
            supplier_name=tax_invoice_in.supplier_name,
            supplier_ceo=tax_invoice_in.supplier_ceo,
            supplier_address=tax_invoice_in.supplier_address,
            supplier_biz_type=tax_invoice_in.supplier_biz_type,
            supplier_biz_item=tax_invoice_in.supplier_biz_item,
            supplier_email=tax_invoice_in.supplier_email,
            customer_reg_no=tax_invoice_in.customer_reg_no,
            customer_sub_no=tax_invoice_in.customer_sub_no,
            customer_name=tax_invoice_in.customer_name,
            customer_ceo=tax_invoice_in.customer_ceo,
            customer_address=tax_invoice_in.customer_address,
            customer_biz_type=tax_invoice_in.customer_biz_type,
            customer_biz_item=tax_invoice_in.customer_biz_item,
            customer_email1=tax_invoice_in.customer_email1,
            total_supply_value=tax_invoice_in.total_supply_value,
            total_tax_value=tax_invoice_in.total_tax_value,
            total_amount=tax_invoice_in.total_amount,
            purpose=tax_invoice_in.purpose or "charge",
            status=tax_invoice_in.status or "draft",
            created_by=creator_id
        )
        db.add(db_invoice)
    else:
        tax_invoice_id = tax_invoice_in.id
        query = select(TaxInvoice).filter(TaxInvoice.id == tax_invoice_id)
        res = await db.execute(query)
        db_invoice = res.scalars().first()
        if not db_invoice:
            raise ValueError("TaxInvoice not found")
            
        # 기본 정보 업데이트
        db_invoice.write_date = tax_invoice_in.write_date
        db_invoice.supplier_reg_no = tax_invoice_in.supplier_reg_no
        db_invoice.supplier_sub_no = tax_invoice_in.supplier_sub_no
        db_invoice.supplier_name = tax_invoice_in.supplier_name
        db_invoice.supplier_ceo = tax_invoice_in.supplier_ceo
        db_invoice.supplier_address = tax_invoice_in.supplier_address
        db_invoice.supplier_biz_type = tax_invoice_in.supplier_biz_type
        db_invoice.supplier_biz_item = tax_invoice_in.supplier_biz_item
        db_invoice.supplier_email = tax_invoice_in.supplier_email
        db_invoice.customer_reg_no = tax_invoice_in.customer_reg_no
        db_invoice.customer_sub_no = tax_invoice_in.customer_sub_no
        db_invoice.customer_name = tax_invoice_in.customer_name
        db_invoice.customer_ceo = tax_invoice_in.customer_ceo
        db_invoice.customer_address = tax_invoice_in.customer_address
        db_invoice.customer_biz_type = tax_invoice_in.customer_biz_type
        db_invoice.customer_biz_item = tax_invoice_in.customer_biz_item
        db_invoice.customer_email1 = tax_invoice_in.customer_email1
        db_invoice.total_supply_value = tax_invoice_in.total_supply_value
        db_invoice.total_tax_value = tax_invoice_in.total_tax_value
        db_invoice.total_amount = tax_invoice_in.total_amount
        db_invoice.purpose = tax_invoice_in.purpose
        db_invoice.status = tax_invoice_in.status
        db.add(db_invoice)
        
        # 기존 품목 삭제
        await db.execute(delete(TaxInvoiceItem).filter(TaxInvoiceItem.tax_invoice_id == tax_invoice_id))
        
    # 세부 품목 저장
    if tax_invoice_in.items:
        order = 10
        for item_in in tax_invoice_in.items:
            db_item = TaxInvoiceItem(
                id=uuid4(),
                tax_invoice_id=tax_invoice_id,
                item_date=item_in.item_date,
                item_name=item_in.item_name,
                spec=item_in.spec,
                qty=item_in.qty,
                unit_price=item_in.unit_price,
                supply_value=item_in.supply_value,
                tax_value=item_in.tax_value,
                remarks=item_in.remarks,
                sort_order=order
            )
            db.add(db_item)
            order += 10
            
    await db.commit()
    return await get_tax_invoice_by_id(db, tax_invoice_id)

async def delete_tax_invoice(db: AsyncSession, id: UUID) -> bool:
    db_obj = await get_tax_invoice_by_id(db, id)
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True
