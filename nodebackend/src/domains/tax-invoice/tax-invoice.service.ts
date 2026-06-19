import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { TaxInvoiceDto, TaxInvoiceItemDto } from './dto/tax-invoice.dto';
import { parseSafeDate } from '../../common/utils/date';
import { randomUUID } from 'crypto';

@Injectable()
export class TaxInvoiceService {
  constructor(private readonly prisma: PrismaService) {}

  async getTaxInvoices(page: number = 1, size: number = 20) {
    const invoices = await this.prisma.erp_tax_invoices.findMany({
      orderBy: { write_date: 'desc' }
    });
    return invoices.map(i => ({
      id: i.id,
      issueId: i.issue_id,
      writeDate: i.write_date ? i.write_date.toISOString().split('T')[0] : null,
      supplierRegNo: i.supplier_reg_no,
      supplierSubNo: i.supplier_sub_no,
      supplierName: i.supplier_name,
      supplierCeo: i.supplier_ceo,
      supplierAddress: i.supplier_address,
      supplierBizType: i.supplier_biz_type,
      supplierBizItem: i.supplier_biz_item,
      supplierEmail: i.supplier_email,
      customerRegNo: i.customer_reg_no,
      customerSubNo: i.customer_sub_no,
      customerName: i.customer_name,
      customerCeo: i.customer_ceo,
      customerAddress: i.customer_address,
      customerBizType: i.customer_biz_type,
      customerBizItem: i.customer_biz_item,
      customerEmail1: i.customer_email1,
      totalSupplyValue: i.total_supply_value ? Number(i.total_supply_value) : 0,
      totalTaxValue: i.total_tax_value ? Number(i.total_tax_value) : 0,
      totalAmount: i.total_amount ? Number(i.total_amount) : 0,
      purpose: i.purpose,
      status: i.status,
      createdBy: i.created_by,
      createdAt: i.created_at,
      updatedAt: i.updated_at,
    }));
  }

