import uuid
from sqlalchemy import Column, String, Boolean, DateTime, ForeignKey, Integer, Numeric, Date, Text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.db.base_class import Base

class Department(Base):
    __tablename__ = "erp_departments"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    parent_id = Column(UUID(as_uuid=True), ForeignKey("erp_departments.id", ondelete="SET NULL"), nullable=True)
    name = Column(String(200), nullable=False)
    code = Column(String(50), nullable=True)
    manager_id = Column(UUID(as_uuid=True), ForeignKey("erp_employees.id", ondelete="SET NULL"), nullable=True)
    sort_order = Column(Integer, default=0)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    employees = relationship("Employee", foreign_keys="[Employee.department_id]", back_populates="department")
    manager = relationship("Employee", foreign_keys=[manager_id], post_update=True)


class Employee(Base):
    __tablename__ = "erp_employees"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    employee_no = Column(String(50), nullable=False, unique=True)
    department_id = Column(UUID(as_uuid=True), ForeignKey("erp_departments.id", ondelete="SET NULL"), nullable=True)
    position = Column(String(50), nullable=True)
    job_title = Column(String(100), nullable=True)
    employment_type = Column(String(30), default="full_time")
    hire_date = Column(Date, nullable=False)
    resignation_date = Column(Date, nullable=True)
    status = Column(String(20), default="active")
    phone = Column(String(50), nullable=True)
    email = Column(String(255), nullable=True)
    emergency_contact = Column(String(100), nullable=True)
    emergency_phone = Column(String(50), nullable=True)
    bank_name = Column(String(100), nullable=True)
    bank_account = Column(String(100), nullable=True)
    annual_leave_days = Column(Numeric(4, 1), default=15.0)
    memo = Column(Text, nullable=True)
    created_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    leave_start_date = Column(Date, nullable=True)
    leave_end_date = Column(Date, nullable=True)
    birth_date = Column(Date, nullable=True)

    user = relationship("User", foreign_keys=[user_id])
    creator = relationship("User", foreign_keys=[created_by])
    department = relationship("Department", foreign_keys=[department_id], back_populates="employees")
    attendances = relationship("Attendance", back_populates="employee", cascade="all, delete-orphan")
    leave_requests = relationship("LeaveRequest", foreign_keys="[LeaveRequest.employee_id]", back_populates="employee", cascade="all, delete-orphan")
    leave_balances = relationship("LeaveBalance", back_populates="employee", cascade="all, delete-orphan")
    payrolls = relationship("PayrollItem", back_populates="employee", cascade="all, delete-orphan")
    salary_template = relationship("SalaryTemplate", uselist=False, back_populates="employee", cascade="all, delete-orphan")


class Attendance(Base):
    __tablename__ = "erp_attendance"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    employee_id = Column(UUID(as_uuid=True), ForeignKey("erp_employees.id", ondelete="CASCADE"), nullable=False)
    work_date = Column(Date, nullable=False)
    check_in = Column(DateTime(timezone=True), nullable=True)
    check_out = Column(DateTime(timezone=True), nullable=True)
    work_hours = Column(Numeric(4, 1), default=0.0)
    overtime_hours = Column(Numeric(4, 1), default=0.0)
    status = Column(String(20), default="present")
    memo = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    employee = relationship("Employee", back_populates="attendances")


class LeaveRequest(Base):
    __tablename__ = "erp_leave_requests"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    employee_id = Column(UUID(as_uuid=True), ForeignKey("erp_employees.id", ondelete="CASCADE"), nullable=False)
    leave_type = Column(String(30), nullable=False)
    start_date = Column(Date, nullable=False)
    end_date = Column(Date, nullable=False)
    total_days = Column(Numeric(4, 1), nullable=False)
    reason = Column(Text, nullable=False)
    status = Column(String(20), default="pending")
    approver_id = Column(UUID(as_uuid=True), ForeignKey("erp_employees.id", ondelete="SET NULL"), nullable=True)
    approved_at = Column(DateTime(timezone=True), nullable=True)
    reject_reason = Column(Text, nullable=True)
    linked_item_id = Column(UUID(as_uuid=True), ForeignKey("items.id", ondelete="SET NULL"), nullable=True)
    created_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    employee = relationship("Employee", foreign_keys=[employee_id], back_populates="leave_requests")
    approver = relationship("Employee", foreign_keys=[approver_id])
    creator = relationship("User", foreign_keys=[created_by])


class LeaveBalance(Base):
    __tablename__ = "erp_leave_balances"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    employee_id = Column(UUID(as_uuid=True), ForeignKey("erp_employees.id", ondelete="CASCADE"), nullable=False)
    year = Column(Integer, nullable=False)
    total_days = Column(Numeric(4, 1), nullable=False, default=15.0)
    used_days = Column(Numeric(4, 1), default=0.0)
    remaining_days = Column(Numeric(4, 1), default=15.0)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    employee = relationship("Employee", back_populates="leave_balances")


