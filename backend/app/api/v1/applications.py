from typing import List, Dict, Any
from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File
from sqlalchemy.orm import Session
from app.api import deps
from app.db.session import get_db
from app.db.models import (
    LoanApplication, Borrower, User, WorkflowEvent, 
    StageData, RepaymentSchedule, Branch
)
from app.schemas import application as app_schemas
from app.schemas.form_schemas import LoanApplicationFormModel, GuarantorsFormModel, PledgeTrustReceiptFormModel


router = APIRouter()

DISBURSEMENT_FORM_TYPES = {
    "loan_application": "Loan Application Form",
    "guarantor": "Guarantors Form",
    "pledge_trust_receipt": "Pledge and Trust Receipt"
}

def find_escalation_user(db: Session, org_id: str, role: str, fallback_user_id: str) -> str:
    """Finds an active user matching a specific role in the organization, falling back to a default."""
    target = db.query(User).filter(
        User.org_id == org_id, 
        User.role == role, 
        User.is_active == True
    ).first()
    return target.id if target else fallback_user_id

@router.post("/", response_model=app_schemas.LoanApplicationResponse)
def create_loan_application(
    app_in: app_schemas.LoanApplicationCreate,
    db: Session = Depends(get_db),
    current_officer: User = Depends(deps.RoleChecker(["Loan Officer"]))
):
    """
    Initiates a new Loan Application (Stage 1 - Loan Officer):
    - Sets current owner as the creating loan officer.
    - Sets status to 'Draft'.
    """
    borrower = db.query(Borrower).filter(Borrower.id == app_in.borrower_id).first()
    if not borrower:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Borrower profile not found."
        )
        
    if borrower.org_id != current_officer.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Borrower does not belong to your organisation context."
        )
        
    if borrower.loan_officer_id != current_officer.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Borrower profile is assigned to a different loan officer."
        )
        
    app = LoanApplication(
        org_id=current_officer.org_id,
        borrower_id=app_in.borrower_id,
        current_stage=1,
        current_owner_id=current_officer.id,
        status="Draft",
        amount=app_in.amount,
        tenure=app_in.tenure,
        product_type=app_in.product_type,
        interest_rate=app_in.interest_rate,
        repayment_frequency=app_in.repayment_frequency,
        collateral_desc=app_in.collateral_desc,
        collateral_value=app_in.collateral_value,
        officer_recommendation=app_in.officer_recommendation
    )
    db.add(app)
    db.commit()
    db.refresh(app)
    
    # Save base Stage 1 data block
    stage_data = StageData(
        application_id=app.id,
        stage=1,
        data_json={"officer_recommendation": app_in.officer_recommendation},
        submitted_by=current_officer.id
    )
    db.add(stage_data)
    db.commit()
    
    return app

