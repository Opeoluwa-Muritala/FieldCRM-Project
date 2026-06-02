from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.api import deps
from app.db.session import get_db
from app.db.models import (
    RepaymentRecord, RepaymentSchedule, LoanApplication, 
    PromiseToPay, User, Borrower
)
from app.schemas import collections as schemas
from app.services import collections as services

router = APIRouter()

@router.post("/repayments", response_model=schemas.RepaymentRecordResponse)
def record_repayment(
    repayment_in: schemas.RepaymentRecordCreate,
    db: Session = Depends(get_db),
    current_officer: User = Depends(deps.RoleChecker(["Loan Officer", "Branch Manager", "System Admin"]))
):
    """
    Records a manual repayment transaction:
    - Automatically manages partial payments and overpayments.
    - Scopes validation context dynamically.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == repayment_in.application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Loan Application not found.")
        
    if app.org_id != current_officer.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Cannot record repayments outside your organisation boundaries."
        )
        
    schedule_entry = db.query(RepaymentSchedule).filter(
        RepaymentSchedule.id == repayment_in.schedule_id,
        RepaymentSchedule.application_id == app.id
    ).first()
    
    if not schedule_entry:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Repayment Schedule entry not found.")
        
    # Create the transaction ledger entry
    record = RepaymentRecord(
        schedule_id=repayment_in.schedule_id,
        application_id=repayment_in.application_id,
        amount_paid=repayment_in.amount_paid,
        channel=repayment_in.channel,
        officer_id=current_officer.id,
        synced_at=None
    )
    db.add(record)
    
    # Process payment math (Partial and Overpayment controls)
    remaining_payment = repayment_in.amount_paid
    
    # Calculate unpaid balance on current target schedule
    if schedule_entry.status != "Paid":
        if remaining_payment >= schedule_entry.amount_due:
            remaining_payment -= schedule_entry.amount_due
            schedule_entry.amount_due = 0.0
            schedule_entry.status = "Paid"
        else:
            schedule_entry.amount_due -= remaining_payment
            remaining_payment = 0.0
            schedule_entry.status = "Pending"
            
    # Overpayment spill-over logic (Apply extra money sequentially to subsequent schedules)
    if remaining_payment > 0.0:
        subsequent_schedules = db.query(RepaymentSchedule).filter(
            RepaymentSchedule.application_id == app.id,
            RepaymentSchedule.status != "Paid",
            RepaymentSchedule.id != schedule_entry.id
        ).order_by(RepaymentSchedule.due_date.asc()).all()
        
        for next_schedule in subsequent_schedules:
            if remaining_payment <= 0.0:
                break
                
            if remaining_payment >= next_schedule.amount_due:
                remaining_payment -= next_schedule.amount_due
                next_schedule.amount_due = 0.0
                next_schedule.status = "Paid"
            else:
                next_schedule.amount_due -= remaining_payment
                remaining_payment = 0.0
                next_schedule.status = "Pending"
                
    # If all schedules are marked Paid, close the loan
    all_paid = db.query(RepaymentSchedule).filter(
        RepaymentSchedule.application_id == app.id,
        RepaymentSchedule.status != "Paid"
    ).count() == 0
    
    if all_paid:
        app.status = "Closed"
        
    db.commit()
    db.refresh(record)
    return record

@router.post("/ptps", response_model=schemas.PromiseToPayResponse)
def create_promise_to_pay(
    ptp_in: schemas.PromiseToPayCreate,
    db: Session = Depends(get_db),
    current_officer: User = Depends(deps.get_current_user)
):
    """Logs a new borrower repayment promise (PTP tracker)."""
    app = db.query(LoanApplication).filter(LoanApplication.id == ptp_in.application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Loan Application not found.")
        
    if app.org_id != current_officer.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Cannot record promise outside your organisation context."
        )
        
    ptp = PromiseToPay(
        application_id=ptp_in.application_id,
        borrower_id=app.borrower_id,
        promised_amount=ptp_in.promised_amount,
        promised_date=ptp_in.promised_date,
        status="Pending",
        created_by=current_officer.id
    )
    db.add(ptp)
    db.commit()
    db.refresh(ptp)
    return ptp

@router.post("/batch-process")
def execute_batch_collections_intelligence(
    db: Session = Depends(get_db),
    current_admin: User = Depends(deps.RoleChecker(["System Admin"]))
):
    """
    Standard administrator route executing:
    1. Days Past Due (DPD) recalculations, escalations and Termii alerts.
    2. Promises-to-Pay evaluations.
    """
    escalations = services.calculate_dpd_and_escalate(db, current_admin.org_id)
    broken_ptps = services.evaluate_promises_to_pay(db, current_admin.org_id)
    
    return {
        "status": "batch completed",
        "escalation_metrics": escalations,
        "broken_promises_flagged": broken_ptps
    }
