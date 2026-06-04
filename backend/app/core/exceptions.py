from fastapi import Request
from fastapi.responses import JSONResponse

class DomainException(Exception):
    def __init__(self, message: str, status_code: int = 400):
        self.message = message
        self.status_code = status_code
        super().__init__(message)

class ResourceNotFound(DomainException):
    def __init__(self, resource: str):
        super().__init__(f"{resource} not found", 404)

class AccessDenied(DomainException):
    def __init__(self):
        super().__init__("Access denied", 403)

class WorkflowViolation(DomainException):
    def __init__(self, message: str):
        super().__init__(message, 422)

async def domain_exception_handler(request: Request, exc: DomainException):
    request_id = getattr(request.state, "request_id", "unknown")
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": exc.message, "request_id": request_id}
    )