@router.post("/{application_id}/submit", response_model=app_schemas.LoanApplicationResponse)
def submit_to_manager(
    application_id: str,
    db: Session = Depends(get_db),
    current_officer: User = Depends(deps.RoleChecker(["Loan Officer"]))
):
    """
    Submits application upward (Stage 1 -> Stage 2 - Branch Manager):
    - Sets current owner as the branch manager.
    - Sets status to 'In Review - Stage 2'.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
        
    if app.current_owner_id != current_officer.id or app.current_stage != 1:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only the active owner at Stage 1 can submit this application."
        )
        
    # Resolve Branch Manager (find manager for officer's branch)
    branch = db.query(Branch).filter(Branch.id == current_officer.branch_id).first()
    manager_id = branch.manager_id if branch and branch.manager_id else current_officer.id
    
    app.current_stage = 2
    app.current_owner_id = manager_id
    app.status = "In Review - Stage 2"
    
    # Register immutable audit trace
    event = WorkflowEvent(
        application_id=app.id,
        action="Submit for Review",
        from_stage=1,
        to_stage=2,
        actor_id=current_officer.id,
        reason="Initial application draft submitted upward for Branch Manager review."
    )
    db.add(event)
    db.commit()
    db.refresh(app)
    return app

@router.post("/{application_id}/manager-review", response_model=app_schemas.LoanApplicationResponse)
def manager_review(
    application_id: str,
    review: app_schemas.ManagerReview,
    db: Session = Depends(get_db),
    current_manager: User = Depends(deps.RoleChecker(["Branch Manager", "System Admin"]))
):
    """
    Manager reviews application (Stage 2 -> Stage 3 - Credit Officer):
    - If recommended: transitions to Stage 3. Resolves organization Credit Officer.
    - If denied: terminates as 'Denied'.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
        
    if app.current_owner_id != current_manager.id or app.current_stage != 2:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only the assigned Branch Manager can review this application."
        )
        
    if review.recommend_approve:
        credit_officer_id = find_escalation_user(db, app.org_id, "Credit Officer", current_manager.id)
        app.current_stage = 3
        app.current_owner_id = credit_officer_id
        app.status = "In Review - Stage 3"
        action_taken = "Recommend Approve"
    else:
        app.status = "Denied"
        action_taken = "Recommend Deny"
        
    # Save Stage 2 review data
    stage_data = StageData(
        application_id=app.id,
        stage=2,
        data_json={"recommend_approve": review.recommend_approve, "note": review.note, "risk_flag": review.risk_flag_toggle},
        submitted_by=current_manager.id
    )
    db.add(stage_data)
    
    # Audit log
    event = WorkflowEvent(
        application_id=app.id,
        action=action_taken,
        from_stage=2,
        to_stage=app.current_stage if review.recommend_approve else 2,
        actor_id=current_manager.id,
        reason=review.note
    )
    db.add(event)
    db.commit()
    db.refresh(app)
    return app

@router.post("/{application_id}/risk-review", response_model=app_schemas.LoanApplicationResponse)
def risk_review(
    application_id: str,
    review: app_schemas.RiskAssessment,
    db: Session = Depends(get_db),
    current_officer: User = Depends(deps.RoleChecker(["Credit Officer", "System Admin"]))
):
    """
    Credit Officer assesses application risk (Stage 3 -> Stage 4 - Auditor):
    - Transitions to Stage 4. Resolves organization Auditor.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
        
    if app.current_owner_id != current_officer.id or app.current_stage != 3:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only the assigned Credit Officer can review this application."
        )
        
    if review.approve_risk:
        auditor_id = find_escalation_user(db, app.org_id, "Auditor", current_officer.id)
        app.current_stage = 4
        app.current_owner_id = auditor_id
        app.status = "In Review - Stage 4"
        action_taken = "Approve Risk"
    else:
        app.status = "Denied"
        action_taken = "Flag Risk"
        
    stage_data = StageData(
        application_id=app.id,
        stage=3,
        data_json={
            "approve_risk": review.approve_risk,
            "debt_to_income": review.debt_to_income_ratio,
            "credit_score": review.credit_score,
            "bureau_result": review.bureau_check_result,
            "risk_rating": review.risk_rating,
            "note": review.note
        },
        submitted_by=current_officer.id
    )
    db.add(stage_data)
    
    event = WorkflowEvent(
        application_id=app.id,
        action=action_taken,
        from_stage=3,
        to_stage=app.current_stage if review.approve_risk else 3,
        actor_id=current_officer.id,
        reason=review.note
    )
    db.add(event)
    db.commit()
    db.refresh(app)
    return app

@router.post("/{application_id}/auditor-review", response_model=app_schemas.LoanApplicationResponse)
def auditor_review(
    application_id: str,
    review: app_schemas.AuditorComplianceCheck,
    db: Session = Depends(get_db),
    current_auditor: User = Depends(deps.RoleChecker(["Auditor", "System Admin"]))
):
    """
    Auditor performs compliance check (Stage 4 -> Stage 5 - MCR Committee):
    - Transitions to Stage 5. Resolves primary committee administrator.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
        
    if app.current_owner_id != current_auditor.id or app.current_stage != 4:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only the assigned Auditor can perform this compliance check."
        )
        
    if review.compliance_pass:
        mcr_admin_id = find_escalation_user(db, app.org_id, "System Admin", current_auditor.id)
        app.current_stage = 5
        app.current_owner_id = mcr_admin_id
        app.status = "In Review - Stage 5"
        action_taken = "Compliance Pass"
    else:
        app.status = "Denied"
        action_taken = "Flag Compliance Issue"
        
    stage_data = StageData(
        application_id=app.id,
        stage=4,
        data_json={
            "compliance_pass": review.compliance_pass,
            "docs_verified": review.document_checklist_verified,
            "regulatory_flags": review.regulatory_flags_triggered,
            "note": review.note
        },
        submitted_by=current_auditor.id
    )
    db.add(stage_data)
    
    event = WorkflowEvent(
        application_id=app.id,
        action=action_taken,
        from_stage=4,
        to_stage=app.current_stage if review.compliance_pass else 4,
        actor_id=current_auditor.id,
        reason=review.note
    )
    db.add(event)
    db.commit()
    db.refresh(app)
    return app

