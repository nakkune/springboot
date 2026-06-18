from typing import Optional, List, Dict, Any
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

# --- Department Schemas ---
class DepartmentBase(CamelModel):
    parent_id: Optional[UUID] = None
    name: str
    code: Optional[str] = None
    manager_id: Optional[UUID] = None
    sort_order: Optional[int] = 0
    is_active: Optional[bool] = True

class DepartmentCreate(DepartmentBase):
    pass

class DepartmentUpdate(CamelModel):
    parent_id: Optional[UUID] = None
    name: Optional[str] = None
    code: Optional[str] = None
    manager_id: Optional[UUID] = None
    sort_order: Optional[int] = None
    is_active: Optional[bool] = None

class Department(DepartmentBase):
    id: UUID
    created_at: datetime
    updated_at: datetime
    manager_name: Optional[str] = None
    children: Optional[List['Department']] = None


# --- Employee Schemas ---
class EmployeeBase(CamelModel):
    user_id: Optional[UUID] = None
    employee_no: Optional[str] = None
    department_id: Optional[UUID] = None
    position: Optional[str] = None
    job_title: Optional[str] = None
    employment_type: Optional[str] = "full_time"
    hire_date: date
    resignation_date: Optional[date] = None
    status: Optional[str] = "active"
    phone: Optional[str] = None
    email: Optional[str] = None
    emergency_contact: Optional[str] = None
    emergency_phone: Optional[str] = None
    bank_name: Optional[str] = None
    bank_account: Optional[str] = None
    annual_leave_days: Optional[Decimal] = Decimal("15.0")
    memo: Optional[str] = None
    leave_start_date: Optional[date] = None
    leave_end_date: Optional[date] = None
    birth_date: Optional[date] = None

class EmployeeCreate(EmployeeBase):
    pass

class EmployeeUpdate(CamelModel):
    user_id: Optional[UUID] = None
    department_id: Optional[UUID] = None
    position: Optional[str] = None
    job_title: Optional[str] = None
    employment_type: Optional[str] = None
    hire_date: Optional[date] = None
    resignation_date: Optional[date] = None
    status: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    emergency_contact: Optional[str] = None
    emergency_phone: Optional[str] = None
    bank_name: Optional[str] = None
    bank_account: Optional[str] = None
    annual_leave_days: Optional[Decimal] = None
    memo: Optional[str] = None
    leave_start_date: Optional[date] = None
    leave_end_date: Optional[date] = None
    birth_date: Optional[date] = None

class Employee(EmployeeBase):
    id: UUID
    employee_name: Optional[str] = None  # JOIN full_name
    department_name: Optional[str] = None  # JOIN name
    created_by: UUID
    created_at: datetime
    updated_at: datetime


class EmployeeListResponse(CamelModel):
    items: List[Employee]
    total: int


# --- Attendance Schemas ---
class AttendanceBase(CamelModel):
    employee_id: UUID
    work_date: date
    check_in: Optional[datetime] = None
    check_out: Optional[datetime] = None
    work_hours: Optional[Decimal] = Decimal("0.0")
    overtime_hours: Optional[Decimal] = Decimal("0.0")
    status: Optional[str] = "present"
    memo: Optional[str] = None

class AttendanceCreate(AttendanceBase):
    pass

class AttendanceUpdate(CamelModel):
    check_in: Optional[datetime] = None
    check_out: Optional[datetime] = None
    work_hours: Optional[Decimal] = None
    overtime_hours: Optional[Decimal] = None
    status: Optional[str] = None
    memo: Optional[str] = None

class Attendance(AttendanceBase):
    id: UUID
    employee_name: Optional[str] = None
    employee_no: Optional[str] = None
    created_at: datetime
    updated_at: datetime


# --- LeaveRequest Schemas ---
class LeaveRequestBase(CamelModel):
    employee_id: UUID
    leave_type: str
    start_date: date
    end_date: date
    total_days: Decimal
    reason: str
    status: Optional[str] = "pending"
    approver_id: Optional[UUID] = None
    approved_at: Optional[datetime] = None
    reject_reason: Optional[str] = None
    linked_item_id: Optional[UUID] = None

class LeaveRequestCreate(LeaveRequestBase):
    pass

class LeaveRequestUpdate(CamelModel):
    status: Optional[str] = None
    approver_id: Optional[UUID] = None
    approved_at: Optional[datetime] = None
    reject_reason: Optional[str] = None

class LeaveRequest(LeaveRequestBase):
    id: UUID
    employee_name: Optional[str] = None
    employee_no: Optional[str] = None
    approver_name: Optional[str] = None
    created_by: UUID
    created_at: datetime
    updated_at: datetime


# --- LeaveBalance Schemas ---
class LeaveBalanceBase(CamelModel):
    employee_id: UUID
    year: int
    total_days: Optional[Decimal] = Decimal("15.0")
    used_days: Optional[Decimal] = Decimal("0.0")
    remaining_days: Optional[Decimal] = Decimal("15.0")

