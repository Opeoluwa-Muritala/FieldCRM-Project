from pathlib import Path
from uuid import UUID
from fastapi import APIRouter,Depends,Form,HTTPException,Request
from app.config import settings
from app.core.dependencies import authenticated_db_conn,get_current_user
from app.core.templates import create_templates
from app.domains.tasks.service import TaskService
router=APIRouter();templates=create_templates(str(Path(__file__).resolve().parents[4]/"frontend"/"templates"))
def enabled():
    if not settings.OPERATIONS_UI_ENABLED:raise HTTPException(404,"Not found")
@router.get("/my-work")
async def my_work(request:Request,current_user=Depends(get_current_user),conn=Depends(authenticated_db_conn)):
    enabled();items=await TaskService(conn).my_work(current_user)
    buckets={key:[] for key in ("due_today","overdue","new","returned","waiting","high_priority")}
    from datetime import datetime,timezone
    now=datetime.now(timezone.utc)
    for item in items:
        row=dict(item);due=row.get("due_at")
        if row["status"] in buckets:buckets[row["status"]].append(row)
        if due and due<now:buckets["overdue"].append(row)
        elif due and due.date()==now.date():buckets["due_today"].append(row)
        if row["priority"] in {"high","urgent"}:buckets["high_priority"].append(row)
    return templates.TemplateResponse(request,"tasks/my_work.html",{"current_user":current_user,"buckets":buckets})
@router.get("/exceptions")
async def exceptions(request:Request,current_user=Depends(get_current_user),conn=Depends(authenticated_db_conn)):
    enabled();rows=await conn.fetch("SELECT * FROM operational_exceptions WHERE org_id=$1 AND status IN('open','investigating') ORDER BY created_at DESC",current_user.org_id)
    return templates.TemplateResponse(request,"tasks/exceptions.html",{"current_user":current_user,"exceptions":rows})
@router.post("/applications/{application_id}/notes")
async def note(application_id:UUID,category:str=Form(...),body:str=Form(...),mentions:list[UUID]=Form(default=[]),current_user=Depends(get_current_user),conn=Depends(authenticated_db_conn)):
    enabled();return dict(await TaskService(conn).add_note(current_user,application_id,category,body,mentions))