@router.post("/{application_id}/mcr-review", response_model=app_schemas.LoanApplicationResponse)
def mcr_review(
    application_id: str,
    review: app_schemas.CommitteeApproval,
    db: Session = Depends(get_db),
    current_member: User = Depends(deps.RoleChecker(["System Admin", "Branch Manager"]))  # MCR represents Senior Leadership
):
    """
    Committee reviews application (Stage 5 -> Stage 6 - disbursement queue):
    - If approved: moves to Stage 6. Status is 'Approved - Pending Disbursement'.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
        
    if app.current_owner_id != current_member.id or app.current_stage != 5:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only the assigned Committee administrator can review this application."
        )
        
    if review.approve:
        # Move to stage 6 (Disbursement Queue)
        app.current_stage = 6
        app.status = "Approved - Pending Disbursement"
        action_taken = "Final Approve"
    else:
        app.status = "Denied"
        action_taken = "Final Deny"
        
    stage_data = StageData(
        application_id=app.id,
        stage=5,
        data_json={
            "approve": review.approve,
            "conditions": review.approval_conditions,
            "votes": review.vote_record,
            "note": review.note
        },
        submitted_by=current_member.id
    )
    db.add(stage_data)
    
    event = WorkflowEvent(
        application_id=app.id,
        action=action_taken,
        from_stage=5,
        to_stage=app.current_stage if review.approve else 5,
        actor_id=current_member.id,
        reason=review.note
    )
    db.add(event)
    db.commit()
    db.refresh(app)
    return app

def _require_application_access(app: LoanApplication, user: User):
    if app.org_id != user.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Application does not belong to your organisation context."
        )

def _serialize_disbursement_stage_data(row: StageData) -> app_schemas.DisbursementFormResponse:
    payload = row.data_json or {}
    return app_schemas.DisbursementFormResponse(
        application_id=row.application_id,
        form_type=payload.get("form_type", ""),
        data=payload.get("data", {}),
        completed=bool(payload.get("completed", False)),
        submitted_by=row.submitted_by,
        submitted_at=row.submitted_at
    )

def _completed_disbursement_form_types(db: Session, application_id: str) -> set[str]:
    rows = db.query(StageData).filter(
        StageData.application_id == application_id,
        StageData.stage == 6
    ).all()
    completed = set()
    for row in rows:
        payload = row.data_json
        if (
            isinstance(payload, dict)
            and payload.get("record_type") == "disbursement_form"
            and payload.get("completed") is True
        ):
            completed.add(payload.get("form_type"))
    return completed

@router.get("/{application_id}/disbursement-forms", response_model=List[app_schemas.DisbursementFormResponse])
def get_disbursement_forms(
    application_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.get_current_user)
):
    """
    Loads Stage 6 disbursement document captures for an approved loan application.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
    _require_application_access(app, current_user)

    rows = db.query(StageData).filter(
        StageData.application_id == application_id,
        StageData.stage == 6
    ).order_by(StageData.submitted_at.asc()).all()

    return [
        _serialize_disbursement_stage_data(row)
        for row in rows
        if isinstance(row.data_json, dict) and row.data_json.get("record_type") == "disbursement_form"
    ]

