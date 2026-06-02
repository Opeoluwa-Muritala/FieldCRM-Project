import uuid
from datetime import datetime
from sqlalchemy import Column, String, Integer, Float, Boolean, DateTime, ForeignKey, Text, JSON, Table
from sqlalchemy.orm import relationship
from app.db.session import Base

def generate_uuid() -> str:
    """Generates unique primary key string values."""
    return str(uuid.uuid4())

class Organisation(Base):
    __tablename__ = "organisation"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    name = Column(String(100), nullable=False)
    type = Column(String(50), nullable=False)  # MFB, Cooperative, SACCO
    subscription_tier = Column(String(50), nullable=False, default="standard")
    config_json = Column(JSON, nullable=True)  # Dynamic branding config, terms
    
    users = relationship("User", back_populates="organisation")
    branches = relationship("Branch", back_populates="organisation")
    borrowers = relationship("Borrower", back_populates="organisation")
    groups = relationship("Group", back_populates="organisation")
    applications = relationship("LoanApplication", back_populates="organisation")

class User(Base):
    __tablename__ = "user"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    org_id = Column(String(36), ForeignKey("organisation.id"), nullable=False)
    branch_id = Column(String(36), ForeignKey("branch.id"), nullable=True)
    name = Column(String(100), nullable=False)
    phone = Column(String(30), nullable=False, unique=True)
    role = Column(String(50), nullable=False)  # Loan Officer, Branch Manager, Credit Officer, Auditor, MCR
    hashed_password = Column(String(255), nullable=False)
    is_active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    organisation = relationship("Organisation", back_populates="users")
    branch = relationship("Branch", foreign_keys=[branch_id], back_populates="users")
    assigned_borrowers = relationship("Borrower", back_populates="loan_officer")

class Branch(Base):
    __tablename__ = "branch"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    org_id = Column(String(36), ForeignKey("organisation.id"), nullable=False)
    name = Column(String(100), nullable=False)
    location = Column(String(255), nullable=False)
    manager_id = Column(String(36), ForeignKey("user.id"), nullable=True)
    
    organisation = relationship("Organisation", back_populates="branches")
    users = relationship("User", foreign_keys=[User.branch_id], back_populates="branch")

