from fastapi import FastAPI
from app.core.config import settings
from app.api.v1.router import api_router
from fastapi.middleware.cors import CORSMiddleware

tags_metadata = [
    {
        "name": "users",
        "description": "User management APIs",
    },
    {
        "name": "workspaces",
        "description": "Workspace management APIs",
    },
    {
        "name": "projects",
        "description": "Project management APIs",
    },
    {
        "name": "boards",
        "description": "Board management APIs",
    },
    {
        "name": "items",
        "description": "Item management APIs",
    },
    {
        "name": "comments",
        "description": "Comment management APIs",
    },
    {
        "name": "erp-hr",
        "description": "ERP HR management APIs",
    },
    {
        "name": "erp-sales",
        "description": "ERP Sales management APIs",
    },
    {
        "name": "extra",
        "description": "Extra management APIs (Dashboards, Automations, etc)",
    },
]

app = FastAPI(
    title="MiniERP API Documentation",
    description="MiniERP 시스템의 백엔드 REST API 문서입니다.",
    version="v1.0.0",
    openapi_tags=tags_metadata,
    openapi_url="/openapi.json"
)

# Set all CORS enabled origins
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

from fastapi.staticfiles import StaticFiles
import os

# Ensure uploads directory exists
os.makedirs("uploads", exist_ok=True)

app.mount("/api/uploads", StaticFiles(directory="uploads"), name="api_uploads")
app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")

app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get("/")
async def root():
    return {"message": "Welcome to FastAPI Backend"}
