import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { QuotationDto, QuotationItemDto } from './dto/quotation.dto';
import { parseSafeDate } from '../../common/utils/date';
import { randomUUID } from 'crypto';

@Injectable()
export class QuotationService {
  constructor(private readonly prisma: PrismaService) {}

  async getQuotations(page: number = 1, size: number = 20) {
    const quotes = await this.prisma.erp_quotations.findMany({
      orderBy: { quote_date: 'desc' }
    });
    return quotes.map(q => ({
      id: q.id,
      quotationNo: q.quotation_no,
      title: q.title,
      quoteDate: q.quote_date ? q.quote_date.toISOString().split('T')[0] : null,
      validDate: q.valid_date ? q.valid_date.toISOString().split('T')[0] : null,
      supplierRegNo: q.supplier_reg_no,
      supplierName: q.supplier_name,
      supplierCeo: q.supplier_ceo,
      supplierAddress: q.supplier_address,
      supplierBizType: q.supplier_biz_type,
      supplierBizItem: q.supplier_biz_item,
      customerName: q.customer_name,
      customerCeo: q.customer_ceo,
      totalSupplyValue: q.total_supply_value ? Number(q.total_supply_value) : 0,
      totalTaxValue: q.total_tax_value ? Number(q.total_tax_value) : 0,
      totalAmount: q.total_amount ? Number(q.total_amount) : 0,
      remarks: q.remarks,
      status: q.status,
      createdBy: q.created_by,
      createdAt: q.created_at,
      updatedAt: q.updated_at,
    }));
  }

  async getQuotationById(id: string) {
    const q = await this.prisma.erp_quotations.findUnique({
      where: { id },
      include: { erp_quotation_items: true }
    });
    if (!q) return null;
    return {
      id: q.id,
      quotationNo: q.quotation_no,
      title: q.title,
      quoteDate: q.quote_date ? q.quote_date.toISOString().split('T')[0] : null,
      validDate: q.valid_date ? q.valid_date.toISOString().split('T')[0] : null,
      supplierRegNo: q.supplier_reg_no,
      supplierName: q.supplier_name,
      supplierCeo: q.supplier_ceo,
      supplierAddress: q.supplier_address,
      supplierBizType: q.supplier_biz_type,
      supplierBizItem: q.supplier_biz_item,
      customerName: q.customer_name,
      customerCeo: q.customer_ceo,
      totalSupplyValue: q.total_supply_value ? Number(q.total_supply_value) : 0,
      totalTaxValue: q.total_tax_value ? Number(q.total_tax_value) : 0,
      totalAmount: q.total_amount ? Number(q.total_amount) : 0,
      remarks: q.remarks,
      status: q.status,
      createdBy: q.created_by,
      createdAt: q.created_at,
      updatedAt: q.updated_at,
      items: q.erp_quotation_items.map(item => ({
        id: item.id,
        quotationId: item.quotation_id,
        itemName: item.item_name,
        spec: item.spec,
        qty: item.qty ? Number(item.qty) : 0,
        unitPrice: item.unit_price ? Number(item.unit_price) : 0,
        supplyValue: item.supply_value ? Number(item.supply_value) : 0,
        taxValue: item.tax_value ? Number(item.tax_value) : 0,
        remarks: item.remarks,
        sortOrder: item.sort_order,
      })),
    };
  }

