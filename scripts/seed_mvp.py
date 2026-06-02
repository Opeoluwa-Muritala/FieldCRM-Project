import os
import sys
from datetime import datetime, timedelta

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND_DIR = os.path.join(ROOT_DIR, "backend")
sys.path.insert(0, BACKEND_DIR)

from app.core import security
from app.core.config import settings
from app.db.models import (
    Borrower,
    Branch,
    CommunicationLog,
    Group,
    GroupMember,
    LoanApplication,
    Organisation,
    RepaymentRecord,
    RepaymentSchedule,
    StageData,
    User,
    WorkflowEvent,
)
from app.db.session import Base, SessionLocal, engine


PASSWORD = "password123"


def get_or_create_user(db, org, branch, *, name, phone, role, password_hash):
    user = db.query(User).filter(User.phone == phone).first()
    if user:
        user.org_id = org.id
        user.branch_id = branch.id
        user.name = name
        user.role = role
        user.hashed_password = password_hash
        user.is_active = True
        return user

    user = User(
        org_id=org.id,
        branch_id=branch.id,
        name=name,
        phone=phone,
        role=role,
        hashed_password=password_hash,
        is_active=True,
    )
    db.add(user)
    db.flush()
    return user


def get_or_create_borrower(db, org, officer, **values):
    borrower = db.query(Borrower).filter(Borrower.phone == values["phone"]).first()
    if not borrower:
        borrower = Borrower(
            org_id=org.id,
            loan_officer_id=officer.id,
            name=values["name"],
            phone=values["phone"],
            bvn=values["bvn"],
            nin=values["nin"],
        )
        db.add(borrower)
        db.flush()

    for key, value in values.items():
        setattr(borrower, key, value)
    borrower.org_id = org.id
    borrower.loan_officer_id = officer.id
    return borrower


def create_application(db, org, borrower, owner, **values):
    existing = db.query(LoanApplication).filter(
        LoanApplication.borrower_id == borrower.id,
        LoanApplication.amount == values["amount"],
        LoanApplication.product_type == values["product_type"],
    ).first()
    if existing:
        app = existing
    else:
        app = LoanApplication(
            org_id=org.id,
            borrower_id=borrower.id,
            current_owner_id=owner.id,
            amount=values["amount"],
            tenure=values["tenure"],
            product_type=values["product_type"],
            interest_rate=values["interest_rate"],
            repayment_frequency=values["repayment_frequency"],
        )
        db.add(app)
        db.flush()

    for key, value in values.items():
        setattr(app, key, value)
    app.org_id = org.id
    app.borrower_id = borrower.id
    app.current_owner_id = owner.id
    return app


def add_stage_event(db, app, actor, action, from_stage, to_stage, reason):
    exists = db.query(WorkflowEvent).filter(
        WorkflowEvent.application_id == app.id,
        WorkflowEvent.action == action,
        WorkflowEvent.reason == reason,
    ).first()
    if exists:
        return exists
    event = WorkflowEvent(
        application_id=app.id,
        action=action,
        from_stage=from_stage,
        to_stage=to_stage,
        actor_id=actor.id,
        reason=reason,
    )
    db.add(event)
    return event


def upsert_stage_data(db, app, stage, submitted_by, data_json):
    record_type = data_json.get("record_type")
    form_type = data_json.get("form_type")
    rows = db.query(StageData).filter(
        StageData.application_id == app.id,
        StageData.stage == stage,
    ).all()
    target = None
    for row in rows:
        payload = row.data_json if isinstance(row.data_json, dict) else {}
        if payload.get("record_type") == record_type and payload.get("form_type") == form_type:
            target = row
            break
    if not target:
        target = StageData(
            application_id=app.id,
            stage=stage,
            data_json=data_json,
            submitted_by=submitted_by.id,
        )
        db.add(target)
    else:
        target.data_json = data_json
        target.submitted_by = submitted_by.id
        target.submitted_at = datetime.utcnow()
    return target