  async getTaxInvoiceById(id: string) {
    const i = await this.prisma.erp_tax_invoices.findUnique({
      where: { id },
      include: { erp_tax_invoice_items: true }
    });
    if (!i) return null;
    return {
      id: i.id,
      issueId: i.issue_id,
      writeDate: i.write_date ? i.write_date.toISOString().split('T')[0] : null,
      supplierRegNo: i.supplier_reg_no,
      supplierSubNo: i.supplier_sub_no,
      supplierName: i.supplier_name,
      supplierCeo: i.supplier_ceo,
      supplierAddress: i.supplier_address,
      supplierBizType: i.supplier_biz_type,
      supplierBizItem: i.supplier_biz_item,
      supplierEmail: i.supplier_email,
      customerRegNo: i.customer_reg_no,
      customerSubNo: i.customer_sub_no,
      customerName: i.customer_name,
      customerCeo: i.customer_ceo,
      customerAddress: i.customer_address,
      customerBizType: i.customer_biz_type,
      customerBizItem: i.customer_biz_item,
      customerEmail1: i.customer_email1,
      totalSupplyValue: i.total_supply_value ? Number(i.total_supply_value) : 0,
      totalTaxValue: i.total_tax_value ? Number(i.total_tax_value) : 0,
      totalAmount: i.total_amount ? Number(i.total_amount) : 0,
      purpose: i.purpose,
      status: i.status,
      createdBy: i.created_by,
      createdAt: i.created_at,
      updatedAt: i.updated_at,
      items: i.erp_tax_invoice_items.map(item => ({
        id: item.id,
        invoiceId: item.tax_invoice_id,
        purchaseDate: item.item_date,
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

  async saveTaxInvoice(dto: TaxInvoiceDto, userId: string) {
    const isNew = !dto.id;
    const taxInvoiceId = dto.id || randomUUID();

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
        tax_invoice_id: taxInvoiceId,
        item_date: item.itemDate || '',
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
      // YYYYMMDD (8자리 작성일자) + -41000000- (10자리 표준구격) + seq (6자리 시퀀스)
      const now = new Date();
      const yearStr = now.getFullYear();
      const monthStr = String(now.getMonth() + 1).padStart(2, '0');
      const dayStr = String(now.getDate()).padStart(2, '0');
      const datePart = `${yearStr}${monthStr}${dayStr}`;
      const searchPattern = `${datePart}-41000000-%`;

      const lastInvoice = await this.prisma.erp_tax_invoices.findFirst({
        where: { issue_id: { startsWith: `${datePart}-41000000-` } },
        orderBy: { issue_id: 'desc' }
      });

      let nextSeq = 1;
      if (lastInvoice) {
        const parts = lastInvoice.issue_id.split('-');
        if (parts.length === 3) {
          nextSeq = parseInt(parts[2], 10) + 1;
        }
      }
      const seqPart = String(nextSeq).padStart(6, '0');
      const issueId = `${datePart}-41000000-${seqPart}`;

      await this.prisma.erp_tax_invoices.create({
        data: {
          id: taxInvoiceId,
          issue_id: issueId,
          write_date: parseSafeDate(dto.writeDate),
          supplier_reg_no: dto.supplierRegNo,
          supplier_sub_no: dto.supplierSubNo || '',
          supplier_name: dto.supplierName,
          supplier_ceo: dto.supplierCeo,
          supplier_address: dto.supplierAddress || '',
          supplier_biz_type: dto.supplierBizType || '',
          supplier_biz_item: dto.supplierBizItem || '',
          supplier_email: dto.supplierEmail || '',
          customer_reg_no: dto.customerRegNo,
          customer_sub_no: dto.customerSubNo || '',
          customer_name: dto.customerName,
          customer_ceo: dto.customerCeo || '',
          customer_address: dto.customerAddress || '',
          customer_biz_type: dto.customerBizType || '',
          customer_biz_item: dto.customerBizItem || '',
          customer_email1: dto.customerEmail1 || '',
          total_supply_value: totalSupply,
          total_tax_value: totalTax,
          total_amount: totalAmount,
          purpose: dto.purpose || 'charge',
          status: dto.status || 'draft',
          created_by: userId,
        }
      });
    } else {
      await this.prisma.erp_tax_invoices.update({
        where: { id: taxInvoiceId },
        data: {
          write_date: parseSafeDate(dto.writeDate),
          supplier_reg_no: dto.supplierRegNo,
          supplier_sub_no: dto.supplierSubNo || '',
          supplier_name: dto.supplierName,
          supplier_ceo: dto.supplierCeo,
          supplier_address: dto.supplierAddress || '',
          supplier_biz_type: dto.supplierBizType || '',
          supplier_biz_item: dto.supplierBizItem || '',
          supplier_email: dto.supplierEmail || '',
          customer_reg_no: dto.customerRegNo,
          customer_sub_no: dto.customerSubNo || '',
          customer_name: dto.customerName,
          customer_ceo: dto.customerCeo || '',
          customer_address: dto.customerAddress || '',
          customer_biz_type: dto.customerBizType || '',
          customer_biz_item: dto.customerBizItem || '',
          customer_email1: dto.customerEmail1 || '',
          total_supply_value: totalSupply,
          total_tax_value: totalTax,
          total_amount: totalAmount,
          purpose: dto.purpose || 'charge',
          status: dto.status || 'draft',
        }
      });

      // Clear existing items
      await this.prisma.erp_tax_invoice_items.deleteMany({
        where: { tax_invoice_id: taxInvoiceId }
      });
    }

    // Insert new items
    for (const item of recalculatedItems) {
      await this.prisma.erp_tax_invoice_items.create({
        data: item
      });
    }

    return this.getTaxInvoiceById(taxInvoiceId);
  }

  async deleteTaxInvoice(id: string) {
    const existing = await this.prisma.erp_tax_invoices.findUnique({ where: { id } });
    if (!existing) throw new NotFoundException(`TaxInvoice not found: ${id}`);

    // Cascade delete items
    await this.prisma.erp_tax_invoice_items.deleteMany({ where: { tax_invoice_id: id } });
    await this.prisma.erp_tax_invoices.delete({ where: { id } });
  }
}
