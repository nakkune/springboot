import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export interface QuotationItem {
  id?: string
  quotationId?: string
  itemName: string
  spec: string
  qty: number
  unitPrice: number
  supplyValue: number
  taxValue: number
  remarks: string
  sortOrder?: number
}

export interface Quotation {
  id?: string
  quotationNo?: string
  title: string
  quoteDate: string
  validDate: string
  supplierRegNo: string
  supplierName: string
  supplierCeo: string
  supplierAddress: string
  supplierBizType: string
  supplierBizItem: string
  customerName: string
  customerCeo: string
  totalSupplyValue: number
  totalTaxValue: number
  totalAmount: number
  remarks: string
  status: string // 'draft' | 'sent' | 'approved' | 'rejected'
  createdBy?: string
  createdAt?: string
  items: QuotationItem[]
}

export interface TaxInvoiceItem {
  id?: string
  taxInvoiceId?: string
  itemDate: string // MM-DD
  itemName: string
  spec: string
  qty: number
  unitPrice: number
  supplyValue: number
  taxValue: number
  remarks: string
  sortOrder?: number
}

export interface TaxInvoice {
  id?: string
  issueId?: string
  writeDate: string
  supplierRegNo: string
  supplierSubNo: string
  supplierName: string
  supplierCeo: string
  supplierAddress: string
  supplierBizType: string
  supplierBizItem: string
  supplierEmail: string
  customerRegNo: string
  customerSubNo: string
  customerName: string
  customerCeo: string
  customerAddress: string
  customerBizType: string
  customerBizItem: string
  customerEmail1: string
  totalSupplyValue: number
  totalTaxValue: number
  totalAmount: number
  purpose: string // 'charge' | 'receipt'
  status: string  // 'draft' | 'issued'
  createdBy?: string
  createdAt?: string
  items: TaxInvoiceItem[]
}

export const useSalesStore = defineStore('sales', () => {
  const quotations = ref<Quotation[]>([])
  const activeQuotation = ref<Quotation | null>(null)
  const taxInvoices = ref<TaxInvoice[]>([])
  const activeTaxInvoice = ref<TaxInvoice | null>(null)
  const loading = ref(false)

  // ── 1. 견적서 API 액션 ──
  async function fetchQuotations() {
    loading.value = true
    try {
      const res = await api.get('/erp/sales/quotations')
      quotations.value = res as any
    } catch (e) {
      console.error('견적 목록 로드 실패:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchQuotation(id: string) {
    loading.value = true
    try {
      const res = await api.get(`/erp/sales/quotations/${id}`) as any
      activeQuotation.value = res
      return res
    } catch (e) {
      console.error('견적 상세 조회 실패:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function saveQuotation(data: Quotation): Promise<any> {
    loading.value = true
    try {
      const res = await api.post('/erp/sales/quotations', data) as any
      activeQuotation.value = res
      return res
    } catch (e) {
      console.error('견적서 저장 실패:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deleteQuotation(id: string) {
    loading.value = true
    try {
      await api.delete(`/erp/sales/quotations/${id}`)
      quotations.value = quotations.value.filter(x => x.id !== id)
    } catch (e) {
      console.error('견적서 삭제 실패:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  // ── 2. 세금계산서 API 액션 ──
  async function fetchTaxInvoices() {
    loading.value = true
    try {
      const res = await api.get('/erp/sales/tax-invoices')
      taxInvoices.value = res as any
    } catch (e) {
      console.error('세금계산서 목록 로드 실패:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchTaxInvoice(id: string) {
    loading.value = true
    try {
      const res = await api.get(`/erp/sales/tax-invoices/${id}`) as any
      activeTaxInvoice.value = res
      return res
    } catch (e) {
      console.error('세금계산서 상세 조회 실패:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function saveTaxInvoice(data: TaxInvoice): Promise<any> {
    loading.value = true
    try {
      const res = await api.post('/erp/sales/tax-invoices', data) as any
      activeTaxInvoice.value = res
      return res
    } catch (e) {
      console.error('세금계산서 저장 실패:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deleteTaxInvoice(id: string) {
    loading.value = true
    try {
      await api.delete(`/erp/sales/tax-invoices/${id}`)
      taxInvoices.value = taxInvoices.value.filter(x => x.id !== id)
    } catch (e) {
      console.error('세금계산서 삭제 실패:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    quotations,
    activeQuotation,
    taxInvoices,
    activeTaxInvoice,
    loading,
    fetchQuotations,
    fetchQuotation,
    saveQuotation,
    deleteQuotation,
    fetchTaxInvoices,
    fetchTaxInvoice,
    saveTaxInvoice,
    deleteTaxInvoice
  }
})