@router.put("/{application_id}/disbursement-forms/{form_type}", response_model=app_schemas.DisbursementFormResponse)
def upsert_disbursement_form(
    application_id: str,
    form_type: str,
    payload: app_schemas.DisbursementFormUpsert,
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.RoleChecker(["Loan Officer", "Branch Manager", "System Admin"]))
):
    """
    Creates or updates a disbursement document capture before final loan release.
    """
    if form_type != payload.form_type:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="URL form_type must match payload form_type."
        )
    if form_type not in DISBURSEMENT_FORM_TYPES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Unsupported disbursement form type."
        )

    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
    _require_application_access(app, current_user)
    if app.current_stage != 6:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Disbursement forms can only be captured after final approval at Stage 6."
        )

    existing_rows = db.query(StageData).filter(
        StageData.application_id == application_id,
        StageData.stage == 6
    ).all()
    target = next(
        (
            row for row in existing_rows
            if isinstance(row.data_json, dict)
            and row.data_json.get("record_type") == "disbursement_form"
            and row.data_json.get("form_type") == form_type
        ),
        None
    )

    data_json = {
        "record_type": "disbursement_form",
        "form_type": form_type,
        "form_name": DISBURSEMENT_FORM_TYPES[form_type],
        "data": payload.data,
        "completed": payload.completed
    }

    if target:
        target.data_json = data_json
        target.submitted_by = current_user.id
        target.submitted_at = datetime.utcnow()
    else:
        target = StageData(
            application_id=application_id,
            stage=6,
            data_json=data_json,
            submitted_by=current_user.id
        )
        db.add(target)

    event = WorkflowEvent(
        application_id=app.id,
        action="Capture Disbursement Form",
        from_stage=6,
        to_stage=6,
        actor_id=current_user.id,
        reason=f"{DISBURSEMENT_FORM_TYPES[form_type]} {'completed' if payload.completed else 'saved as draft'}."
    )
    db.add(event)
    db.commit()
    db.refresh(target)
    return _serialize_disbursement_stage_data(target)

@router.post("/{application_id}/return", response_model=app_schemas.LoanApplicationResponse)
def return_application(
    application_id: str,
    request: app_schemas.WorkflowReturnRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.get_current_user)
):
    """
    Bidirectional Return workflow execution:
    - Moves target application backward to any chosen previous stage.
    - Soft invalidates intermediate stage data submissions.
    - Captures mandatory reason text permanently in immutable audit log.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
        
    if app.current_owner_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only the active owner can perform a return action."
        )
        
    if request.target_stage >= app.current_stage:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Target return stage must be strictly prior to the current workflow stage."
        )
        
    from_stage = app.current_stage
    app.current_stage = request.target_stage
    app.status = f"Returned - Stage {request.target_stage}"
    
    # Resolve the target stage owner
    if request.target_stage == 1:
        # Resolve the original Loan Officer who created the profile
        original_log = db.query(StageData).filter(
            StageData.application_id == app.id, 
            StageData.stage == 1
        ).first()
        app.current_owner_id = original_log.submitted_by if original_log else app.current_owner_id
    elif request.target_stage == 2:
        original_log = db.query(StageData).filter(
            StageData.application_id == app.id, 
            StageData.stage == 2
        ).first()
        app.current_owner_id = original_log.submitted_by if original_log else app.current_owner_id
    elif request.target_stage == 3:
        original_log = db.query(StageData).filter(
            StageData.application_id == app.id, 
            StageData.stage == 3
        ).first()
        app.current_owner_id = original_log.submitted_by if original_log else app.current_owner_id
    elif request.target_stage == 4:
        original_log = db.query(StageData).filter(
            StageData.application_id == app.id, 
            StageData.stage == 4
        ).first()
        app.current_owner_id = original_log.submitted_by if original_log else app.current_owner_id
        
    # Soft invalidates database stage logs by deleting intermediate logs (above target)
    db.query(StageData).filter(
        StageData.application_id == app.id,
        StageData.stage > request.target_stage
    ).delete()
    
    # Write audit log
    event = WorkflowEvent(
        application_id=app.id,
        action=f"Return to Stage {request.target_stage}",
        from_stage=from_stage,
        to_stage=request.target_stage,
        actor_id=current_user.id,
        reason=request.reason
    )
    db.add(event)
    db.commit()
    db.refresh(app)
    return app

@router.post("/{application_id}/disburse", response_model=app_schemas.LoanApplicationResponse)
def disburse_loan(
    application_id: str,
    channel: str = "transfer",
    db: Session = Depends(get_db),
    current_admin: User = Depends(deps.RoleChecker(["System Admin", "Branch Manager"]))
):
    """
    Performs manual disbursement of approved loan (Stage 6 -> Active):
    1. Sets status to 'Active'.
    2. Dynamically generates the installment RepaymentSchedule entries.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
        
    if app.current_stage != 6:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Application must be approved and inside Stage 6 (Disbursement Queue)."
        )

    completed_forms = _completed_disbursement_form_types(db, app.id)
    missing_forms = [
        form_name for form_type, form_name in DISBURSEMENT_FORM_TYPES.items()
        if form_type not in completed_forms
    ]
    if missing_forms:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Complete required disbursement forms before loan release: " + ", ".join(missing_forms)
        )
        
    app.status = "Active"
    
    # Generate RepaymentSchedule installments
    # Simplistic calculation: equal installments weekly or monthly
    total_amount = app.amount * (1 + (app.interest_rate / 100))
    installment_value = total_amount / app.tenure
    interval_days = 7 if app.repayment_frequency.lower() == "weekly" else 30
    
    current_due_date = datetime.utcnow()
    for index in range(1, app.tenure + 1):
        current_due_date += timedelta(days=interval_days)
        schedule_entry = RepaymentSchedule(
            application_id=app.id,
            due_date=current_due_date,
            amount_due=installment_value,
            status="Pending"
        )
        db.add(schedule_entry)
        
    event = WorkflowEvent(
        application_id=app.id,
        action="Disburse",
        from_stage=6,
        to_stage=6,
        actor_id=current_admin.id,
        reason=f"Loan disbursed via {channel}. Generated {app.tenure} installment schedules."
    )
    db.add(event)
    db.commit()
    db.refresh(app)
    return app