def seed_disbursement_forms(db, app, officer):
    forms = {
        "loan_application": {
            "customer_status": "New",
            "product_type": "Enterprise Loan",
            "full_name": app.borrower.name,
            "telephone_no": app.borrower.phone,
            "bvn": app.borrower.bvn,
            "home_address": app.borrower.physical_address,
            "loan_purpose": "Working capital for retail inventory expansion",
            "loan_amount": str(app.amount),
            "loan_tenor": str(app.tenure),
            "repayment_mode": "Direct Debit",
            "disbursement_account_name": app.borrower.name,
            "disbursement_account_number": app.borrower.account_number,
            "disbursement_bank_name": app.borrower.bank_name,
            "credit_bureau_consent": True,
            "credit_check_authorized": True,
            "cheque_repayment_authorized": True,
            "gsi_mandate_authorized": True,
        },
        "guarantor": {
            "customer_status": "New",
            "full_name": app.borrower.guarantor_name or "Tunde Adewale",
            "relationship_to_client": "Business associate",
            "telephone_no": app.borrower.guarantor_phone or "08055550101",
            "bvn": "22334455667",
            "home_address": "18 Allen Avenue, Ikeja, Lagos",
            "education_level": "Graduate",
            "employment_status": "Self Employed",
            "type_of_business": "Wholesale provisions",
            "average_monthly_sales": "1800000",
            "maximum_limit_figures": str(app.amount),
            "maximum_limit_words": "Five Hundred Thousand Naira Only",
            "recovery_bank_name": "GTBank",
            "recovery_account_no": "0123000001",
            "declaration_terms_confirmed": True,
            "pledged_items": "1 | Shop stock | STK-001 | Provisions and household goods | 750000",
        },
        "pledge_trust_receipt": {
            "receipt_date": datetime.utcnow().date().isoformat(),
            "given_by": app.borrower.name,
            "facility_amount_figures": str(app.amount),
            "facility_amount_words": "Five Hundred Thousand Naira Only",
            "borrower_or_association_name": app.borrower.name,
            "pledged_location": app.borrower.physical_address,
            "obligor_name": app.borrower.name,
            "charged_shop_location": "Shop B12, Tejuosho Market, Lagos",
            "continuing_security_confirmed": True,
            "pledged_items_schedule": "1 | Retail stock | Various | Provisions and household goods | 850000",
            "legal_mortgage_charge_acknowledged": True,
            "signed_by": app.borrower.name,
        },
    }

    names = {
        "loan_application": "Loan Application Form",
        "guarantor": "Guarantors Form",
        "pledge_trust_receipt": "Pledge and Trust Receipt",
    }

    for form_type, data in forms.items():
        upsert_stage_data(
            db,
            app,
            6,
            officer,
            {
                "record_type": "disbursement_form",
                "form_type": form_type,
                "form_name": names[form_type],
                "data": data,
                "completed": True,
            },
        )


def seed_repayment_schedule(db, app, first_due_days=7):
    if db.query(RepaymentSchedule).filter(RepaymentSchedule.application_id == app.id).first():
        return
    total_amount = app.amount * (1 + (app.interest_rate / 100))
    installment_value = total_amount / app.tenure
    interval_days = 7 if app.repayment_frequency.lower() == "weekly" else 30
    due = datetime.utcnow() + timedelta(days=first_due_days)
    schedules = []
    for _ in range(app.tenure):
        schedule = RepaymentSchedule(
            application_id=app.id,
            due_date=due,
            amount_due=installment_value,
            status="Pending",
        )
        db.add(schedule)
        schedules.append(schedule)
        due += timedelta(days=interval_days)
    db.flush()
    return schedules