class PerformanceReview(Base):
    __tablename__ = "erp_performance_reviews"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    employee_id = Column(UUID(as_uuid=True), ForeignKey("erp_employees.id", ondelete="CASCADE"), nullable=False)
    reviewer_id = Column(UUID(as_uuid=True), ForeignKey("erp_employees.id"), nullable=False)
    review_year = Column(Integer, nullable=False)
    review_period = Column(String(20), nullable=False, default="annual")
    ratings = Column(JSONB, nullable=False, default={})
    total_score = Column(Numeric(4, 2), nullable=True)
    comment = Column(Text, nullable=True)
    status = Column(String(20), default="draft")
    submitted_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    employee = relationship("Employee", foreign_keys=[employee_id])
    reviewer = relationship("Employee", foreign_keys=[reviewer_id])


class CommonCode(Base):
    __tablename__ = "erp_codes"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    code_group = Column(String(50), nullable=False)
    code = Column(String(50), nullable=False)
    label = Column(String(100), nullable=False)
    sort_order = Column(Integer, nullable=False, default=0)
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    code_group_name = Column(String(100), nullable=False, default="")


class PayrollLedger(Base):
    __tablename__ = "erp_payroll_ledgers"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    title = Column(String(200), nullable=False)
    pay_year = Column(Integer, nullable=False)
    pay_month = Column(Integer, nullable=False)
    pay_date = Column(Date, nullable=False)
    pay_type = Column(String(50), nullable=False, default="정기급여")
    start_date = Column(Date, nullable=False)
    end_date = Column(Date, nullable=False)
    status = Column(String(20), nullable=False, default="draft")
    created_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    creator = relationship("User", foreign_keys=[created_by])
    items = relationship("PayrollItem", back_populates="ledger", cascade="all, delete-orphan")


class PayrollCode(Base):
    __tablename__ = "erp_payroll_codes"

    code = Column(String(50), primary_key=True)
    name = Column(String(100), nullable=False)
    type = Column(String(20), nullable=False)
    is_taxable = Column(Boolean, default=True)
    tax_free_limit = Column(Numeric(12, 2), default=0.0)
    is_system = Column(Boolean, default=False)
    is_active = Column(Boolean, default=True)
    sort_order = Column(Integer, default=0)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class SalaryTemplate(Base):
    __tablename__ = "erp_salary_templates"

    employee_id = Column(UUID(as_uuid=True), ForeignKey("erp_employees.id", ondelete="CASCADE"), primary_key=True)
    base_pay = Column(Numeric(12, 2), default=0.0)
    position_pay = Column(Numeric(12, 2), default=0.0)
    meal_allowance = Column(Numeric(12, 2), default=200000.0)
    car_allowance = Column(Numeric(12, 2), default=0.0)
    use_national_pension = Column(Boolean, default=True)
    use_health_insurance = Column(Boolean, default=True)
    use_employment_insurance = Column(Boolean, default=True)
    income_tax_rate = Column(Integer, default=100)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    employee = relationship("Employee", back_populates="salary_template")


class PayrollItem(Base):
    __tablename__ = "erp_payroll_items"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    ledger_id = Column(UUID(as_uuid=True), ForeignKey("erp_payroll_ledgers.id", ondelete="CASCADE"), nullable=False)
    employee_id = Column(UUID(as_uuid=True), ForeignKey("erp_employees.id", ondelete="CASCADE"), nullable=False)
    gross_pay = Column(Numeric(12, 2), default=0.0)
    total_deduction = Column(Numeric(12, 2), default=0.0)
    net_pay = Column(Numeric(12, 2), default=0.0)
    bank_name = Column(String(100), nullable=True)
    bank_account = Column(String(100), nullable=True)
    bank_owner = Column(String(100), nullable=True)
    status = Column(String(20), nullable=False, default="draft")
    memo = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    ledger = relationship("PayrollLedger", back_populates="items")
    employee = relationship("Employee", back_populates="payrolls")
    details = relationship("PayrollDetail", back_populates="payroll_item", cascade="all, delete-orphan")


class PayrollDetail(Base):
    __tablename__ = "erp_payroll_details"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    payroll_item_id = Column(UUID(as_uuid=True), ForeignKey("erp_payroll_items.id", ondelete="CASCADE"), nullable=False)
    code = Column(String(50), ForeignKey("erp_payroll_codes.code"), nullable=False)
    amount = Column(Numeric(12, 2), default=0.0)
    is_taxable = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    payroll_item = relationship("PayrollItem", back_populates="details")
    payroll_code = relationship("PayrollCode")
