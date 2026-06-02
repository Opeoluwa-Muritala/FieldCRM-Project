import logging
from datetime import datetime
from sqlalchemy.orm import Session
from app.db.models import LoanApplication, RepaymentSchedule, PromiseToPay, Borrower

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("FieldCRMCollections")

def trigger_mock_termii_sms(phone: str, message: str) -> bool:
    """
    Mock dispatcher mimicking Termii SMS aggregator API for Nigeria:
    - Logs transmission securely.
    """
    logger.info("Termii SMS Sent to [%s] -> Message: '%s'", phone, message)
    return True

def calculate_dpd_and_escalate(db: Session, org_id: str) -> dict:
    """
    Background batch processor for Collections Intelligence:
    1. Fetches all active loans.
    2. Calculates DPD (Days Past Due) from the earliest overdue repayment installment.
    3. Triggers automated escalation states.
    4. Issues mock Termii SMS alerts at key stages.
    """
    active_loans = db.query(LoanApplication).filter(
        LoanApplication.org_id == org_id, 
        LoanApplication.status == "Active"
    ).all()
    
    current_time = datetime.utcnow()
    updated_counts = {
        "level_1": 0,
        "level_2": 0,
        "level_3": 0,
        "level_4": 0
    }
    
    for loan in active_loans:
        # Find the earliest unpaid installment that is past its due date
        overdue_installment = db.query(RepaymentSchedule).filter(
            RepaymentSchedule.application_id == loan.id,
            RepaymentSchedule.status != "Paid",
            RepaymentSchedule.due_date < current_time
        ).order_by(RepaymentSchedule.due_date.asc()).first()
        
        if overdue_installment:
            dpd = (current_time - overdue_installment.due_date).days
            borrower = db.query(Borrower).filter(Borrower.id == loan.borrower_id).first()
            borrower_phone = borrower.phone if borrower else ""
            
            # Map DPD to active Escalation Levels
            if 1 <= dpd <= 7:
                updated_counts["level_1"] += 1
                trigger_mock_termii_sms(
                    borrower_phone, 
                    f"Reminder: Your installment of NGN {overdue_installment.amount_due:.2f} is {dpd} days overdue. Please repay immediately."
                )
            elif 8 <= dpd <= 30:
                updated_counts["level_2"] += 1
                # Auto escalates to Branch Manager review state
                logger.warning("Loan [%s] escalated to Level 2 (Manager) due to DPD %d", loan.id, dpd)
            elif 31 <= dpd <= 60:
                updated_counts["level_3"] += 1
                logger.warning("Loan [%s] escalated to Level 3 (Senior Collector) due to DPD %d", loan.id, dpd)
            elif dpd > 60:
                updated_counts["level_4"] += 1
                loan.status = "Written Off"
                logger.error("Loan [%s] written off to bad debt due to DPD %d", loan.id, dpd)
                
    db.commit()
    return updated_counts

def evaluate_promises_to_pay(db: Session, org_id: str) -> int:
    """
    Evaluates active PTPs and flags broken promises:
    - Scans for pending PTPs whose promised_date has passed without full repayment.
    """
    current_time = datetime.utcnow()
    pending_ptps = db.query(PromiseToPay).filter(
        PromiseToPay.status == "Pending",
        PromiseToPay.promised_date < current_time
    ).all()
    
    broken_count = 0
    for ptp in pending_ptps:
        # Check if the loan still has unpaid overdue schedules
        overdue_exists = db.query(RepaymentSchedule).filter(
            RepaymentSchedule.application_id == ptp.application_id,
            RepaymentSchedule.status != "Paid",
            RepaymentSchedule.due_date < current_time
        ).first()
        
        if overdue_exists:
            ptp.status = "Broken"
            broken_count += 1
            # Alert creating officer
            logger.warning("PTP [%s] flagged as BROKEN. Borrower failed to pay NGN %.2f", ptp.id, ptp.promised_amount)
        else:
            ptp.status = "Met"
            
    db.commit()
    return broken_count