@router.get("/{application_id}/history", response_model=List[app_schemas.WorkflowEventResponse])
def get_application_history(
    application_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.get_current_user)
):
    """
    Gets the complete immutable audit trail logs for a specific application:
    - Scoped access check matches RBAC definitions.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
        
    if app.org_id != current_user.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access Denied. Resource outside organization boundaries."
        )
        
    if current_user.role == "Loan Officer" and app.current_owner_id != current_user.id:
        # Verify if the loan officer created it originally (Stage 1 data block check)
        creator_log = db.query(StageData).filter(
            StageData.application_id == app.id, 
            StageData.stage == 1
        ).first()
        if not creator_log or creator_log.submitted_by != current_user.id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Access Denied. Restricted history view."
            )
            
    events = db.query(WorkflowEvent).filter(
        WorkflowEvent.application_id == application_id
    ).order_by(WorkflowEvent.timestamp.asc()).all()
    
    return events

# Form Intake, Upload, simulated OCR, and manual verification/correction API
@router.post("/{application_id}/forms/{form_type}/ocr")
async def simulate_ocr_extraction(
    application_id: str,
    form_type: str,
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.get_current_user)
):
    """
    Accepts PDF, JPG, JPEG, and PNG, performs basic format and size validation,
    and returns a simulated OCR extraction response with confidence scores, quality markers,
    low confidence highlights, and validation warnings.
    """
    # 1. Validate file extension and size
    filename = file.filename or ""
    ext = filename.split(".")[-1].lower() if "." in filename else ""
    if ext not in ["pdf", "jpg", "jpeg", "png"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Unsupported file format. Please upload PDF, JPG, JPEG, or PNG."
        )
        
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
    _require_application_access(app, current_user)

    # 2. Simulated OCR extraction payload
    if form_type == "loan_application":
        detected_form = "Loan Application Form (MMFB/CRM/01)"
        fields = [
            {"name": "applicant_full_name", "label": "Applicant Full Name", "value": "Grace Omowunmi", "confidence": 98, "critical": True},
            {"name": "loan_amount_requested", "label": "Loan Amount Requested", "value": "500000", "confidence": 61, "critical": True},
            {"name": "bvn", "label": "BVN", "value": "22334455667", "confidence": 94, "critical": True},
            {"name": "tenor", "label": "Tenor (Months)", "value": "12", "confidence": 90, "critical": True},
            {"name": "gsi_mandate", "label": "GSI Mandate", "value": "Accepted", "confidence": 95, "critical": True},
            {"name": "witness_signature", "label": "Witness Signature", "value": "Unreadable", "confidence": 22, "critical": True}
        ]
        flags = [
            "Loan amount has low confidence and needs manual correction.",
            "Witness signature is unreadable."
        ]
    elif form_type == "guarantor":
        detected_form = "Guarantors Form (MMFB/CRM/03)"
        fields = [
            {"name": "guarantor_full_name", "label": "Guarantor Full Name", "value": "Tunde Adewale", "confidence": 88, "critical": True},
            {"name": "bvn", "label": "BVN", "value": "22334455667", "confidence": 84, "critical": True},
            {"name": "maximum_guarantee_limit", "label": "Maximum Guarantee Limit", "value": "500000", "confidence": 68, "critical": True},
            {"name": "cheque_number", "label": "Recovery Cheque Number", "value": "000441", "confidence": 64, "critical": True},
            {"name": "witness_signature", "label": "Witness Signature", "value": "Unreadable", "confidence": 38, "critical": True}
        ]
        flags = [
            "Maximum guarantee limit has low confidence and needs verification.",
            "Witness signature is unreadable."
        ]
    elif form_type == "pledge_trust_receipt":
        detected_form = "Pledge and Trust Receipt (MMFB/CRM/02)"
        fields = [
            {"name": "borrower_name", "label": "Borrower Name", "value": "Grace Omowunmi", "confidence": 95, "critical": True},
            {"name": "facility_amount", "label": "Facility Amount", "value": "500000", "confidence": 90, "critical": True},
            {"name": "witness_signature", "label": "Witness Signature", "value": "Unreadable", "confidence": 22, "critical": True}
        ]
        flags = [
            "Witness signature is unreadable."
        ]
    else:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid form type for OCR.")

    return {
        "status": "success",
        "detected_form": detected_form,
        "image_quality": "High",
        "fields": fields,
        "validation_flags": flags
    }

@router.put("/{application_id}/forms/{form_type}")
def save_form_data(
    application_id: str,
    form_type: str,
    payload: Dict[str, Any],
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.get_current_user)
):
    """
    Saves structured form data (manual or OCR extraction) into the StageData storage.
    Includes source metadata ('manual', 'ocr', 'corrected', or 'approved') for each value.
    """
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
    _require_application_access(app, current_user)

    # Resolve existing StageData for this form type at the current stage
    existing = db.query(StageData).filter(
        StageData.application_id == application_id,
        StageData.stage == app.current_stage
    ).all()

    target = None
    for row in existing:
        if isinstance(row.data_json, dict) and row.data_json.get("form_type") == form_type:
            target = row
            break

    data_json = {
        "form_type": form_type,
        "data": payload.get("data", {}),
        "source": payload.get("source", "manual"),
        "completed": payload.get("completed", True)
    }

    if target:
        target.data_json = data_json
        target.submitted_by = current_user.id
        target.submitted_at = datetime.utcnow()
    else:
        target = StageData(
            application_id=application_id,
            stage=app.current_stage,
            data_json=data_json,
            submitted_by=current_user.id
        )
        db.add(target)

    # Register workflow event for audit log
    event = WorkflowEvent(
        application_id=application_id,
        action=f"Save Form - {form_type}",
        from_stage=app.current_stage,
        to_stage=app.current_stage,
        actor_id=current_user.id,
        reason=f"Saved {form_type} data capture with source: {payload.get('source', 'manual')}."
    )
    db.add(event)
    db.commit()

    return {"status": "success", "form_type": form_type, "completed": payload.get("completed", True)}

@router.get("/{application_id}/forms/{form_type}")
def get_form_data(
    application_id: str,
    form_type: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.get_current_user)
):
    """Retrieves captured form data for an application."""
    app = db.query(LoanApplication).filter(LoanApplication.id == application_id).first()
    if not app:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found.")
    _require_application_access(app, current_user)

    row = db.query(StageData).filter(
        StageData.application_id == application_id
    ).all()

    for r in row:
        if isinstance(r.data_json, dict) and r.data_json.get("form_type") == form_type:
            return r.data_json

    raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Form data not found.")

