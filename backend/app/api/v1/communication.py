from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.api import deps
from app.db.session import get_db
from app.db.models import CommunicationLog, Borrower, User
from app.schemas import communication as comm_schemas

router = APIRouter()

@router.post("/", response_model=comm_schemas.CommunicationLogResponse)
def create_communication_log(
    log_in: comm_schemas.CommunicationLogCreate,
    db: Session = Depends(get_db),
    current_officer: User = Depends(deps.get_current_user)
):
    """
    Registers a new field interaction log (Call, WhatsApp outcome, physical visit):
    - Secure boundary: Validates the target borrower exists in the officer's organization.
    - Ownership boundary: Loan Officers can only log events against their assigned profiles.
    """
    borrower = db.query(Borrower).filter(Borrower.id == log_in.borrower_id).first()
    if not borrower:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Borrower profile not found."
        )
        
    if borrower.org_id != current_officer.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Cannot log interaction outside your organization's context."
        )
        
    if current_officer.role == "Loan Officer" and borrower.loan_officer_id != current_officer.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Cannot log interaction against a borrower assigned to another officer."
        )
        
    log = CommunicationLog(
        borrower_id=log_in.borrower_id,
        type=log_in.type,
        outcome=log_in.outcome,
        note=log_in.note,
        photo_urls=log_in.photo_urls,
        officer_id=current_officer.id
    )
    db.add(log)
    db.commit()
    db.refresh(log)
    return log

@router.get("/{borrower_id}", response_model=List[comm_schemas.CommunicationLogResponse])
def get_borrower_communication_history(
    borrower_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.get_current_user)
):
    """
    Retrieves all interaction logs registered for a specific borrower:
    - Scoping check:
      - Back-office roles (Auditors, Managers, Credit Officers) can read full history.
      - Loan Officers can read logs only if the borrower is assigned to them.
    """
    borrower = db.query(Borrower).filter(Borrower.id == borrower_id).first()
    if not borrower:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Borrower profile not found."
        )
        
    if borrower.org_id != current_user.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access Denied. Resource outside organization boundaries."
        )
        
    if current_user.role == "Loan Officer" and borrower.loan_officer_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access Denied. Borrower communication logs are restricted."
        )
        
    logs = db.query(CommunicationLog).filter(
        CommunicationLog.borrower_id == borrower_id
    ).order_by(CommunicationLog.timestamp.desc()).all()
    
    return logs
