from typing import Any, List
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.erp.sales import schemas, service

router = APIRouter()

# =============================================================
# ── 1. 견적서 (Quotation) API                                 ──
# =============================================================

@router.get("/quotations", response_model=List[schemas.Quotation])
async def read_quotations(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_all_quotations(db)

@router.get("/quotations/{id}", response_model=schemas.Quotation)
async def read_quotation(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    quotation = await service.get_quotation_by_id(db, id)
    if not quotation:
        raise HTTPException(status_code=404, detail="Quotation not found")
    return quotation

@router.post("/quotations", response_model=schemas.Quotation, status_code=status.HTTP_201_CREATED)
async def save_quotation(
    quotation_in: schemas.QuotationCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.save_quotation(db, quotation_in=quotation_in, creator_id=current_user.id)

@router.delete("/quotations/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_quotation(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    deleted = await service.delete_quotation(db, id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Quotation not found")
    return None


# =============================================================
# ── 2. 세금계산서 (Tax Invoice) API                           ──
# =============================================================

@router.get("/tax-invoices", response_model=List[schemas.TaxInvoice])
async def read_tax_invoices(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_all_tax_invoices(db)

@router.get("/tax-invoices/{id}", response_model=schemas.TaxInvoice)
async def read_tax_invoice(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    tax_invoice = await service.get_tax_invoice_by_id(db, id)
    if not tax_invoice:
        raise HTTPException(status_code=404, detail="Tax invoice not found")
    return tax_invoice

@router.post("/tax-invoices", response_model=schemas.TaxInvoice, status_code=status.HTTP_201_CREATED)
async def save_tax_invoice(
    tax_invoice_in: schemas.TaxInvoiceCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.save_tax_invoice(db, tax_invoice_in=tax_invoice_in, creator_id=current_user.id)

@router.delete("/tax-invoices/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_tax_invoice(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    deleted = await service.delete_tax_invoice(db, id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Tax invoice not found")
    return None
