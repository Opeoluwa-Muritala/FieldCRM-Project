from fastapi import Depends
from app.core.dependencies import authenticated_db_conn, get_current_user
from app.domains.workflow.engine import WorkflowEngine


class PermissionChecker:
    def __init__(self, permission): self.permission=permission
    async def __call__(self,current_user=Depends(get_current_user),conn=Depends(authenticated_db_conn)):
        await WorkflowEngine(conn).require_permission(current_user,self.permission)
        return current_user