def main():
    if not settings.DATABASE_URL:
        raise RuntimeError("DATABASE_URL must be configured before running scripts/seed_mvp.py")

    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        password_hash = security.get_password_hash(PASSWORD)

        org = db.query(Organisation).filter(Organisation.name == "Mainstreet Microfinance Bank Limited").first()
        if not org:
            org = Organisation(
                name="Mainstreet Microfinance Bank Limited",
                type="MFB",
                subscription_tier="enterprise",
                config_json={"form_codes": ["MMFB/CRM/01", "MMFB/CRM/02", "MMFB/CRM/03"]},
            )
            db.add(org)
            db.flush()

        branch = db.query(Branch).filter(Branch.org_id == org.id, Branch.name == "Broad Street Branch").first()
        if not branch:
            branch = Branch(org_id=org.id, name="Broad Street Branch", location="94 Broad Street, Lagos State")
            db.add(branch)
            db.flush()

        admin = get_or_create_user(db, org, branch, name="MVP Admin", phone="08010000000", role="System Admin", password_hash=password_hash)
        manager = get_or_create_user(db, org, branch, name="Adebayo Johnson", phone="08010000001", role="Branch Manager", password_hash=password_hash)
        officer = get_or_create_user(db, org, branch, name="Chidi Obi", phone="08010000002", role="Loan Officer", password_hash=password_hash)
        credit = get_or_create_user(db, org, branch, name="Fatima Bello", phone="08010000003", role="Credit Officer", password_hash=password_hash)
        auditor = get_or_create_user(db, org, branch, name="Samuel Okafor", phone="08010000004", role="Auditor", password_hash=password_hash)
        branch.manager_id = manager.id

        borrowers = [
            get_or_create_borrower(
                db,
                org,
                officer,
                name="Grace Omowunmi",
                phone="08020000001",
                bvn="22233344455",
                nin="99988877766",
                status="Active",
                gps_coordinates="6.5244 N, 3.3792 E",
                physical_address="12 Herbert Macaulay Way, Yaba, Lagos",
                employment_status="Self Employed",
                monthly_income=450000.0,
                bank_name="GTBank",
                account_number="0123456789",
                guarantor_name="Tunde Adewale",
                guarantor_phone="08055550101",
            ),
            get_or_create_borrower(
                db,
                org,
                officer,
                name="Ibrahim Musa",
                phone="08020000002",
                bvn="11122233344",
                nin="88877766655",
                status="Applicant",
                gps_coordinates="6.5244 N, 3.3792 E",
                physical_address="45 Sabo Market Road, Yaba, Lagos",
                employment_status="Trader",
                monthly_income=300000.0,
                bank_name="Access Bank",
                account_number="0987654321",
                guarantor_name="Aisha Musa",
                guarantor_phone="08055550102",
            ),
            get_or_create_borrower(
                db,
                org,
                officer,
                name="Ngozi Eze",
                phone="08020000003",
                bvn="33344455566",
                nin="77766655544",
                status="Applicant",
                physical_address="7 Market Street, Surulere, Lagos",
                employment_status="Salary Earner",
                employer_name="Bluechip Retail Ltd",
                monthly_income=520000.0,
                bank_name="Zenith Bank",
                account_number="0044556677",
                guarantor_name="Emeka Eze",
                guarantor_phone="08055550103",
            ),
        ]

        pending_disbursement = create_application(
            db,
            org,
            borrowers[0],
            manager,
            current_stage=6,
            status="Approved - Pending Disbursement",
            amount=500000.0,
            tenure=10,
            product_type="Enterprise Loan",
            interest_rate=7.5,
            repayment_frequency="Weekly",
            collateral_desc="Shop stock and pledged household appliances",
            collateral_value=850000.0,
            officer_recommendation="Strong daily turnover and verified guarantor capacity.",
        )
        seed_disbursement_forms(db, pending_disbursement, officer)

        active_app = create_application(
            db,
            org,
            borrowers[1],
            manager,
            current_stage=6,
            status="Active",
            amount=250000.0,
            tenure=8,
            product_type="MSEF",
            interest_rate=6.0,
            repayment_frequency="Weekly",
            collateral_desc="POS terminal and stock inventory",
            collateral_value=400000.0,
            officer_recommendation="Existing trading cash flow supports weekly repayment.",
        )
        schedules = seed_repayment_schedule(db, active_app)
        if schedules:
            paid = RepaymentRecord(
                schedule_id=schedules[0].id,
                application_id=active_app.id,
                amount_paid=schedules[0].amount_due,
                channel="transfer",
                officer_id=officer.id,
            )
            schedules[0].status = "Paid"
            db.add(paid)

        stage_apps = [
            (borrowers[2], officer, 1, "Draft", 180000.0, "PAYEE"),
            (borrowers[1], manager, 2, "In Review - Stage 2", 350000.0, "Enterprise Loan"),
            (borrowers[2], credit, 3, "In Review - Stage 3", 420000.0, "Enterprise Loan"),
            (borrowers[0], auditor, 4, "In Review - Stage 4", 300000.0, "MSEF"),
            (borrowers[1], admin, 5, "In Review - Stage 5", 600000.0, "Enterprise Loan"),
        ]
        for borrower, owner, stage, status, amount, product_type in stage_apps:
            create_application(
                db,
                org,
                borrower,
                owner,
                current_stage=stage,
                status=status,
                amount=amount,
                tenure=6,
                product_type=product_type,
                interest_rate=5.0,
                repayment_frequency="Weekly",
                collateral_desc="Inventory and guarantor undertaking",
                collateral_value=amount * 1.5,
                officer_recommendation="Seeded MVP workflow sample for role-based review.",
            )

        group = db.query(Group).filter(Group.org_id == org.id, Group.name == "Broad Street Traders Ajo").first()
        if not group:
            group = Group(
                org_id=org.id,
                name="Broad Street Traders Ajo",
                type="ajo",
                leader_id=borrowers[0].id,
                meeting_schedule="Mondays 9:00 AM",
                territory="Lagos Island",
            )
            db.add(group)
            db.flush()
        for borrower in borrowers:
            member = db.query(GroupMember).filter(GroupMember.group_id == group.id, GroupMember.borrower_id == borrower.id).first()
            if not member:
                db.add(GroupMember(group_id=group.id, borrower_id=borrower.id, status="Active"))

        for borrower in borrowers:
            exists = db.query(CommunicationLog).filter(CommunicationLog.borrower_id == borrower.id).first()
            if not exists:
                db.add(
                    CommunicationLog(
                        borrower_id=borrower.id,
                        type="Field Visit",
                        outcome="Address Verified",
                        note="Premises and business activity verified for MVP demo.",
                        officer_id=officer.id,
                    )
                )

        for app in db.query(LoanApplication).filter(LoanApplication.org_id == org.id).all():
            add_stage_event(db, app, officer, "Seed MVP Record", None, app.current_stage, f"MVP seed data for {app.status}.")

        db.commit()
        print("MVP seed complete.")
        print("Organisation: Mainstreet Microfinance Bank Limited")
        print("Login password for all seeded staff: password123")
        print("Staff phones: admin 08010000000, manager 08010000001, loan officer 08010000002, credit 08010000003, auditor 08010000004")
        print("Borrower portal sample: phone 08020000001, BVN 22233344455")
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


if __name__ == "__main__":
    main()
