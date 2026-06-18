from app.db.base_class import Base

# Import all models here to ensure they are registered with SQLAlchemy
from app.domains.user.models import User, OAuthAccount
from app.domains.workspace.models import Workspace, WorkspaceMember
from app.domains.project.models import Project, ProjectMember
from app.domains.board.models import Board, BoardColumn, BoardGroup, BoardView
from app.domains.item.models import Item, ItemValue
from app.domains.comment.models import Comment, CommentMention
from app.domains.erp.hr.models import (
    Department, Employee, Attendance, LeaveRequest, LeaveBalance,
    PerformanceReview, CommonCode, PayrollLedger, PayrollCode,
    SalaryTemplate, PayrollItem, PayrollDetail
)
from app.domains.erp.sales.models import Quotation, QuotationItem, TaxInvoice, TaxInvoiceItem
from app.domains.extra.models import ActivityLog, Notification, Automation, Dashboard
from app.domains.team.models import Team, TeamMember
from app.domains.attachment.models import Attachment
