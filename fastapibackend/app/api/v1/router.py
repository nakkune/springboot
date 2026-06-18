from fastapi import APIRouter
from app.domains.user import router as user_router
from app.domains.project import router as project_router
from app.domains.board import router as board_router
from app.domains.item import router as item_router
from app.domains.erp.hr import router as hr_router
from app.domains.erp.sales import router as sales_router
from app.domains.workspace import router as workspace_router
from app.domains.extra import router as extra_router
from app.domains.comment import router as comment_router
from app.domains.notification import router as notification_router
from app.domains.team import router as team_router
from app.domains.attachment import router as attachment_router


api_router = APIRouter()
api_router.include_router(user_router.router, prefix="/users", tags=["users"])
api_router.include_router(project_router.router, prefix="/projects", tags=["projects"])
api_router.include_router(board_router.router, prefix="/boards", tags=["boards"])
api_router.include_router(board_router.board_columns_router, prefix="/board-columns", tags=["board-columns"])
api_router.include_router(board_router.board_groups_router, prefix="/board-groups", tags=["board-groups"])
api_router.include_router(item_router.router, prefix="/items", tags=["items"])
api_router.include_router(item_router.item_values_router, prefix="/item-values", tags=["item-values"])
api_router.include_router(hr_router.router, prefix="/erp/hr", tags=["erp-hr"])
api_router.include_router(sales_router.router, prefix="/erp/sales", tags=["erp-sales"])
api_router.include_router(workspace_router.router, prefix="/workspaces", tags=["workspaces"])
api_router.include_router(extra_router.router, prefix="/extra", tags=["extra"])
api_router.include_router(comment_router.router, tags=["comments"])
api_router.include_router(notification_router.router, prefix="/notifications", tags=["notifications"])
api_router.include_router(team_router.router, prefix="/teams", tags=["teams"])
api_router.include_router(attachment_router.router, prefix="/attachments", tags=["attachments"])
api_router.include_router(attachment_router.files_router, prefix="/files", tags=["files"])
