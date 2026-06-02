from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.api import deps
from app.db.session import get_db
from app.db.models import Group, GroupMember, User, Borrower
from app.schemas import groups as schemas

router = APIRouter()

@router.post("/", response_model=schemas.GroupResponse)
def create_group(
    group_in: schemas.GroupCreate,
    db: Session = Depends(get_db),
    current_officer: User = Depends(deps.RoleChecker(["Loan Officer", "Branch Manager", "System Admin"]))
):
    """Creates a new cooperative lending group (ajo/esusu/coop)."""
    group = Group(
        org_id=current_officer.org_id,
        name=group_in.name,
        type=group_in.type,
        meeting_schedule=group_in.meeting_schedule,
        territory=group_in.territory
    )
    db.add(group)
    db.commit()
    db.refresh(group)
    return group

@router.post("/{group_id}/members", response_model=schemas.GroupResponse)
def add_group_member(
    group_id: str,
    member_in: schemas.GroupMemberAdd,
    db: Session = Depends(get_db),
    current_officer: User = Depends(deps.get_current_user)
):
    """Enrolls a borrower into a cooperative lending group."""
    group = db.query(Group).filter(Group.id == group_id).first()
    if not group:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Group not found.")
        
    if group.org_id != current_officer.org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Cannot edit groups outside your organisation."
        )
        
    borrower = db.query(Borrower).filter(Borrower.id == member_in.borrower_id).first()
    if not borrower or borrower.org_id != current_officer.org_id:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, 
            detail="Borrower not found or mapping invalid."
        )
        
    # Check if already a member
    existing = db.query(GroupMember).filter(
        GroupMember.group_id == group_id,
        GroupMember.borrower_id == member_in.borrower_id
    ).first()
    
    if existing:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Borrower is already enrolled in this group.")
        
    member = GroupMember(
        group_id=group_id,
        borrower_id=member_in.borrower_id,
        status="Active"
    )
    db.add(member)
    
    # If group has no leader, set this first member as leader by default
    if not group.leader_id:
        group.leader_id = borrower.id
        
    db.commit()
    db.refresh(group)
    return group