class LeaveBalanceCreate(LeaveBalanceBase):
    pass

class LeaveBalance(LeaveBalanceBase):
    id: UUID
    employee_name: Optional[str] = None
    employee_no: Optional[str] = None
    created_at: datetime
    updated_at: datetime


# --- PerformanceReview Schemas ---
class PerformanceReviewBase(CamelModel):
    employee_id: UUID
    reviewer_id: UUID
    review_year: int
    review_period: Optional[str] = "annual"
    ratings: Dict[str, Any] = {}
    total_score: Optional[Decimal] = None
    comment: Optional[str] = None
    status: Optional[str] = "draft"

class PerformanceReviewCreate(PerformanceReviewBase):
    pass

class PerformanceReviewUpdate(CamelModel):
    ratings: Optional[Dict[str, Any]] = None
    total_score: Optional[Decimal] = None
    comment: Optional[str] = None
    status: Optional[str] = None
    submitted_at: Optional[datetime] = None

class PerformanceReview(PerformanceReviewBase):
    id: UUID
    employee_name: Optional[str] = None
    employee_no: Optional[str] = None
    reviewer_name: Optional[str] = None
    submitted_at: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime


# --- CommonCode Schemas ---
class CommonCodeBase(CamelModel):
    code_group: str
    code: str
    label: str
    sort_order: Optional[int] = 0
    is_active: Optional[bool] = True
    code_group_name: Optional[str] = ""

class CommonCodeCreate(CommonCodeBase):
    pass

class CommonCode(CommonCodeBase):
    id: UUID
    created_at: datetime
    updated_at: datetime


# --- PayrollCode Schemas ---
class PayrollCodeBase(CamelModel):
    code: str
    name: str
    type: str
    is_taxable: Optional[bool] = True
    tax_free_limit: Optional[Decimal] = Decimal("0.0")
    is_system: Optional[bool] = False
    is_active: Optional[bool] = True
    sort_order: Optional[int] = 0

class PayrollCode(PayrollCodeBase):
    created_at: datetime


# --- SalaryTemplate Schemas ---
class SalaryTemplateBase(CamelModel):
    employee_id: UUID
    base_pay: Optional[Decimal] = Decimal("0.0")
    position_pay: Optional[Decimal] = Decimal("0.0")
    meal_allowance: Optional[Decimal] = Decimal("200000.0")
    car_allowance: Optional[Decimal] = Decimal("0.0")
    use_national_pension: Optional[bool] = True
    use_health_insurance: Optional[bool] = True
    use_employment_insurance: Optional[bool] = True
    income_tax_rate: Optional[int] = 100

class SalaryTemplate(SalaryTemplateBase):
    employee_name: Optional[str] = None
    employee_no: Optional[str] = None
    department_name: Optional[str] = None
    created_at: datetime
    updated_at: datetime


# --- PayrollDetail Schemas ---
class PayrollDetailBase(CamelModel):
    payroll_item_id: UUID
    code: str
    amount: Optional[Decimal] = Decimal("0.0")
    is_taxable: Optional[bool] = True

class PayrollDetailCreate(PayrollDetailBase):
    pass

class PayrollDetail(PayrollDetailBase):
    id: UUID
    name: Optional[str] = None
    type: Optional[str] = None
    created_at: datetime


# --- PayrollItem Schemas ---
class PayrollItemBase(CamelModel):
    ledger_id: UUID
    employee_id: UUID
    gross_pay: Optional[Decimal] = Decimal("0.0")
    total_deduction: Optional[Decimal] = Decimal("0.0")
    net_pay: Optional[Decimal] = Decimal("0.0")
    bank_name: Optional[str] = None
    bank_account: Optional[str] = None
    bank_owner: Optional[str] = None
    status: Optional[str] = "draft"
    memo: Optional[str] = None

class PayrollItem(PayrollItemBase):
    id: UUID
    employee_name: Optional[str] = None
    employee_no: Optional[str] = None
    department_name: Optional[str] = None
    position: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    details: Optional[List[PayrollDetail]] = []


# --- PayrollLedger Schemas ---
class PayrollLedgerBase(CamelModel):
    title: str
    pay_year: int
    pay_month: int
    pay_date: date
    pay_type: Optional[str] = "정기급여"
    start_date: date
    end_date: date
    status: Optional[str] = "draft"

class PayrollLedgerCreate(PayrollLedgerBase):
    pass

class PayrollLedger(PayrollLedgerBase):
    id: UUID
    created_by: UUID
    created_at: datetime
    updated_at: datetime
    total_gross: Optional[Decimal] = Decimal("0.0")
    total_deduction: Optional[Decimal] = Decimal("0.0")
    total_net: Optional[Decimal] = Decimal("0.0")
    employee_count: Optional[int] = 0