  async saveQuotation(dto: QuotationDto, userId: string) {
    const isNew = !dto.id;
    const quotationId = dto.id || randomUUID();

    // Recalculate totals
    let totalSupply = 0;
    let totalTax = 0;

    const recalculatedItems = (dto.items || []).map((item, index) => {
      const itemQty = item.qty || 0;
      const itemPrice = item.unitPrice || 0;
      const itemSupply = itemQty * itemPrice;
      const itemTax = Math.floor(itemSupply * 0.1); // 원단위 절사

      totalSupply += itemSupply;
      totalTax += itemTax;

      return {
        id: item.id || randomUUID(),
        quotation_id: quotationId,
        item_name: item.itemName,
        spec: item.spec || '',
        qty: itemQty,
        unit_price: itemPrice,
        supply_value: itemSupply,
        tax_value: itemTax,
        remarks: item.remarks || '',
        sort_order: item.sortOrder || (index + 1) * 10,
      };
    });

    const totalAmount = totalSupply + totalTax;

    if (isNew) {
      // Automatic sequence number creation: QT-YYYYMMDD-seq (3-digit seq)
      const now = new Date();
      const yearStr = now.getFullYear();
      const monthStr = String(now.getMonth() + 1).padStart(2, '0');
      const dayStr = String(now.getDate()).padStart(2, '0');
      const datePart = `${yearStr}${monthStr}${dayStr}`;
      const searchPattern = `QT-${datePart}-%`;

      const lastQuote = await this.prisma.erp_quotations.findFirst({
        where: { quotation_no: { startsWith: `QT-${datePart}-` } },
        orderBy: { quotation_no: 'desc' }
      });

      let nextSeq = 1;
      if (lastQuote) {
        const parts = lastQuote.quotation_no.split('-');
        if (parts.length === 3) {
          nextSeq = parseInt(parts[2], 10) + 1;
        }
      }
      const seqPart = String(nextSeq).padStart(3, '0');
      const quotationNo = `QT-${datePart}-${seqPart}`;

      await this.prisma.erp_quotations.create({
        data: {
          id: quotationId,
          quotation_no: quotationNo,
          title: dto.title,
          quote_date: parseSafeDate(dto.quoteDate),
          valid_date: dto.validDate ? parseSafeDate(dto.validDate) : null,
          supplier_reg_no: dto.supplierRegNo,
          supplier_name: dto.supplierName,
          supplier_ceo: dto.supplierCeo,
          supplier_address: dto.supplierAddress || '',
          supplier_biz_type: dto.supplierBizType || '',
          supplier_biz_item: dto.supplierBizItem || '',
          customer_name: dto.customerName,
          customer_ceo: dto.customerCeo || '',
          total_supply_value: totalSupply,
          total_tax_value: totalTax,
          total_amount: totalAmount,
          remarks: dto.remarks || '',
          status: dto.status || 'draft',
          created_by: userId,
        }
      });
    } else {
      await this.prisma.erp_quotations.update({
        where: { id: quotationId },
        data: {
          title: dto.title,
          quote_date: parseSafeDate(dto.quoteDate),
          valid_date: dto.validDate ? parseSafeDate(dto.validDate) : null,
          supplier_reg_no: dto.supplierRegNo,
          supplier_name: dto.supplierName,
          supplier_ceo: dto.supplierCeo,
          supplier_address: dto.supplierAddress || '',
          supplier_biz_type: dto.supplierBizType || '',
          supplier_biz_item: dto.supplierBizItem || '',
          customer_name: dto.customerName,
          customer_ceo: dto.customerCeo || '',
          total_supply_value: totalSupply,
          total_tax_value: totalTax,
          total_amount: totalAmount,
          remarks: dto.remarks || '',
          status: dto.status || 'draft',
        }
      });

      // Clear existing items
      await this.prisma.erp_quotation_items.deleteMany({
        where: { quotation_id: quotationId }
      });
    }

    // Insert new items
    for (const item of recalculatedItems) {
      await this.prisma.erp_quotation_items.create({
        data: item
      });
    }

    return this.getQuotationById(quotationId);
  }

  async deleteQuotation(id: string) {
    const existing = await this.prisma.erp_quotations.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`Quotation not found: ${id}`);

    // Cascade delete items
    await this.prisma.erp_quotation_items.deleteMany({ where: { quotation_id: id } });
    await this.prisma.erp_quotations.delete({ where: { id } });
  }
}
