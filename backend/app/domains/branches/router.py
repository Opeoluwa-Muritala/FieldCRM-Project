from fastapi import APIRouter, Depends, HTTPException, status
from app.core.database import db_conn
from app.core.dependencies import RoleChecker
from app.core.cache import cache_response
from app.domains.users.schemas import UserRow
from app.domains.branches.repository import BranchRepository
from app.domains.branches.schemas import BranchCreate, BranchResponse
from uuid import UUID

router = APIRouter(prefix="/api/v1/branches", tags=["Branches"])

@router.post("", response_model=BranchResponse, status_code=status.HTTP_201_CREATED)
async def create_branch(
    payload: BranchCreate,
    conn = Depends(db_conn),
    current_user: UserRow = Depends(RoleChecker(["System Admin"]))
):
    repo = BranchRepository(conn)
    try:
        return await repo.create(current_user.org_id, payload.name, payload.code)
    except Exception as e:
        raise HTTPException(status_code=400, detail="Branch with this code already exists.")

@router.get("", response_model=list[BranchResponse])
@cache_response(ttl_seconds=600)
async def list_branches(
    conn = Depends(db_conn),
    current_user: UserRow = Depends(RoleChecker(["System Admin", "Branch Manager", "Account Officer"]))
):
    repo = BranchRepository(conn)
    return await repo.list_by_org(current_user.org_id)
