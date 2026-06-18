import pytest
import pytest_asyncio
from uuid import uuid4
from decimal import Decimal
from datetime import date
from sqlalchemy.future import select
from sqlalchemy.orm import selectinload

from app.domains.user.models import User
from app.domains.erp.hr.models import (
    Employee, SalaryTemplate, PayrollLedger, PayrollItem, PayrollDetail
)
from app.domains.erp.hr.service import (
    create_ledger, calculate_item_payroll, get_ledger_item, delete_ledger
)
from app.db.session import AsyncSessionLocal

@pytest_asyncio.fixture
async def db_session():
    async with AsyncSessionLocal() as session:
        yield session

@pytest.mark.asyncio
async def test_payroll_calculation(db_session):
    # 1. Setup temporary test user & employee
    test_user_id = uuid4()
    test_emp_id = uuid4()
    
    test_user = User(
        id=test_user_id,
        email=f"test_payroll_{uuid4().hex[:6]}@example.com",
        password_hash="test",
        full_name="홍길동",
        role="member",
        member_status="active",
        is_active=True
    )
    db_session.add(test_user)
    
    test_employee = Employee(
        id=test_emp_id,
        user_id=test_user_id,
        employee_no=f"test_no_{uuid4().hex[:6]}",
        hire_date=date.today(),
        status="active",
        created_by=test_user_id,
        bank_name="신한은행",
        bank_account="110-482-910243"
    )
    db_session.add(test_employee)
    
    # 2. Setup Salary Template for the employee
    test_template = SalaryTemplate(
        employee_id=test_emp_id,
        base_pay=Decimal("3000000.00"),
        position_pay=Decimal("200000.00"),
        meal_allowance=Decimal("200000.00"),
        car_allowance=Decimal("100000.00"),
        use_national_pension=True,
        use_health_insurance=True,
        use_employment_insurance=True,
        income_tax_rate=100
    )
    db_session.add(test_template)
    await db_session.commit()

    # 3. Create a temporary Payroll Ledger
    # create_ledger will fetch active employees and create items & details automatically
    from app.domains.erp.hr.schemas import PayrollLedgerCreate
    
    ledger_in = PayrollLedgerCreate(
        title="2026년 6월 정기급여",
        pay_year=2026,
        pay_month=6,
        pay_date=date(2026, 6, 25),
        pay_type="정기급여",
        start_date=date(2026, 6, 1),
        end_date=date(2026, 6, 30)
    )
    
    db_ledger = await create_ledger(db_session, ledger_in, creator_id=test_user_id)
    assert db_ledger is not None
    
    # Find the payroll item created for our test employee
    item_query = select(PayrollItem).filter(
        PayrollItem.ledger_id == db_ledger.id,
        PayrollItem.employee_id == test_emp_id
    )
    res = await db_session.execute(item_query)
    payroll_item = res.scalars().first()
    
    assert payroll_item is not None
    
    # Check that calculation was run correctly
    # Gross pay should be 3,500,000
    # Taxable salary = 3,200,000 (meal and car allowances are non-taxable since they are <= 200,000)
    # Pension: 3,200,000 * 0.045 = 144,000
    # Health: 3,200,000 * 0.03545 = 113,440
    # LTC: 113,440 * 0.1295 = 14,690.48 -> 14,690
    # Employment: 3,200,000 * 0.009 = 28,800
    # Income Tax rate: for 3.2M, it's 2%. 3,200,000 * 0.02 = 64,000
    # Local Tax: 64,000 * 0.1 = 6,400
    # Total deduction: 144,000 + 113,440 + 14,690 + 28,800 + 64,000 + 6,400 = 371,330
    # Net pay: 3,500,000 - 371,330 = 3,128,670
    
    assert payroll_item.gross_pay == Decimal("3500000.00")
    assert payroll_item.total_deduction == Decimal("371330.00")
    assert payroll_item.net_pay == Decimal("3128670.00")
    
    # Clean up test data
    await delete_ledger(db_session, db_ledger.id)
    await db_session.delete(test_template)
    await db_session.delete(test_employee)
    await db_session.delete(test_user)
    await db_session.commit()