class Borrower(Base):
    __tablename__ = "borrower"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    org_id = Column(String(36), ForeignKey("organisation.id"), nullable=False)
    loan_officer_id = Column(String(36), ForeignKey("user.id"), nullable=False)
    name = Column(String(100), nullable=False)
    phone = Column(String(30), nullable=False)
    bvn = Column(String(20), nullable=False)
    nin = Column(String(20), nullable=False)
    photo_url = Column(String(255), nullable=True)
    status = Column(String(50), nullable=False, default="Applicant")  # Active, Applicant, Blacklisted, Dormant
    gps_coordinates = Column(String(100), nullable=True)
    physical_address = Column(String(255), nullable=True)
    employment_status = Column(String(50), nullable=True)
    employer_name = Column(String(100), nullable=True)
    monthly_income = Column(Float, nullable=True)
    bank_name = Column(String(100), nullable=True)
    account_number = Column(String(30), nullable=True)
    guarantor_name = Column(String(100), nullable=True)
    guarantor_phone = Column(String(30), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    organisation = relationship("Organisation", back_populates="borrowers")
    loan_officer = relationship("User", back_populates="assigned_borrowers")
    applications = relationship("LoanApplication", back_populates="borrower")
    communication_logs = relationship("CommunicationLog", back_populates="borrower")

class Group(Base):
    __tablename__ = "group"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    org_id = Column(String(36), ForeignKey("organisation.id"), nullable=False)
    name = Column(String(100), nullable=False)
    type = Column(String(50), nullable=False)  # esusu, ajo, coop
    leader_id = Column(String(36), ForeignKey("borrower.id"), nullable=True)
    meeting_schedule = Column(String(255), nullable=True)
    territory = Column(String(100), nullable=True)
    
    organisation = relationship("Organisation", back_populates="groups")
    members = relationship("GroupMember", back_populates="group")

class GroupMember(Base):
    __tablename__ = "group_member"
    
    group_id = Column(String(36), ForeignKey("group.id"), primary_key=True)
    borrower_id = Column(String(36), ForeignKey("borrower.id"), primary_key=True)
    joined_date = Column(DateTime, default=datetime.utcnow)
    status = Column(String(50), nullable=False, default="Active")
    
    group = relationship("Group", back_populates="members")
    borrower = relationship("Borrower")

class LoanApplication(Base):
    __tablename__ = "loan_application"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    org_id = Column(String(36), ForeignKey("organisation.id"), nullable=False)
    borrower_id = Column(String(36), ForeignKey("borrower.id"), nullable=False)
    current_stage = Column(Integer, nullable=False, default=1)  # Stage 1-6
    current_owner_id = Column(String(36), ForeignKey("user.id"), nullable=False)
    status = Column(String(50), nullable=False, default="Draft")
    amount = Column(Float, nullable=False)
    tenure = Column(Integer, nullable=False)  # Weeks/Months
    product_type = Column(String(50), nullable=False)  # Individual, SME, Emergency
    interest_rate = Column(Float, nullable=False)
    repayment_frequency = Column(String(50), nullable=False)  # Weekly, Monthly
    collateral_desc = Column(Text, nullable=True)
    collateral_value = Column(Float, nullable=True)
    officer_recommendation = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    organisation = relationship("Organisation", back_populates="applications")
    borrower = relationship("Borrower", back_populates="applications")
    current_owner = relationship("User")
    workflow_events = relationship("WorkflowEvent", back_populates="application")
    stage_data = relationship("StageData", back_populates="application")
    repayment_schedule = relationship("RepaymentSchedule", back_populates="application")

class WorkflowEvent(Base):
    __tablename__ = "workflow_event"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    application_id = Column(String(36), ForeignKey("loan_application.id"), nullable=False)
    action = Column(String(50), nullable=False)  # Recommend Approve, Return, Request Docs
    from_stage = Column(Integer, nullable=True)
    to_stage = Column(Integer, nullable=False)
    actor_id = Column(String(36), ForeignKey("user.id"), nullable=False)
    reason = Column(Text, nullable=True)  # Mandatory reason for Return/Denials
    timestamp = Column(DateTime, default=datetime.utcnow)
    
    application = relationship("LoanApplication", back_populates="workflow_events")
    actor = relationship("User")

class StageData(Base):
    __tablename__ = "stage_data"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    application_id = Column(String(36), ForeignKey("loan_application.id"), nullable=False)
    stage = Column(Integer, nullable=False)
    data_json = Column(JSON, nullable=False)
    submitted_by = Column(String(36), ForeignKey("user.id"), nullable=False)
    submitted_at = Column(DateTime, default=datetime.utcnow)
    
    application = relationship("LoanApplication", back_populates="stage_data")

class RepaymentSchedule(Base):
    __tablename__ = "repayment_schedule"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    application_id = Column(String(36), ForeignKey("loan_application.id"), nullable=False)
    due_date = Column(DateTime, nullable=False)
    amount_due = Column(Float, nullable=False)
    status = Column(String(50), nullable=False, default="Pending")  # Paid, Pending, Overdue
    
    application = relationship("LoanApplication", back_populates="repayment_schedule")
    records = relationship("RepaymentRecord", back_populates="schedule")

class RepaymentRecord(Base):
    __tablename__ = "repayment_record"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    schedule_id = Column(String(36), ForeignKey("repayment_schedule.id"), nullable=False)
    application_id = Column(String(36), ForeignKey("loan_application.id"), nullable=False)
    amount_paid = Column(Float, nullable=False)
    date = Column(DateTime, default=datetime.utcnow)
    channel = Column(String(50), nullable=False)  # cash, transfer, POS
    officer_id = Column(String(36), ForeignKey("user.id"), nullable=False)
    synced_at = Column(DateTime, nullable=True)
    
    schedule = relationship("RepaymentSchedule", back_populates="records")
    officer = relationship("User")

class CommunicationLog(Base):
    __tablename__ = "communication_log"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    borrower_id = Column(String(36), ForeignKey("borrower.id"), nullable=False)
    type = Column(String(50), nullable=False)  # Phone Call, Field Visit, WhatsApp
    outcome = Column(String(100), nullable=False)
    note = Column(Text, nullable=False)
    officer_id = Column(String(36), ForeignKey("user.id"), nullable=False)
    timestamp = Column(DateTime, default=datetime.utcnow)
    photo_urls = Column(JSON, nullable=True)  # List of strings
    
    borrower = relationship("Borrower", back_populates="communication_logs")
    officer = relationship("User")

class PromiseToPay(Base):
    __tablename__ = "promise_to_pay"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    application_id = Column(String(36), ForeignKey("loan_application.id"), nullable=False)
    borrower_id = Column(String(36), ForeignKey("borrower.id"), nullable=False)
    promised_amount = Column(Float, nullable=False)
    promised_date = Column(DateTime, nullable=False)
    status = Column(String(50), nullable=False, default="Pending")  # Met, Broken, Pending
    created_by = Column(String(36), ForeignKey("user.id"), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

class Document(Base):
    __tablename__ = "document"
    
    id = Column(String(36), primary_key=True, default=generate_uuid)
    application_id = Column(String(36), ForeignKey("loan_application.id"), nullable=False)
    type = Column(String(100), nullable=False)
    url = Column(String(255), nullable=False)
    uploaded_by = Column(String(36), ForeignKey("user.id"), nullable=False)
    verified_by = Column(String(36), ForeignKey("user.id"), nullable=True)
    status = Column(String(50), nullable=False, default="Uploaded")  # Uploaded, Verified, Flagged
