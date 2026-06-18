from typing import List, Optional, Dict, Any
from uuid import UUID, uuid4
from datetime import date, datetime
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy import func, update, delete, or_
from sqlalchemy.orm import selectinload
from decimal import Decimal

from app.domains.erp.hr.models import (
    Department, Employee, Attendance, LeaveRequest, LeaveBalance,
    PerformanceReview, CommonCode, PayrollLedger, PayrollCode,
    SalaryTemplate, PayrollItem, PayrollDetail
)
from app.domains.erp.hr.schemas import (
    EmployeeCreate, EmployeeUpdate, DepartmentCreate, DepartmentUpdate,
    AttendanceCreate, AttendanceUpdate, LeaveRequestCreate, LeaveRequestUpdate,
    LeaveBalanceCreate, PerformanceReviewCreate, PerformanceReviewUpdate,
    CommonCodeCreate, PayrollLedgerCreate
)
from app.domains.user.models import User

# =============================================================
# ── 1. 부서 (Department) 서비스                             ──
# =============================================================

async def get_all_departments(db: AsyncSession) -> List[Department]:
    result = await db.execute(
        select(Department, User.full_name.label("manager_name"))
        .outerjoin(Employee, Department.manager_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .order_by(Department.sort_order, Department.name)
    )
    depts = []
    for row in result.all():
        dept, manager_name = row[0], row[1]
        dept.manager_name = manager_name
        depts.append(dept)
    return depts

async def get_department(db: AsyncSession, id: UUID) -> Optional[Department]:
    result = await db.execute(
        select(Department, User.full_name.label("manager_name"))
        .outerjoin(Employee, Department.manager_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .filter(Department.id == id)
    )
    row = result.first()
    if row:
        dept, manager_name = row[0], row[1]
        dept.manager_name = manager_name
        return dept
    return None

async def create_department(db: AsyncSession, department_in: DepartmentCreate) -> Department:
    db_obj = Department(
        id=uuid4(),
        parent_id=department_in.parent_id,
        name=department_in.name,
        code=department_in.code,
        manager_id=department_in.manager_id,
        sort_order=department_in.sort_order,
        is_active=department_in.is_active
    )
    db.add(db_obj)
    await db.commit()
    return await get_department(db, db_obj.id)

async def update_department(db: AsyncSession, id: UUID, department_in: DepartmentUpdate) -> Optional[Department]:
    db_obj = await get_department(db, id)
    if not db_obj:
        return None
    update_data = department_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    return await get_department(db, id)

async def delete_department(db: AsyncSession, id: UUID) -> bool:
    db_obj = await get_department(db, id)
    if not db_obj:
        raise ValueError("Department not found")
    # 하위 부서가 존재하는지 검사
    child_res = await db.execute(select(Department).filter(Department.parent_id == id))
    if child_res.scalars().first():
        raise ValueError("Cannot delete department with child departments")
    await db.delete(db_obj)
    await db.commit()
    return True

async def get_department_tree(db: AsyncSession) -> List[Department]:
    all_depts = await get_all_departments(db)
    if not all_depts:
        return []
        
    # Map id -> Department
    dept_map = {dept.id: dept for dept in all_depts}
    for dept in all_depts:
        dept.children = []
        
    roots = []
    for dept in all_depts:
        if dept.parent_id is None:
            roots.append(dept)
        else:
            parent = dept_map.get(dept.parent_id)
            if parent:
                parent.children.append(dept)
                
    # Sort roots and children by sort_order
    roots.sort(key=lambda x: x.sort_order)
    for dept in all_depts:
        dept.children.sort(key=lambda x: x.sort_order)
        
    return roots


# =============================================================
# ── 2. 직원 (Employee) 서비스                             ──
# =============================================================

async def get_employees(
    db: AsyncSession,
    department_id: Optional[UUID] = None,
    status: Optional[str] = None,
    search: Optional[str] = None,
    offset: int = 0,
    limit: int = 20
) -> Dict[str, Any]:
    query = (
        select(
            Employee,
            User.full_name.label("employee_name"),
            Department.name.label("department_name")
        )
        .outerjoin(User, Employee.user_id == User.id)
        .outerjoin(Department, Employee.department_id == Department.id)
    )
    
    if department_id:
        query = query.filter(Employee.department_id == department_id)
    if status:
        query = query.filter(Employee.status == status)
    if search:
        query = query.filter(
            or_(
                Employee.employee_no.like(f"%{search}%"),
                User.full_name.like(f"%{search}%")
            )
        )
        
    # Get total count first
    count_query = select(func.count()).select_from(query.subquery())
    total_res = await db.execute(count_query)
    total = total_res.scalar() or 0
    
    # Fetch paginated items
    query = query.order_by(Employee.employee_no).offset(offset).limit(limit)
    result = await db.execute(query)
    
    items = []
    for row in result.all():
        emp, emp_name, dept_name = row[0], row[1], row[2]
        emp.employee_name = emp_name
        emp.department_name = dept_name
        items.append(emp)
        
    return {
        "items": items,
        "total": total
    }

async def get_employee(db: AsyncSession, id: UUID) -> Optional[Employee]:
    query = (
        select(
            Employee,
            User.full_name.label("employee_name"),
            Department.name.label("department_name")
        )
        .outerjoin(User, Employee.user_id == User.id)
        .outerjoin(Department, Employee.department_id == Department.id)
        .filter(Employee.id == id)
    )
    result = await db.execute(query)
    row = result.first()
    if row:
        emp, emp_name, dept_name = row[0], row[1], row[2]
        emp.employee_name = emp_name
        emp.department_name = dept_name
        return emp
    return None

async def get_employee_by_user(db: AsyncSession, user_id: UUID) -> Optional[Employee]:
    query = (
        select(
            Employee,
            User.full_name.label("employee_name"),
            Department.name.label("department_name")
        )
        .outerjoin(User, Employee.user_id == User.id)
        .outerjoin(Department, Employee.department_id == Department.id)
        .filter(Employee.user_id == user_id)
        .limit(1)
    )
    result = await db.execute(query)
    row = result.first()
    if row:
        emp, emp_name, dept_name = row[0], row[1], row[2]
        emp.employee_name = emp_name
        emp.department_name = dept_name
        return emp
    return None

async def create_employee(db: AsyncSession, employee_in: EmployeeCreate, creator_id: UUID) -> Employee:
    # 사번이 없는 경우 자동 채번 가동 (YYMMDD + 3자리 순번)
    emp_no = employee_in.employee_no
    if not emp_no:
        yymmdd = date.today().strftime("%y%m%d")
        result = await db.execute(
            select(Employee.employee_no)
            .filter(Employee.employee_no.like(f"{yymmdd}%"))
            .order_by(Employee.employee_no.desc())
            .limit(1)
        )
        last_no = result.scalar()
        if last_no and len(last_no) >= 9:
            try:
                seq = int(last_no[6:]) + 1
                emp_no = f"{yymmdd}{seq:03d}"
            except ValueError:
                emp_no = f"{yymmdd}001"
        else:
            emp_no = f"{yymmdd}001"
            
    db_obj = Employee(
        id=uuid4(),
        user_id=employee_in.user_id,
        employee_no=emp_no,
        department_id=employee_in.department_id,
        position=employee_in.position,
        job_title=employee_in.job_title,
        employment_type=employee_in.employment_type,
        hire_date=employee_in.hire_date,
        resignation_date=employee_in.resignation_date,
        status=employee_in.status,
        phone=employee_in.phone,
        email=employee_in.email,
        emergency_contact=employee_in.emergency_contact,
        emergency_phone=employee_in.emergency_phone,
        bank_name=employee_in.bank_name,
        bank_account=employee_in.bank_account,
        annual_leave_days=employee_in.annual_leave_days,
        memo=employee_in.memo,
        leave_start_date=employee_in.leave_start_date,
        leave_end_date=employee_in.leave_end_date,
        birth_date=employee_in.birth_date,
        created_by=creator_id
    )
    db.add(db_obj)
    await db.commit()
    return await get_employee(db, db_obj.id)

async def update_employee(db: AsyncSession, id: UUID, employee_in: EmployeeUpdate) -> Optional[Employee]:
    db_obj = await get_employee(db, id)
    if not db_obj:
        return None
    update_data = employee_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    return await get_employee(db, id)

async def update_employee_status(db: AsyncSession, id: UUID, status: str) -> None:
    await db.execute(
        update(Employee)
        .where(Employee.id == id)
        .values(status=status, updated_at=func.now())
    )
    await db.commit()

async def delete_employee(db: AsyncSession, id: UUID) -> bool:
    db_obj = await get_employee(db, id)
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True


# =============================================================
# ── 3. 근태 (Attendance) 서비스                              ──
# =============================================================

async def get_attendances_by_employee(db: AsyncSession, employee_id: UUID) -> List[Attendance]:
    result = await db.execute(
        select(Attendance, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, Attendance.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .filter(Attendance.employee_id == employee_id)
        .order_by(Attendance.work_date.desc())
    )
    attendances = []
    for row in result.all():
        att, emp_name, emp_no = row[0], row[1], row[2]
        att.employee_name = emp_name
        att.employee_no = emp_no
        attendances.append(att)
    return attendances

async def create_attendance(db: AsyncSession, attendance_in: AttendanceCreate) -> Attendance:
    db_obj = Attendance(
        id=uuid4(),
        employee_id=attendance_in.employee_id,
        work_date=attendance_in.work_date,
        check_in=attendance_in.check_in,
        check_out=attendance_in.check_out,
        work_hours=attendance_in.work_hours,
        overtime_hours=attendance_in.overtime_hours,
        status=attendance_in.status,
        memo=attendance_in.memo
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def update_attendance(db: AsyncSession, id: UUID, attendance_in: AttendanceUpdate) -> Optional[Attendance]:
    result = await db.execute(select(Attendance).filter(Attendance.id == id))
    db_obj = result.scalars().first()
    if not db_obj:
        return None
    update_data = attendance_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def get_attendance(
    db: AsyncSession,
    employee_id: Optional[UUID] = None,
    from_date: Optional[date] = None,
    to_date: Optional[date] = None,
    status: Optional[str] = None
) -> List[Attendance]:
    query = (
        select(Attendance, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, Attendance.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
    )
    if employee_id:
        query = query.filter(Attendance.employee_id == employee_id)
    if from_date:
        query = query.filter(Attendance.work_date >= from_date)
    if to_date:
        query = query.filter(Attendance.work_date <= to_date)
    if status:
        query = query.filter(Attendance.status == status)
        
    query = query.order_by(Attendance.work_date.desc(), Employee.employee_no.asc())
    result = await db.execute(query)
    
    attendances = []
    for row in result.all():
        att, emp_name, emp_no = row[0], row[1], row[2]
        att.employee_name = emp_name
        att.employee_no = emp_no
        attendances.append(att)
    return attendances

async def find_by_employee_and_date(db: AsyncSession, employee_id: UUID, work_date: date) -> Optional[Attendance]:
    result = await db.execute(
        select(Attendance, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, Attendance.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .filter(Attendance.employee_id == employee_id, Attendance.work_date == work_date)
        .limit(1)
    )
    row = result.first()
    if row:
        att, emp_name, emp_no = row[0], row[1], row[2]
        att.employee_name = emp_name
        att.employee_no = emp_no
        return att
    return None

async def check_in(db: AsyncSession, employee_id: UUID) -> Attendance:
    today = date.today()
    existing = await find_by_employee_and_date(db, employee_id, today)
    if existing:
        raise ValueError("Already checked in today")
        
    db_obj = Attendance(
        id=uuid4(),
        employee_id=employee_id,
        work_date=today,
        check_in=datetime.now(),
        status="present",
        work_hours=Decimal("0.0"),
        overtime_hours=Decimal("0.0")
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    
    result = await db.execute(
        select(User.full_name, Employee.employee_no)
        .join(Employee, User.id == Employee.user_id)
        .filter(Employee.id == employee_id)
    )
    row = result.first()
    if row:
        db_obj.employee_name = row[0]
        db_obj.employee_no = row[1]
        
    return db_obj

async def check_out(db: AsyncSession, employee_id: UUID) -> Attendance:
    today = date.today()
    db_obj = await find_by_employee_and_date(db, employee_id, today)
    if not db_obj:
        raise ValueError("No check-in record found for today")
    if db_obj.check_out is not None:
        raise ValueError("Already checked out today")
        
    now = datetime.now()
    db_obj.check_out = now
    
    if db_obj.check_in:
        duration = now - db_obj.check_in
        minutes = duration.total_seconds() / 60.0
        work_hours = Decimal(f"{minutes / 60.0:.1f}")
        db_obj.work_hours = work_hours
        
        if work_hours > Decimal("8.0"):
            db_obj.overtime_hours = work_hours - Decimal("8.0")
        else:
            db_obj.overtime_hours = Decimal("0.0")
            
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def get_attendance_by_id(db: AsyncSession, id: UUID) -> Optional[Attendance]:
    result = await db.execute(
        select(Attendance, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, Attendance.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .filter(Attendance.id == id)
    )
    row = result.first()
    if row:
        att, emp_name, emp_no = row[0], row[1], row[2]
        att.employee_name = emp_name
        att.employee_no = emp_no
        return att
    return None

async def delete_attendance(db: AsyncSession, id: UUID) -> bool:
    result = await db.execute(select(Attendance).filter(Attendance.id == id))
    db_obj = result.scalars().first()
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True


# =============================================================
# ── 4. 휴가 (Leave Requests & Balances) 서비스                ──
# =============================================================

async def get_leaves_by_employee(db: AsyncSession, employee_id: UUID, status: Optional[str] = None) -> List[LeaveRequest]:
    query = (
        select(LeaveRequest, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, LeaveRequest.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .filter(LeaveRequest.employee_id == employee_id)
    )
    if status:
        query = query.filter(LeaveRequest.status == status)
    query = query.order_by(LeaveRequest.start_date.desc())
    result = await db.execute(query)
    leaves = []
    for row in result.all():
        lr, emp_name, emp_no = row[0], row[1], row[2]
        lr.employee_name = emp_name
        lr.employee_no = emp_no
        leaves.append(lr)
    return leaves

async def get_leaves_by_manager(db: AsyncSession, manager_id: UUID, status: Optional[str] = None) -> List[LeaveRequest]:
    # 부서장 소속의 모든 사원들의 휴가 신청 목록
    sub_depts = await db.execute(
        select(Department.id).filter(Department.manager_id == manager_id)
    )
    dept_ids = sub_depts.scalars().all()
    if not dept_ids:
        return []
        
    query = (
        select(LeaveRequest, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, LeaveRequest.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .filter(Employee.department_id.in_(dept_ids))
    )
    if status:
        query = query.filter(LeaveRequest.status == status)
    query = query.order_by(LeaveRequest.start_date.desc())
    result = await db.execute(query)
    leaves = []
    for row in result.all():
        lr, emp_name, emp_no = row[0], row[1], row[2]
        lr.employee_name = emp_name
        lr.employee_no = emp_no
        leaves.append(lr)
    return leaves

async def get_leave(db: AsyncSession, id: UUID) -> Optional[LeaveRequest]:
    query = (
        select(LeaveRequest, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, LeaveRequest.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .filter(LeaveRequest.id == id)
    )
    result = await db.execute(query)
    row = result.first()
    if row:
        lr, emp_name, emp_no = row[0], row[1], row[2]
        lr.employee_name = emp_name
        lr.employee_no = emp_no
        return lr
    return None

async def create_leave(db: AsyncSession, leave_in: LeaveRequestCreate, creator_id: UUID) -> LeaveRequest:
    db_obj = LeaveRequest(
        id=uuid4(),
        employee_id=leave_in.employee_id,
        leave_type=leave_in.leave_type,
        start_date=leave_in.start_date,
        end_date=leave_in.end_date,
        total_days=leave_in.total_days,
        reason=leave_in.reason,
        status="pending",
        linked_item_id=leave_in.linked_item_id,
        created_by=creator_id
    )
    db.add(db_obj)
    await db.commit()
    return await get_leave(db, db_obj.id)

async def update_leave(db: AsyncSession, id: UUID, leave_in: LeaveRequestUpdate) -> Optional[LeaveRequest]:
    db_obj = await get_leave(db, id)
    if not db_obj:
        return None
    update_data = leave_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    return await get_leave(db, id)

async def approve_leave(db: AsyncSession, id: UUID, approver_id: UUID) -> None:
    db_obj = await get_leave(db, id)
    if not db_obj:
        raise ValueError("Leave request not found")
    if db_obj.status != "pending":
        raise ValueError("Only pending requests can be approved")
        
    db_obj.status = "approved"
    db_obj.approver_id = approver_id
    db_obj.approved_at = func.now()
    db.add(db_obj)
    
    # Update leave balance
    await update_leave_balance(db, db_obj.employee_id, date.today().year, db_obj.total_days, is_restore=False)
    await db.commit()

async def reject_leave(db: AsyncSession, id: UUID, approver_id: UUID, reject_reason: str) -> None:
    db_obj = await get_leave(db, id)
    if not db_obj:
        raise ValueError("Leave request not found")
    if db_obj.status != "pending":
        raise ValueError("Only pending requests can be rejected")
        
    db_obj.status = "rejected"
    db_obj.approver_id = approver_id
    db_obj.approved_at = func.now()
    db_obj.reject_reason = reject_reason
    db.add(db_obj)
    await db.commit()

async def cancel_leave(db: AsyncSession, id: UUID) -> None:
    db_obj = await get_leave(db, id)
    if not db_obj:
        raise ValueError("Leave request not found")
    
    old_status = db_obj.status
    db_obj.status = "cancelled"
    db.add(db_obj)
    
    if old_status == "approved":
        # Restore leave balance
        await update_leave_balance(db, db_obj.employee_id, date.today().year, db_obj.total_days, is_restore=True)
        
    await db.commit()

async def delete_leave(db: AsyncSession, id: UUID) -> bool:
    db_obj = await get_leave(db, id)
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True

async def get_leave_balance(db: AsyncSession, employee_id: UUID, year: int) -> LeaveBalance:
    result = await db.execute(
        select(LeaveBalance).filter(LeaveBalance.employee_id == employee_id, LeaveBalance.year == year)
    )
    balance = result.scalars().first()
    if not balance:
        balance = LeaveBalance(
            id=uuid4(),
            employee_id=employee_id,
            year=year,
            total_days=Decimal("15.0"),
            used_days=Decimal("0.0"),
            remaining_days=Decimal("15.0")
        )
        db.add(balance)
        await db.commit()
        await db.refresh(balance)
    return balance

async def update_leave_balance(db: AsyncSession, employee_id: UUID, year: int, days: Decimal, is_restore: bool) -> None:
    balance = await get_leave_balance(db, employee_id, year)
    if is_restore:
        balance.used_days = balance.used_days - days
    else:
        balance.used_days = balance.used_days + days
    balance.remaining_days = balance.total_days - balance.used_days
    db.add(balance)


# =============================================================
# ── 5. 공통 코드 (Common Code) 서비스                          ──
# =============================================================

async def get_groups(db: AsyncSession) -> List[Dict[str, str]]:
    result = await db.execute(
        select(CommonCode.code_group, CommonCode.code_group_name).distinct()
    )
    return [{"codeGroup": row[0], "codeGroupName": row[1]} for row in result.all()]

async def get_codes(db: AsyncSession, code_group: str) -> List[CommonCode]:
    result = await db.execute(
        select(CommonCode).filter(CommonCode.code_group == code_group, CommonCode.is_active == True).order_by(CommonCode.sort_order)
    )
    return list(result.scalars().all())

async def get_all_codes(db: AsyncSession) -> List[CommonCode]:
    result = await db.execute(
        select(CommonCode).order_by(CommonCode.code_group, CommonCode.sort_order)
    )
    return list(result.scalars().all())

async def get_code(db: AsyncSession, id: UUID) -> Optional[CommonCode]:
    result = await db.execute(select(CommonCode).filter(CommonCode.id == id))
    return result.scalars().first()

async def create_code(db: AsyncSession, code_in: CommonCodeCreate) -> CommonCode:
    # 중복 검사
    dup = await db.execute(
        select(CommonCode).filter(CommonCode.code_group == code_in.code_group, CommonCode.code == code_in.code)
    )
    if dup.scalars().first():
        raise ValueError(f"Code '{code_in.code}' already exists in group '{code_in.code_group}'")
        
    db_obj = CommonCode(
        id=uuid4(),
        code_group=code_in.code_group,
        code=code_in.code,
        label=code_in.label,
        sort_order=code_in.sort_order,
        is_active=code_in.is_active,
        code_group_name=code_in.code_group_name
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def update_code(db: AsyncSession, id: UUID, code_in: CommonCodeCreate) -> Optional[CommonCode]:
    db_obj = await get_code(db, id)
    if not db_obj:
        return None
    # 중복 검사 (본인 제외)
    dup = await db.execute(
        select(CommonCode).filter(
            CommonCode.code_group == db_obj.code_group,
            CommonCode.code == code_in.code,
            CommonCode.id != id
        )
    )
    if dup.scalars().first():
        raise ValueError(f"Code '{code_in.code}' already exists in group '{db_obj.code_group}'")
        
    update_data = code_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def delete_code(db: AsyncSession, id: UUID) -> bool:
    db_obj = await get_code(db, id)
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True


# =============================================================
# ── 6. 인사 평가 (Performance Review) 서비스                  ──
# =============================================================

async def get_reviews_by_employee(db: AsyncSession, employee_id: UUID) -> List[PerformanceReview]:
    result = await db.execute(
        select(PerformanceReview, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, PerformanceReview.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .filter(PerformanceReview.employee_id == employee_id)
        .order_by(PerformanceReview.review_year.desc())
    )
    reviews = []
    for row in result.all():
        pr, emp_name, emp_no = row[0], row[1], row[2]
        pr.employee_name = emp_name
        pr.employee_no = emp_no
        reviews.append(pr)
    return reviews

async def get_reviews_by_reviewer(db: AsyncSession, reviewer_id: UUID) -> List[PerformanceReview]:
    result = await db.execute(
        select(PerformanceReview, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, PerformanceReview.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .filter(PerformanceReview.reviewer_id == reviewer_id)
        .order_by(PerformanceReview.review_year.desc())
    )
    reviews = []
    for row in result.all():
        pr, emp_name, emp_no = row[0], row[1], row[2]
        pr.employee_name = emp_name
        pr.employee_no = emp_no
        reviews.append(pr)
    return reviews

async def get_review(db: AsyncSession, id: UUID) -> Optional[PerformanceReview]:
    result = await db.execute(
        select(PerformanceReview, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, PerformanceReview.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .filter(PerformanceReview.id == id)
    )
    row = result.first()
    if row:
        pr, emp_name, emp_no = row[0], row[1], row[2]
        pr.employee_name = emp_name
        pr.employee_no = emp_no
        return pr
    return None

async def create_review(db: AsyncSession, review_in: PerformanceReviewCreate) -> PerformanceReview:
    db_obj = PerformanceReview(
        id=uuid4(),
        employee_id=review_in.employee_id,
        reviewer_id=review_in.reviewer_id,
        review_year=review_in.review_year,
        review_period=review_in.review_period,
        ratings=review_in.ratings,
        total_score=review_in.total_score,
        comment=review_in.comment,
        status="draft"
    )
    db.add(db_obj)
    await db.commit()
    return await get_review(db, db_obj.id)

async def update_review(db: AsyncSession, id: UUID, review_in: PerformanceReviewUpdate) -> Optional[PerformanceReview]:
    db_obj = await get_review(db, id)
    if not db_obj:
        return None
    update_data = review_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    return await get_review(db, id)

async def submit_review(db: AsyncSession, id: UUID) -> Optional[PerformanceReview]:
    db_obj = await get_review(db, id)
    if not db_obj:
        return None
    db_obj.status = "submitted"
    db_obj.submitted_at = func.now()
    db.add(db_obj)
    await db.commit()
    return await get_review(db, id)

async def delete_review(db: AsyncSession, id: UUID) -> bool:
    db_obj = await get_review(db, id)
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True


# =============================================================
# ── 7. 급여 및 급여대장 (Payroll & Payroll Ledger) 서비스       ──
# =============================================================

def safe_decimal(v: Optional[Decimal]) -> Decimal:
    return v if v is not None else Decimal("0.0")

async def get_ledgers(
    db: AsyncSession,
    pay_year: Optional[int] = None,
    pay_month: Optional[int] = None,
    status: Optional[str] = None
) -> List[PayrollLedger]:
    query = select(PayrollLedger).options(selectinload(PayrollLedger.items))
    if pay_year:
        query = query.filter(PayrollLedger.pay_year == pay_year)
    if pay_month:
        query = query.filter(PayrollLedger.pay_month == pay_month)
    if status:
        query = query.filter(PayrollLedger.status == status)
    query = query.order_by(PayrollLedger.pay_year.desc(), PayrollLedger.pay_month.desc())
    
    result = await db.execute(query)
    ledgers = list(result.scalars().all())
    
    # 각 대장의 통계값 계산
    for ledger in ledgers:
        total_gross = Decimal("0.0")
        total_deduction = Decimal("0.0")
        total_net = Decimal("0.0")
        for item in ledger.items:
            total_gross += safe_decimal(item.gross_pay)
            total_deduction += safe_decimal(item.total_deduction)
            total_net += safe_decimal(item.net_pay)
        ledger.total_gross = total_gross
        ledger.total_deduction = total_deduction
        ledger.total_net = total_net
        ledger.employee_count = len(ledger.items)
        
    return ledgers

async def get_ledger(db: AsyncSession, id: UUID) -> Optional[PayrollLedger]:
    query = select(PayrollLedger).filter(PayrollLedger.id == id).options(selectinload(PayrollLedger.items))
    result = await db.execute(query)
    ledger = result.scalars().first()
    if ledger:
        total_gross = Decimal("0.0")
        total_deduction = Decimal("0.0")
        total_net = Decimal("0.0")
        for item in ledger.items:
            total_gross += safe_decimal(item.gross_pay)
            total_deduction += safe_decimal(item.total_deduction)
            total_net += safe_decimal(item.net_pay)
        ledger.total_gross = total_gross
        ledger.total_deduction = total_deduction
        ledger.total_net = total_net
        ledger.employee_count = len(ledger.items)
        return ledger
    return None

async def create_ledger(db: AsyncSession, ledger_in: PayrollLedgerCreate, creator_id: UUID) -> PayrollLedger:
    ledger_id = uuid4()
    db_ledger = PayrollLedger(
        id=ledger_id,
        title=ledger_in.title,
        pay_year=ledger_in.pay_year,
        pay_month=ledger_in.pay_month,
        pay_date=ledger_in.pay_date,
        pay_type=ledger_in.pay_type or "정기급여",
        start_date=ledger_in.start_date,
        end_date=ledger_in.end_date,
        status="draft",
        created_by=creator_id
    )
    db.add(db_ledger)
    
    # 활성 상태 사원들 조회
    emp_query = select(Employee).filter(Employee.status == "active")
    emp_result = await db.execute(emp_query)
    active_employees = emp_result.scalars().all()
    
    for emp in active_employees:
        # 사원의 급여 템플릿 로드
        temp_query = select(SalaryTemplate).filter(SalaryTemplate.employee_id == emp.id)
        temp_res = await db.execute(temp_query)
        temp = temp_res.scalars().first()
        
        if not temp:
            # 템플릿이 없을 경우 기본값 생성하여 저장
            temp = SalaryTemplate(
                employee_id=emp.id,
                base_pay=Decimal("3000000.00"),
                position_pay=Decimal("200000.00"),
                meal_allowance=Decimal("200000.00"),
                car_allowance=Decimal("100000.00"),
                use_national_pension=True,
                use_health_insurance=True,
                use_employment_insurance=True,
                income_tax_rate=100
            )
            db.add(temp)
            
        # 사원명 가져오기 (Employee.user 관계 등)
        user_query = select(User.full_name).filter(User.id == emp.user_id)
        user_res = await db.execute(user_query)
        emp_name = user_res.scalar() or emp.employee_no
        
        # Payroll Item 생성
        item_id = uuid4()
        db_item = PayrollItem(
            id=item_id,
            ledger_id=ledger_id,
            employee_id=emp.id,
            gross_pay=Decimal("0.0"),
            total_deduction=Decimal("0.0"),
            net_pay=Decimal("0.0"),
            bank_name=emp.bank_name or "신한은행",
            bank_account=emp.bank_account or "110-482-910243",
            bank_owner=emp_name,
            status="draft",
            memo=""
        )
        db.add(db_item)
        
        # 기본 수당/공제 상세 항목(Detail) 12개 생성
        allowances = [
            ("BASE", "기본급", True, temp.base_pay),
            ("POSITION", "직책수당", True, temp.position_pay),
            ("MEAL", "식대", False, temp.meal_allowance),
            ("CAR", "자가운전보조금", False, temp.car_allowance),
            ("OVERTIME", "연장근로수당", True, Decimal("0.0")),
            ("BONUS", "성과상여금", True, Decimal("0.0"))
        ]
        
        deductions = [
            ("PENSION", "국민연금", False, Decimal("0.0")),
            ("HEALTH", "건강보험", False, Decimal("0.0")),
            ("LTC", "장기요양보험", False, Decimal("0.0")),
            ("EMP", "고용보험", False, Decimal("0.0")),
            ("INCOME_TAX", "소득세", False, Decimal("0.0")),
            ("LOCAL_TAX", "지방소득세", False, Decimal("0.0"))
        ]
        
        for code, name, taxable, amt in allowances + deductions:
            db_detail = PayrollDetail(
                id=uuid4(),
                payroll_item_id=item_id,
                code=code,
                amount=amt,
                is_taxable=taxable
            )
            db.add(db_detail)
            
    await db.commit()
    
    # 생성된 모든 사원 계산 돌리기
    db_ledger = await get_ledger(db, ledger_id)
    if db_ledger:
        for item in db_ledger.items:
            temp_res = await db.execute(select(SalaryTemplate).filter(SalaryTemplate.employee_id == item.employee_id))
            temp = temp_res.scalars().first()
            await calculate_item_payroll(db, item.id, temp)
            
    return await get_ledger(db, ledger_id)

async def delete_ledger(db: AsyncSession, id: UUID) -> bool:
    db_obj = await get_ledger(db, id)
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True

async def confirm_ledger(db: AsyncSession, id: UUID) -> None:
    db_obj = await get_ledger(db, id)
    if not db_obj:
        raise ValueError("Payroll ledger not found")
    db_obj.status = "confirmed"
    db_obj.updated_at = func.now()
    db.add(db_obj)
    await db.commit()

async def pay_ledger(db: AsyncSession, id: UUID) -> None:
    db_obj = await get_ledger(db, id)
    if not db_obj:
        raise ValueError("Payroll ledger not found")
    db_obj.status = "paid"
    db_obj.updated_at = func.now()
    db.add(db_obj)
    await db.commit()

async def get_ledger_items(db: AsyncSession, ledger_id: UUID) -> List[PayrollItem]:
    query = (
        select(
            PayrollItem,
            User.full_name.label("employee_name"),
            Employee.employee_no.label("employee_no"),
            Department.name.label("department_name"),
            Employee.position.label("position")
        )
        .join(Employee, PayrollItem.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .outerjoin(Department, Employee.department_id == Department.id)
        .filter(PayrollItem.ledger_id == ledger_id)
        .options(selectinload(PayrollItem.details))
    )
    result = await db.execute(query)
    items = []
    for row in result.all():
        item = row[0]
        item.employee_name = row[1]
        item.employee_no = row[2]
        item.department_name = row[3]
        item.position = row[4]
        items.append(item)
    return items

async def get_ledger_item(db: AsyncSession, id: UUID) -> Optional[PayrollItem]:
    query = (
        select(
            PayrollItem,
            User.full_name.label("employee_name"),
            Employee.employee_no.label("employee_no"),
            Department.name.label("department_name"),
            Employee.position.label("position")
        )
        .join(Employee, PayrollItem.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .outerjoin(Department, Employee.department_id == Department.id)
        .filter(PayrollItem.id == id)
        .options(selectinload(PayrollItem.details))
    )
    result = await db.execute(query)
    row = result.first()
    if row:
        item = row[0]
        item.employee_name = row[1]
        item.employee_no = row[2]
        item.department_name = row[3]
        item.position = row[4]
        return item
    return None

async def update_ledger_item_details(db: AsyncSession, item_id: UUID, item_in: Any) -> Optional[PayrollItem]:
    db_item = await get_ledger_item(db, item_id)
    if not db_item:
        return None
        
    db_item.bank_name = item_in.bank_name
    db_item.bank_account = item_in.bank_account
    db_item.bank_owner = item_in.bank_owner
    db_item.memo = item_in.memo
    db.add(db_item)
    
    if item_in.details:
        for det_in in item_in.details:
            # 기존 detail이 있는지 확인
            det_query = select(PayrollDetail).filter(
                PayrollDetail.payroll_item_id == item_id,
                PayrollDetail.code == det_in.code
            )
            det_res = await db.execute(det_query)
            db_det = det_res.scalars().first()
            
            if db_det:
                db_det.amount = det_in.amount
                db.add(db_det)
            else:
                db_det = PayrollDetail(
                    id=uuid4(),
                    payroll_item_id=item_id,
                    code=det_in.code,
                    amount=det_in.amount,
                    is_taxable=det_in.is_taxable
                )
                db.add(db_det)
                
    await db.commit()
    
    # 다시 세금 계산 가동
    temp_res = await db.execute(select(SalaryTemplate).filter(SalaryTemplate.employee_id == db_item.employee_id))
    temp = temp_res.scalars().first()
    await calculate_item_payroll(db, item_id, temp)
    
    return await get_ledger_item(db, item_id)

async def calculate_item_payroll(db: AsyncSession, item_id: UUID, temp: Optional[SalaryTemplate]) -> None:
    # 1) 해당 Item의 detail 리스트 조회
    det_query = select(PayrollDetail).filter(PayrollDetail.payroll_item_id == item_id)
    det_res = await db.execute(det_query)
    details = det_res.scalars().all()
    
    gross_pay = Decimal("0.0")
    taxable_salary = Decimal("0.0")
    
    details_map = {det.code: det for det in details}
    
    # 과세/비과세 분류
    for det in details:
        # allowance인지 확인 (공제 code 제외: PENSION, HEALTH, LTC, EMP, INCOME_TAX, LOCAL_TAX)
        if det.code not in ["PENSION", "HEALTH", "LTC", "EMP", "INCOME_TAX", "LOCAL_TAX"]:
            amt = safe_decimal(det.amount)
            gross_pay += amt
            
            if det.is_taxable:
                taxable_salary += amt
            else:
                # 식대(MEAL), 자가운전(CAR) 한도 초과분 과세로 전환
                if det.code == "MEAL" and amt > Decimal("200000.0"):
                    taxable_salary += (amt - Decimal("200000.0"))
                elif det.code == "CAR" and amt > Decimal("200000.0"):
                    taxable_salary += (amt - Decimal("200000.0"))

    if not temp:
        temp = SalaryTemplate(
            use_national_pension=True,
            use_health_insurance=True,
            use_employment_insurance=True,
            income_tax_rate=100
        )
        
    pension = Decimal("0.0")
    health = Decimal("0.0")
    ltc = Decimal("0.0")
    employment = Decimal("0.0")
    income_tax = Decimal("0.0")
    local_tax = Decimal("0.0")
    
    # 2) 4대보험 연산 (10원 미만 절사)
    if temp.use_national_pension:
        pension = (taxable_salary * Decimal("0.045")) // Decimal("10.0") * Decimal("10.0")
    if temp.use_health_insurance:
        health = (taxable_salary * Decimal("0.03545")) // Decimal("10.0") * Decimal("10.0")
        ltc = (health * Decimal("0.1295")) // Decimal("10.0") * Decimal("10.0")
    if temp.use_employment_insurance:
        employment = (taxable_salary * Decimal("0.009")) // Decimal("10.0") * Decimal("10.0")
        
    # 3) 소득세 (간이세액 누진 요율 구간)
    rate = Decimal("0.0")
    if taxable_salary > Decimal("10000000.0"):
        rate = Decimal("0.22")
    elif taxable_salary > Decimal("6000000.0"):
        rate = Decimal("0.14")
    elif taxable_salary > Decimal("4000000.0"):
        rate = Decimal("0.07")
    elif taxable_salary > Decimal("2000000.0"):
        rate = Decimal("0.02")
    else:
        rate = Decimal("0.004")
        
    raw_tax = taxable_salary * rate
    tax_scale = Decimal(str(temp.income_tax_rate or 100)) / Decimal("100.0")
    raw_tax = raw_tax * tax_scale
    
    income_tax = raw_tax // Decimal("10.0") * Decimal("10.0")
    local_tax = (income_tax * Decimal("0.1")) // Decimal("10.0") * Decimal("10.0")
    
    # 4) DB 업데이트
    deduction_values = {
        "PENSION": pension,
        "HEALTH": health,
        "LTC": ltc,
        "EMP": employment,
        "INCOME_TAX": income_tax,
        "LOCAL_TAX": local_tax
    }
    
    for code, amt in deduction_values.items():
        if code in details_map:
            details_map[code].amount = amt
            db.add(details_map[code])
        else:
            db_det = PayrollDetail(
                id=uuid4(),
                payroll_item_id=item_id,
                code=code,
                amount=amt,
                is_taxable=False
            )
            db.add(db_det)
            
    total_deduction = pension + health + ltc + employment + income_tax + local_tax
    net_pay = gross_pay - total_deduction
    
    # 5) Item 갱신
    item_res = await db.execute(select(PayrollItem).filter(PayrollItem.id == item_id))
    db_item = item_res.scalars().first()
    if db_item:
        db_item.gross_pay = gross_pay
        db_item.total_deduction = total_deduction
        db_item.net_pay = net_pay
        db.add(db_item)
        
    await db.commit()

async def calculate_ledger(db: AsyncSession, ledger_id: UUID) -> None:
    items_res = await db.execute(select(PayrollItem).filter(PayrollItem.ledger_id == ledger_id))
    items = items_res.scalars().all()
    for item in items:
        temp_res = await db.execute(select(SalaryTemplate).filter(SalaryTemplate.employee_id == item.employee_id))
        temp = temp_res.scalars().first()
        await calculate_item_payroll(db, item.id, temp)

# --- 공통 급여 코드 및 템플릿 관리 ---
async def get_payroll_codes(db: AsyncSession) -> List[PayrollCode]:
    res = await db.execute(select(PayrollCode).order_by(PayrollCode.sort_order))
    return list(res.scalars().all())

async def save_payroll_code(db: AsyncSession, code_in: Any) -> PayrollCode:
    res = await db.execute(select(PayrollCode).filter(PayrollCode.code == code_in.code))
    db_obj = res.scalars().first()
    if not db_obj:
        db_obj = PayrollCode(code=code_in.code)
    db_obj.name = code_in.name
    db_obj.type = code_in.type
    db_obj.is_taxable = code_in.is_taxable
    db_obj.tax_free_limit = code_in.tax_free_limit
    db_obj.is_system = code_in.is_system
    db_obj.is_active = code_in.is_active
    db_obj.sort_order = code_in.sort_order
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj

async def get_salary_templates(db: AsyncSession) -> List[SalaryTemplate]:
    query = (
        select(
            SalaryTemplate,
            User.full_name.label("employee_name"),
            Employee.employee_no.label("employee_no"),
            Department.name.label("department_name")
        )
        .join(Employee, SalaryTemplate.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .outerjoin(Department, Employee.department_id == Department.id)
    )
    res = await db.execute(query)
    templates = []
    for row in res.all():
        temp = row[0]
        temp.employee_name = row[1]
        temp.employee_no = row[2]
        temp.department_name = row[3]
        templates.append(temp)
    return templates

async def get_salary_template(db: AsyncSession, employee_id: UUID) -> Optional[SalaryTemplate]:
    query = (
        select(
            SalaryTemplate,
            User.full_name.label("employee_name"),
            Employee.employee_no.label("employee_no"),
            Department.name.label("department_name")
        )
        .join(Employee, SalaryTemplate.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .outerjoin(Department, Employee.department_id == Department.id)
        .filter(SalaryTemplate.employee_id == employee_id)
    )
    res = await db.execute(query)
    row = res.first()
    if row:
        temp = row[0]
        temp.employee_name = row[1]
        temp.employee_no = row[2]
        temp.department_name = row[3]
        return temp
    return None

async def save_salary_template(db: AsyncSession, template_in: Any) -> SalaryTemplate:
    res = await db.execute(select(SalaryTemplate).filter(SalaryTemplate.employee_id == template_in.employee_id))
    db_obj = res.scalars().first()
    if not db_obj:
        db_obj = SalaryTemplate(employee_id=template_in.employee_id)
    db_obj.base_pay = template_in.base_pay
    db_obj.position_pay = template_in.position_pay
    db_obj.meal_allowance = template_in.meal_allowance
    db_obj.car_allowance = template_in.car_allowance
    db_obj.use_national_pension = template_in.use_national_pension
    db_obj.use_health_insurance = template_in.use_health_insurance
    db_obj.use_employment_insurance = template_in.use_employment_insurance
    db_obj.income_tax_rate = template_in.income_tax_rate
    db.add(db_obj)
    await db.commit()
    return await get_salary_template(db, template_in.employee_id)


# --- [호환성 보전] 레거시 erp_payrolls 기반 임시 서비스 ---
async def get_payrolls_by_employee(db: AsyncSession, employee_id: UUID) -> List[PayrollItem]:
    # 기존 UI 호환성을 위해 사원의 PayrollItem 리스트를 리턴
    query = select(PayrollItem).filter(PayrollItem.employee_id == employee_id).options(selectinload(PayrollItem.details))
    res = await db.execute(query)
    return list(res.scalars().all())

async def create_payroll(db: AsyncSession, payroll_in: Any) -> PayrollItem:
    # 레거시 create 대응을 위해 기본 PayrollItem을 생성하고 계산
    item_id = uuid4()
    # ledger_id는 레거시 호출 시 임의 생성하거나 기존 대장이 있으면 거기에 태워야 함.
    # 여기서는 호환을 위한 더미 레거시 ledger_id 할당
    ledger_query = select(PayrollLedger).limit(1)
    ledger_res = await db.execute(ledger_query)
    ledger = ledger_res.scalars().first()
    if not ledger:
        ledger = PayrollLedger(
            id=uuid4(),
            title="임시 레거시 대장",
            pay_year=date.today().year,
            pay_month=date.today().month,
            pay_date=date.today(),
            pay_type="정기급여",
            start_date=date.today(),
            end_date=date.today(),
            status="confirmed",
            created_by=payroll_in.employee_id
        )
        db.add(ledger)
        await db.commit()
        
    db_obj = PayrollItem(
        id=item_id,
        ledger_id=ledger.id,
        employee_id=payroll_in.employee_id,
        gross_pay=payroll_in.base_pay,
        total_deduction=Decimal("0.0"),
        net_pay=payroll_in.base_pay,
        status="draft"
    )
    db.add(db_obj)
    await db.commit()
    
    # 계산 실행
    temp_res = await db.execute(select(SalaryTemplate).filter(SalaryTemplate.employee_id == payroll_in.employee_id))
    temp = temp_res.scalars().first()
    await calculate_item_payroll(db, item_id, temp)
    
    return await get_ledger_item(db, item_id)


# =============================================================
# ── 8. 추가 서비스 (Spring Boot 호환 라우트 지원)              ──
# =============================================================

async def acknowledge_review(db: AsyncSession, id: UUID) -> Optional[PerformanceReview]:
    db_obj = await get_review(db, id)
    if not db_obj:
        return None
    if db_obj.status != "submitted":
        raise ValueError("Only submitted reviews can be acknowledged")
    db_obj.status = "acknowledged"
    db.add(db_obj)
    await db.commit()
    return await get_review(db, id)

async def get_reviews(
    db: AsyncSession,
    employee_id: Optional[UUID] = None,
    review_year: Optional[int] = None,
    review_period: Optional[str] = None,
    status: Optional[str] = None,
    offset: int = 0,
    limit: int = 20
) -> Dict[str, Any]:
    query = (
        select(
            PerformanceReview,
            User.full_name.label("employee_name"),
            Employee.employee_no
        )
        .join(Employee, PerformanceReview.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
    )
    if employee_id:
        query = query.filter(PerformanceReview.employee_id == employee_id)
    if review_year:
        query = query.filter(PerformanceReview.review_year == review_year)
    if review_period:
        query = query.filter(PerformanceReview.review_period == review_period)
    if status:
        query = query.filter(PerformanceReview.status == status)

    count_query = select(func.count()).select_from(query.subquery())
    total_res = await db.execute(count_query)
    total = total_res.scalar() or 0

    query = query.order_by(PerformanceReview.review_year.desc()).offset(offset).limit(limit)
    result = await db.execute(query)
    items = []
    for row in result.all():
        pr, emp_name, emp_no = row[0], row[1], row[2]
        pr.employee_name = emp_name
        pr.employee_no = emp_no
        items.append(pr)
    return {"items": items, "total": total}

async def get_leaves(
    db: AsyncSession,
    employee_id: Optional[UUID] = None,
    manager_id: Optional[UUID] = None,
    status: Optional[str] = None
) -> List[LeaveRequest]:
    """통합 휴가 조회 (Spring Boot LeaveController.getLeaves 호환)"""
    if manager_id:
        return await get_leaves_by_manager(db, manager_id=manager_id, status=status)
    if employee_id:
        return await get_leaves_by_employee(db, employee_id=employee_id, status=status)
    # 전체 조회
    query = (
        select(LeaveRequest, User.full_name.label("employee_name"), Employee.employee_no)
        .join(Employee, LeaveRequest.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
    )
    if status:
        query = query.filter(LeaveRequest.status == status)
    query = query.order_by(LeaveRequest.start_date.desc())
    result = await db.execute(query)
    leaves = []
    for row in result.all():
        lr, emp_name, emp_no = row[0], row[1], row[2]
        lr.employee_name = emp_name
        lr.employee_no = emp_no
        leaves.append(lr)
    return leaves

async def get_leave_balance_by_query(db: AsyncSession, employee_id: UUID, year: int) -> LeaveBalance:
    """query param 방식의 leave balance 조회 (Spring Boot LeaveController.getLeaveBalance 호환)"""
    return await get_leave_balance(db, employee_id=employee_id, year=year)

# --- 레거시 급여 추가 서비스 (Spring Boot PayrollController 호환) ---

async def get_payrolls(
    db: AsyncSession,
    pay_year: Optional[int] = None,
    pay_month: Optional[int] = None,
    status: Optional[str] = None,
    page: int = 1,
    size: int = 20
) -> Dict[str, Any]:
    """레거시 급여 목록 조회 (페이지네이션)"""
    query = (
        select(
            PayrollItem,
            User.full_name.label("employee_name"),
            Employee.employee_no,
            Department.name.label("department_name"),
            Employee.position
        )
        .join(Employee, PayrollItem.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .outerjoin(Department, Employee.department_id == Department.id)
        .join(PayrollLedger, PayrollItem.ledger_id == PayrollLedger.id)
        .options(selectinload(PayrollItem.details))
    )
    if pay_year:
        query = query.filter(PayrollLedger.pay_year == pay_year)
    if pay_month:
        query = query.filter(PayrollLedger.pay_month == pay_month)
    if status:
        query = query.filter(PayrollItem.status == status)

    count_query = select(func.count()).select_from(query.subquery())
    total_res = await db.execute(count_query)
    total = total_res.scalar() or 0

    offset = (page - 1) * size
    query = query.order_by(Employee.employee_no).offset(offset).limit(size)
    result = await db.execute(query)

    items = []
    for row in result.all():
        item = row[0]
        item.employee_name = row[1]
        item.employee_no = row[2]
        item.department_name = row[3]
        item.position = row[4]
        items.append(item)

    return {"items": items, "total": total}

async def get_payroll(db: AsyncSession, id: UUID) -> Optional[PayrollItem]:
    """레거시 급여 단건 조회"""
    return await get_ledger_item(db, id)

async def update_payroll(db: AsyncSession, id: UUID, payroll_in: Any) -> Optional[PayrollItem]:
    """레거시 급여 수정"""
    return await update_ledger_item_details(db, item_id=id, item_in=payroll_in)

async def bulk_create_payroll(db: AsyncSession, pay_year: int, pay_month: int, pay_date: str, creator_id: UUID) -> List[PayrollItem]:
    """레거시 급여 일괄 생성"""
    from datetime import datetime as dt
    ledger_in = PayrollLedgerCreate(
        title=f"{pay_year}년 {pay_month}월 급여",
        pay_year=pay_year,
        pay_month=pay_month,
        pay_date=date.fromisoformat(pay_date) if pay_date else date.today(),
        start_date=date(pay_year, pay_month, 1),
        end_date=date(pay_year, pay_month, 28),
        status="draft"
    )
    ledger = await create_ledger(db, ledger_in=ledger_in, creator_id=creator_id)
    return await get_ledger_items(db, ledger_id=ledger.id)

async def confirm_payroll(db: AsyncSession, id: UUID, confirmed_by: Optional[str] = None) -> None:
    """레거시 급여 확정"""
    item = await get_ledger_item(db, id)
    if not item:
        raise ValueError("Payroll not found")
    item.status = "confirmed"
    db.add(item)
    await db.commit()

async def mark_paid(db: AsyncSession, id: UUID, confirmed_by: Optional[str] = None) -> None:
    """레거시 급여 지급 완료"""
    item = await get_ledger_item(db, id)
    if not item:
        raise ValueError("Payroll not found")
    item.status = "paid"
    db.add(item)
    await db.commit()

async def delete_payroll(db: AsyncSession, id: UUID) -> bool:
    """레거시 급여 삭제"""
    result = await db.execute(select(PayrollItem).filter(PayrollItem.id == id))
    db_obj = result.scalars().first()
    if not db_obj:
        return False
    await db.delete(db_obj)
    await db.commit()
    return True

async def get_employee_payrolls(db: AsyncSession, employee_id: UUID) -> List[PayrollItem]:
    """레거시 사원별 급여 조회"""
    query = (
        select(
            PayrollItem,
            User.full_name.label("employee_name"),
            Employee.employee_no,
            Department.name.label("department_name"),
            Employee.position
        )
        .join(Employee, PayrollItem.employee_id == Employee.id)
        .outerjoin(User, Employee.user_id == User.id)
        .outerjoin(Department, Employee.department_id == Department.id)
        .filter(PayrollItem.employee_id == employee_id)
        .options(selectinload(PayrollItem.details))
    )
    result = await db.execute(query)
    items = []
    for row in result.all():
        item = row[0]
        item.employee_name = row[1]
        item.employee_no = row[2]
        item.department_name = row[3]
        item.position = row[4]
        items.append(item)
    return items
