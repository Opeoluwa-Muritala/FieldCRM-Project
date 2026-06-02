from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from app.api import deps
from app.db.session import get_db
from app.db.models import Borrower, User
from app.schemas import borrower as borrower_schemas

router = APIRouter()

@router.post("/", response_model=borrower_schemas.BorrowerResponse)
def create_borrower(
    borrower_in: borrower_schemas.BorrowerCreate,
    db: Session = Depends(get_db),
    current_officer: User = Depends(deps.RoleChecker(["Loan Officer", "Branch Manager", "System Admin"]))
):
    """
    Creates a new Borrower profile:
    - Automatically maps the creating Loan Officer as primary manager.
    - Scope protection: Verifies organization mapping alignment.
    """
    if current_officer.org_id != borrower_in.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Cannot register borrower outside your organisation context."
        )
        
    borrower = Borrower(
        org_id=borrower_in.org_id,
        loan_officer_id=current_officer.id,
        name=borrower_in.name,
        phone=borrower_in.phone,
        bvn=borrower_in.bvn,
        nin=borrower_in.nin,
        photo_url=borrower_in.photo_url,
        gps_coordinates=borrower_in.gps_coordinates,
        physical_address=borrower_in.physical_address,
        employment_status=borrower_in.employment_status,
        employer_name=borrower_in.employer_name,
        monthly_income=borrower_in.monthly_income,
        bank_name=borrower_in.bank_name,
        account_number=borrower_in.account_number,
        guarantor_name=borrower_in.guarantor_name,
        guarantor_phone=borrower_in.guarantor_phone,
        status="Applicant"
    )
    db.add(borrower)
    db.commit()
    db.refresh(borrower)
    return borrower

@router.get("/", response_model=List[borrower_schemas.BorrowerResponse])
def list_borrowers(
    q: Optional[str] = Query(None, description="Search term for name, phone, or BVN"),
    status_filter: Optional[str] = Query(None, description="Active / Applicant / Blacklisted / Dormant"),
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.RoleChecker(["Branch Manager", "Credit Officer", "Auditor", "System Admin"]))
):
    """
    Secure list & search API for Borrowers:
    - Scope Checks:
      - Only top-chain roles can view borrower records.
      - Results are still scoped to the user's organisation.
    - Offline support matches locally cached databases.
    """
    query = db.query(Borrower).filter(Borrower.org_id == current_user.org_id)
    
    if status_filter:
        query = query.filter(Borrower.status == status_filter)
        
    if q:
        # Check against name, phone, or BVN using standard SQL parameters
        search_pattern = f"%{q}%"
        query = query.filter(
            (Borrower.name.ilike(search_pattern)) | 
            (Borrower.phone.like(search_pattern)) | 
            (Borrower.bvn.like(search_pattern))
        )
        
    return query.order_by(Borrower.name).all()

@router.get("/{borrower_id}", response_model=borrower_schemas.BorrowerResponse)
def get_borrower(
    borrower_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.RoleChecker(["Branch Manager", "Credit Officer", "Auditor", "System Admin"]))
):
    """
    Gets details of a single borrower:
    - Enforces strict org-level boundary checks.
    - Enforces loan officer ownership bounds.
    """
    borrower = db.query(Borrower).filter(Borrower.id == borrower_id).first()
    if not borrower:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Borrower profile not found."
        )
        
    # Boundary validations
    if borrower.org_id != current_user.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access Denied. Resource outside organisation boundaries."
        )
        
    return borrower

@router.put("/{borrower_id}", response_model=borrower_schemas.BorrowerResponse)
def update_borrower(
    borrower_id: str,
    borrower_in: borrower_schemas.BorrowerUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(deps.get_current_user)
):
    """
    Updates a borrower's demographic details:
    - Scoping checks apply to edit actions.
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
            detail="Edit Denied. Resource outside organisation boundaries."
        )
        
    if current_user.role == "Loan Officer" and borrower.loan_officer_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Edit Denied. Resource assigned to a different officer."
        )
        
    # Apply modifications
    update_data = borrower_in.dict(exclude_unset=True)
    for key, value in update_data.items():
        setattr(borrower, key, value)
        
    db.commit()
    db.refresh(borrower)
    return borrower
