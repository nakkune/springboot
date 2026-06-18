from typing import Any, List, Optional, Dict
from uuid import UUID
from datetime import date
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_db, get_current_active_user
from app.domains.user.models import User
from app.domains.erp.hr import schemas, service

router = APIRouter()

# =============================================================
# ── 1. 부서 (Department) API                                 ──
# =============================================================

@router.get("/departments", response_model=List[schemas.Department])
async def read_departments(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_all_departments(db)

@router.get("/departments/tree", response_model=List[schemas.Department])
async def read_departments_tree(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_department_tree(db)

@router.get("/departments/{id}", response_model=schemas.Department)
async def read_department(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    dept = await service.get_department(db, id)
    if not dept:
        raise HTTPException(status_code=404, detail="Department not found")
    return dept

@router.post("/departments", response_model=schemas.Department, status_code=status.HTTP_201_CREATED)
async def create_department(
    department_in: schemas.DepartmentCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_department(db, department_in=department_in)

@router.put("/departments/{id}", response_model=schemas.Department)
async def update_department(
    id: UUID,
    department_in: schemas.DepartmentUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    dept = await service.update_department(db, id, department_in)
    if not dept:
        raise HTTPException(status_code=404, detail="Department not found")
    return dept

@router.delete("/departments/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_department(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    try:
        await service.delete_department(db, id)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    return None

# =============================================================
# ── 2. 직원 (Employee) API                                   ──
# =============================================================

@router.get("/employees", response_model=schemas.EmployeeListResponse)
async def read_employees(
    department_id: Optional[UUID] = Query(None, alias="departmentId"),
    status: Optional[str] = None,
    search: Optional[str] = None,
    page: int = 1,
    size: int = 20,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    offset = (page - 1) * size
    return await service.get_employees(db, department_id=department_id, status=status, search=search, offset=offset, limit=size)

@router.get("/employees/{id}", response_model=schemas.Employee)
async def read_employee(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    emp = await service.get_employee(db, id)
    if not emp:
        raise HTTPException(status_code=404, detail="Employee not found")
    return emp

@router.get("/employees/user/{user_id}", response_model=schemas.Employee)
async def read_employee_by_user(
    user_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    emp = await service.get_employee_by_user(db, user_id=user_id)
    if not emp:
        raise HTTPException(status_code=404, detail="Employee not found for user")
    return emp

@router.post("/employees", response_model=schemas.Employee, status_code=status.HTTP_201_CREATED)
async def create_employee(
    employee_in: schemas.EmployeeCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_employee(db, employee_in=employee_in, creator_id=current_user.id)

@router.put("/employees/{id}", response_model=schemas.Employee)
async def update_employee(
    id: UUID,
    employee_in: schemas.EmployeeUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    emp = await service.update_employee(db, id, employee_in)
    if not emp:
        raise HTTPException(status_code=404, detail="Employee not found")
    return emp

@router.patch("/employees/{id}/status")
async def update_employee_status(
    id: UUID,
    body: Dict[str, str],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    emp_status = body.get("status")
    if not emp_status:
        raise HTTPException(status_code=400, detail="status is required")
    await service.update_employee_status(db, id, status=emp_status)
    return {"message": "success"}

@router.delete("/employees/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_employee(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    deleted = await service.delete_employee(db, id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Employee not found")
    return None

# =============================================================
# ── 3. 근태 (Attendance) API                                 ──
# =============================================================

@router.get("/employees/{employee_id}/attendances", response_model=List[schemas.Attendance])
async def read_attendances(
    employee_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_attendances_by_employee(db, employee_id=employee_id)

@router.post("/attendances", response_model=schemas.Attendance, status_code=status.HTTP_201_CREATED)
async def create_attendance(
    attendance_in: schemas.AttendanceCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_attendance(db, attendance_in=attendance_in)

@router.put("/attendances/{id}", response_model=schemas.Attendance)
async def update_attendance(
    id: UUID,
    attendance_in: schemas.AttendanceUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    att = await service.update_attendance(db, id, attendance_in)
    if not att:
        raise HTTPException(status_code=404, detail="Attendance record not found")
    return att


# --- Singular Attendance Endpoints for Frontend Compatibility ---
@router.get("/attendance", response_model=List[schemas.Attendance])
async def read_attendance(
    employeeId: Optional[UUID] = Query(None),
    fromDate: Optional[date] = Query(None),
    toDate: Optional[date] = Query(None),
    status: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    근태 목록 조회
    """
    return await service.get_attendance(
        db,
        employee_id=employeeId,
        from_date=fromDate,
        to_date=toDate,
        status=status
    )

@router.get("/attendance/{id}", response_model=schemas.Attendance)
async def read_attendance_by_id(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    근태 단건 조회
    """
    att = await service.get_attendance_by_id(db, id)
    if not att:
        raise HTTPException(status_code=404, detail="Attendance record not found")
    return att

@router.post("/attendance", response_model=schemas.Attendance, status_code=status.HTTP_201_CREATED)
async def create_attendance_singular(
    attendance_in: schemas.AttendanceCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    근태 기록 생성
    """
    return await service.create_attendance(db, attendance_in=attendance_in)

@router.post("/attendance/check-in", response_model=schemas.Attendance, status_code=status.HTTP_201_CREATED)
async def check_in_endpoint(
    body: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    출근 처리
    """
    emp_id_str = body.get("employeeId")
    if not emp_id_str:
        raise HTTPException(status_code=400, detail="employeeId is required")
    try:
        emp_id = UUID(emp_id_str)
        return await service.check_in(db, employee_id=emp_id)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/attendance/check-out", response_model=schemas.Attendance)
async def check_out_endpoint(
    body: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    퇴근 처리
    """
    emp_id_str = body.get("employeeId")
    if not emp_id_str:
        raise HTTPException(status_code=400, detail="employeeId is required")
    try:
        emp_id = UUID(emp_id_str)
        return await service.check_out(db, employee_id=emp_id)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/attendance/{id}", response_model=schemas.Attendance)
async def update_attendance_singular(
    id: UUID,
    attendance_in: schemas.AttendanceUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    근태 기록 수정
    """
    att = await service.update_attendance(db, id, attendance_in)
    if not att:
        raise HTTPException(status_code=404, detail="Attendance record not found")
    return att

@router.delete("/attendance/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_attendance_singular(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """
    근태 기록 삭제
    """
    success = await service.delete_attendance(db, id)
    if not success:
        raise HTTPException(status_code=404, detail="Attendance record not found")
    return None

# =============================================================
# ── 4. 휴가 (Leave Requests & Balances) API                   ──
# =============================================================

@router.get("/leaves/employee/{employee_id}", response_model=List[schemas.LeaveRequest])
async def read_leaves_by_employee(
    employee_id: UUID,
    status: Optional[str] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_leaves_by_employee(db, employee_id=employee_id, status=status)

@router.get("/leaves/manager/{manager_id}", response_model=List[schemas.LeaveRequest])
async def read_leaves_by_manager(
    manager_id: UUID,
    status: Optional[str] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_leaves_by_manager(db, manager_id=manager_id, status=status)

@router.get("/leaves/{id}", response_model=schemas.LeaveRequest)
async def read_leave(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    leave = await service.get_leave(db, id)
    if not leave:
        raise HTTPException(status_code=404, detail="Leave request not found")
    return leave

@router.post("/leaves", response_model=schemas.LeaveRequest, status_code=status.HTTP_201_CREATED)
async def create_leave(
    leave_in: schemas.LeaveRequestCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_leave(db, leave_in, creator_id=current_user.id)

@router.put("/leaves/{id}", response_model=schemas.LeaveRequest)
async def update_leave(
    id: UUID,
    leave_in: schemas.LeaveRequestUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    leave = await service.update_leave(db, id, leave_in)
    if not leave:
        raise HTTPException(status_code=404, detail="Leave request not found")
    return leave

@router.post("/leaves/{id}/approve")
async def approve_leave(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    try:
        await service.approve_leave(db, id, approver_id=current_user.id)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    return {"message": "approved"}

@router.post("/leaves/{id}/reject")
async def reject_leave(
    id: UUID,
    body: Dict[str, str],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    reject_reason = body.get("rejectReason", "")
    try:
        await service.reject_leave(db, id, approver_id=current_user.id, reject_reason=reject_reason)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    return {"message": "rejected"}

@router.post("/leaves/{id}/cancel")
async def cancel_leave(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    try:
        await service.cancel_leave(db, id)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    return {"message": "cancelled"}

@router.delete("/leaves/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_leave(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    deleted = await service.delete_leave(db, id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Leave request not found")
    return None

@router.get("/leaves/balance/{employee_id}", response_model=schemas.LeaveBalance)
async def read_leave_balance(
    employee_id: UUID,
    year: int = Query(..., description="연도"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_leave_balance(db, employee_id=employee_id, year=year)

# =============================================================
# ── 5. 공통 코드 (Common Code) API                             ──
# =============================================================

@router.get("/codes/groups")
async def read_code_groups(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_groups(db)

@router.get("/codes/group/{code_group}", response_model=List[schemas.CommonCode])
async def read_codes_by_group(
    code_group: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_codes(db, code_group=code_group)

@router.get("/codes", response_model=List[schemas.CommonCode])
async def read_all_codes(
    group: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    if group:
        return await service.get_codes(db, code_group=group)
    return await service.get_all_codes(db)

@router.post("/codes", response_model=schemas.CommonCode, status_code=status.HTTP_201_CREATED)
async def create_code(
    code_in: schemas.CommonCodeCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    try:
        return await service.create_code(db, code_in=code_in)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/codes/{id}", response_model=schemas.CommonCode)
async def update_code(
    id: UUID,
    code_in: schemas.CommonCodeCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    try:
        code = await service.update_code(db, id, code_in)
        if not code:
            raise HTTPException(status_code=404, detail="Code not found")
        return code
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.delete("/codes/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_code(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    deleted = await service.delete_code(db, id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Code not found")
    return None

# =============================================================
# ── 6. 인사 평가 (Performance Review) API                      ──
# =============================================================

@router.get("/reviews/employee/{employee_id}", response_model=List[schemas.PerformanceReview])
async def read_reviews_by_employee(
    employee_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_reviews_by_employee(db, employee_id=employee_id)

@router.get("/reviews/reviewer/{reviewer_id}", response_model=List[schemas.PerformanceReview])
async def read_reviews_by_reviewer(
    reviewer_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_reviews_by_reviewer(db, reviewer_id=reviewer_id)

@router.get("/reviews/{id}", response_model=schemas.PerformanceReview)
async def read_review(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    rev = await service.get_review(db, id)
    if not rev:
        raise HTTPException(status_code=404, detail="Review not found")
    return rev

@router.post("/reviews", response_model=schemas.PerformanceReview, status_code=status.HTTP_201_CREATED)
async def create_review(
    review_in: schemas.PerformanceReviewCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_review(db, review_in=review_in)

@router.put("/reviews/{id}", response_model=schemas.PerformanceReview)
async def update_review(
    id: UUID,
    review_in: schemas.PerformanceReviewUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    rev = await service.update_review(db, id, review_in)
    if not rev:
        raise HTTPException(status_code=404, detail="Review not found")
    return rev

@router.post("/reviews/{id}/submit", response_model=schemas.PerformanceReview)
async def submit_review(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    rev = await service.submit_review(db, id)
    if not rev:
        raise HTTPException(status_code=404, detail="Review not found")
    return rev

@router.delete("/reviews/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_review(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    deleted = await service.delete_review(db, id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Review not found")
    return None

# =============================================================
# ── 7. 급여 및 급여대장 (Payroll & Payroll Ledger) API           ──
# =============================================================

@router.get("/payrolls/ledgers", response_model=List[schemas.PayrollLedger])
async def read_payrolls_ledgers(
    payYear: Optional[int] = Query(None, alias="payYear"),
    payMonth: Optional[int] = Query(None, alias="payMonth"),
    status: Optional[str] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_ledgers(db, pay_year=payYear, pay_month=payMonth, status=status)

@router.get("/payrolls/ledgers/{id}", response_model=schemas.PayrollLedger)
async def read_payroll_ledger(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    ledger = await service.get_ledger(db, id)
    if not ledger:
        raise HTTPException(status_code=404, detail="Payroll ledger not found")
    return ledger

@router.post("/payrolls/ledgers", response_model=schemas.PayrollLedger, status_code=status.HTTP_201_CREATED)
async def create_payroll_ledger(
    ledger_in: schemas.PayrollLedgerCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_ledger(db, ledger_in=ledger_in, creator_id=current_user.id)

@router.delete("/payrolls/ledgers/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_payroll_ledger(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    deleted = await service.delete_ledger(db, id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Payroll ledger not found")
    return None

@router.post("/payrolls/ledgers/{id}/confirm")
async def confirm_payroll_ledger(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    try:
        await service.confirm_ledger(db, id)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    return {"message": "confirmed"}

@router.post("/payrolls/ledgers/{id}/pay")
async def pay_payroll_ledger(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    try:
        await service.pay_ledger(db, id)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    return {"message": "paid"}

@router.get("/payrolls/ledgers/{ledgerId}/items", response_model=List[schemas.PayrollItem])
async def read_payroll_ledger_items(
    ledgerId: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_ledger_items(db, ledger_id=ledgerId)

@router.get("/payrolls/ledgers/items/{id}", response_model=schemas.PayrollItem)
async def read_payroll_ledger_item(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    item = await service.get_ledger_item(db, id)
    if not item:
        raise HTTPException(status_code=404, detail="Payroll item not found")
    return item

@router.put("/payrolls/ledgers/items/{id}", response_model=schemas.PayrollItem)
async def update_payroll_ledger_item(
    id: UUID,
    item_in: schemas.PayrollItem,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    item = await service.update_ledger_item_details(db, item_id=id, item_in=item_in)
    if not item:
        raise HTTPException(status_code=404, detail="Payroll item not found")
    return item

@router.post("/payrolls/ledgers/{id}/calculate")
async def calculate_payroll_ledger(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    await service.calculate_ledger(db, ledger_id=id)
    return {"message": "calculated"}

@router.get("/payrolls/codes", response_model=List[schemas.PayrollCode])
async def read_payroll_codes(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_payroll_codes(db)

@router.post("/payrolls/codes", response_model=schemas.PayrollCode)
async def save_payroll_code(
    code_in: schemas.PayrollCode,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.save_payroll_code(db, code_in=code_in)

@router.get("/payrolls/templates", response_model=List[schemas.SalaryTemplate])
async def read_salary_templates(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_salary_templates(db)

@router.get("/payrolls/templates/{employeeId}", response_model=schemas.SalaryTemplate)
async def read_salary_template(
    employeeId: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    temp = await service.get_salary_template(db, employee_id=employeeId)
    if not temp:
        raise HTTPException(status_code=404, detail="Salary template not found")
    return temp

@router.post("/payrolls/templates", response_model=schemas.SalaryTemplate)
async def save_salary_template(
    template_in: schemas.SalaryTemplate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.save_salary_template(db, template_in=template_in)

# --- [호환성 보전] 레거시 erp_payrolls 기반 임시 API ---
@router.get("/employees/{employee_id}/payrolls", response_model=List[schemas.PayrollItem])
async def read_payrolls(
    employee_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_payrolls_by_employee(db, employee_id=employee_id)

@router.post("/payrolls", response_model=schemas.PayrollItem)
async def create_payroll(
    payroll_in: schemas.PayrollItem,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.create_payroll(db, payroll_in=payroll_in)


# =============================================================
# ── Spring Boot 호환 추가 엔드포인트                           ──
# =============================================================

# --- 통합 휴가 조회 (Spring Boot: GET /erp/hr/leaves) ---
@router.get("/leaves", response_model=List[schemas.LeaveRequest])
async def read_leaves(
    employeeId: Optional[UUID] = Query(None),
    managerId: Optional[UUID] = Query(None),
    status: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    """통합 휴가 목록 조회 — employeeId 또는 managerId query param으로 분기"""
    return await service.get_leaves(db, employee_id=employeeId, manager_id=managerId, status=status)

# --- query param 방식 Leave Balance (Spring Boot: GET /erp/hr/leaves/balance?employeeId=...&year=...) ---
@router.get("/leaves/balance", response_model=schemas.LeaveBalance)
async def read_leave_balance_by_query(
    employeeId: UUID = Query(...),
    year: int = Query(0),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    if year == 0:
        from datetime import date as d
        year = d.today().year
    return await service.get_leave_balance_by_query(db, employee_id=employeeId, year=year)

# --- 평가 목록 조회 (Spring Boot: GET /erp/hr/reviews with pagination) ---
@router.get("/reviews", response_model=Dict)
async def read_reviews(
    employeeId: Optional[UUID] = Query(None),
    reviewYear: Optional[int] = Query(None),
    reviewPeriod: Optional[str] = Query(None),
    status: Optional[str] = Query(None),
    page: int = Query(1),
    size: int = Query(20),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    offset = (page - 1) * size
    return await service.get_reviews(
        db,
        employee_id=employeeId,
        review_year=reviewYear,
        review_period=reviewPeriod,
        status=status,
        offset=offset,
        limit=size
    )

# --- 사원별 평가 조회 (Spring Boot: GET /erp/hr/reviews/employee?employeeId=...) ---
@router.get("/reviews/employee", response_model=List[schemas.PerformanceReview])
async def read_employee_reviews(
    employeeId: UUID = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_reviews_by_employee(db, employee_id=employeeId)

# --- 평가 확인 (Spring Boot: POST /erp/hr/reviews/{id}/acknowledge) ---
@router.post("/reviews/{id}/acknowledge", response_model=schemas.PerformanceReview)
async def acknowledge_review(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    try:
        rev = await service.acknowledge_review(db, id)
        if not rev:
            raise HTTPException(status_code=404, detail="Review not found")
        return rev
    except ValueError as e:
        raise HTTPException(status_code=409, detail=str(e))

# --- 레거시 급여 목록 조회 (Spring Boot: GET /erp/hr/payrolls with pagination) ---
@router.get("/payrolls", response_model=Dict)
async def read_payrolls_list(
    payYear: Optional[int] = Query(None),
    payMonth: Optional[int] = Query(None),
    status: Optional[str] = Query(None),
    page: int = Query(1),
    size: int = Query(20),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_payrolls(db, pay_year=payYear, pay_month=payMonth, status=status, page=page, size=size)

# --- 레거시 사원별 급여 조회 (Spring Boot: GET /erp/hr/payrolls/employee?employeeId=...) ---
@router.get("/payrolls/employee", response_model=List[schemas.PayrollItem])
async def read_employee_payrolls(
    employeeId: UUID = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    return await service.get_employee_payrolls(db, employee_id=employeeId)

# --- 레거시 급여 단건 조회 (Spring Boot: GET /erp/hr/payrolls/{id}) ---
@router.get("/payrolls/{id}", response_model=schemas.PayrollItem)
async def read_payroll(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    item = await service.get_payroll(db, id)
    if not item:
        raise HTTPException(status_code=404, detail="Payroll not found")
    return item

# --- 레거시 급여 일괄 생성 (Spring Boot: POST /erp/hr/payrolls/bulk-create) ---
@router.post("/payrolls/bulk-create", response_model=List[schemas.PayrollItem], status_code=status.HTTP_201_CREATED)
async def bulk_create_payroll(
    body: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    pay_year = body.get("payYear")
    pay_month = body.get("payMonth")
    pay_date = body.get("payDate")
    return await service.bulk_create_payroll(db, pay_year=pay_year, pay_month=pay_month, pay_date=pay_date, creator_id=current_user.id)

# --- 레거시 급여 수정 (Spring Boot: PUT /erp/hr/payrolls/{id}) ---
@router.put("/payrolls/{id}", response_model=schemas.PayrollItem)
async def update_payroll(
    id: UUID,
    payroll_in: schemas.PayrollItem,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    item = await service.update_payroll(db, id, payroll_in)
    if not item:
        raise HTTPException(status_code=404, detail="Payroll not found")
    return item

# --- 레거시 급여 확정 (Spring Boot: POST /erp/hr/payrolls/{id}/confirm) ---
@router.post("/payrolls/{id}/confirm")
async def confirm_payroll(
    id: UUID,
    body: Dict[str, str] = {},
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    try:
        await service.confirm_payroll(db, id, confirmed_by=body.get("confirmedBy"))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    return {"message": "confirmed"}

# --- 레거시 급여 지급 완료 (Spring Boot: POST /erp/hr/payrolls/{id}/pay) ---
@router.post("/payrolls/{id}/pay")
async def pay_payroll(
    id: UUID,
    body: Dict[str, str] = {},
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    try:
        await service.mark_paid(db, id, confirmed_by=body.get("confirmedBy"))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    return {"message": "paid"}

# --- 레거시 급여 삭제 (Spring Boot: DELETE /erp/hr/payrolls/{id}) ---
@router.delete("/payrolls/{id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_payroll(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    deleted = await service.delete_payroll(db, id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Payroll not found")
    return None

# --- 코드 통합 조회 (Spring Boot: GET /erp/hr/codes?group=...) ---
@router.get("/codes/{id}", response_model=schemas.CommonCode)
async def read_code_by_id(
    id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_active_user),
) -> Any:
    code = await service.get_code(db, id)
    if not code:
        raise HTTPException(status_code=404, detail="Code not found")
    return code

