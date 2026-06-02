import sys
import os
from datetime import datetime, timedelta

# Add backend directory to Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "backend"))

from app.db.session import SessionLocal, engine, Base
from app.db.models import Organisation, User, Branch, Borrower, LoanApplication, RepaymentSchedule, WorkflowEvent
from app.core import security

def bootstrap():
    print("Initializing local SQLite database tables...")
    Base.metadata.create_all(bind=engine)
    
    db = SessionLocal()
    try:
        # Check if already bootstrapped
        if db.query(Organisation).first():
            print("[OK] Database already bootstrapped with demo data!")
            return
            
        print("Populating MFB demo entities...")
        
        # 1. Create Organisation
        org = Organisation(
            name="Lagos Trust Microfinance Bank",
            type="MFB",
            subscription_tier="enterprise"
        )
        db.add(org)
        db.flush()
        
        # 2. Create HQ Branch
        branch = Branch(
            org_id=org.id,
            name="Lagos Mainland HQ",
            location="Yaba, Lagos"
        )
        db.add(branch)
        db.flush()
        
        # 3. Create Users (hashed passwords)
        admin_pwd = security.get_password_hash("password123")
        
        # Branch Manager
        manager = User(
            org_id=org.id,
            branch_id=branch.id,
            name="Adebayo Johnson",
            phone="08012345678",
            role="Branch Manager",
            hashed_password=admin_pwd
        )
        db.add(manager)
        db.flush()
        branch.manager_id = manager.id
        
        # Loan Officer
        officer = User(
            org_id=org.id,
            branch_id=branch.id,
            name="Chidi Obi",
            phone="08087654321",
            role="Loan Officer",
            hashed_password=admin_pwd
        )
        db.add(officer)
        db.flush()

        # Credit Officer
        risk = User(
            org_id=org.id,
            branch_id=branch.id,
            name="Fatima Bello",
            phone="08011223344",
            role="Credit Officer",
            hashed_password=admin_pwd
        )
        db.add(risk)
        
        # Auditor
        auditor = User(
            org_id=org.id,
            branch_id=branch.id,
            name="Auditor Samuel",
            phone="08055667788",
            role="Auditor",
            hashed_password=admin_pwd
        )
        db.add(auditor)
        db.flush()
        
        # 4. Create Borrowers
        borrower1 = Borrower(
            org_id=org.id,
            loan_officer_id=officer.id,
            name="Grace Omowunmi",
            phone="08022334455",
            bvn="22233344455",
            nin="99988877766",
            status="Active",
            gps_coordinates="6.5244° N, 3.3792° E",
            physical_address="12, Herbert Macaulay Way, Yaba",
            monthly_income=120000.0,
            bank_name="GTBank",
            account_number="0123456789"
        )
        db.add(borrower1)
        
        borrower2 = Borrower(
            org_id=org.id,
            loan_officer_id=officer.id,
            name="Ibrahim Musa",
            phone="08033445566",
            bvn="11122233344",
            nin="88877766655",
            status="Applicant",
            gps_coordinates="6.5244° N, 3.3792° E",
            physical_address="45, Sabo Market Road, Yaba",
            monthly_income=85000.0,
            bank_name="Access Bank",
            account_number="0987654321"
        )
        db.add(borrower2)
        db.flush()
        
        # 5. Create Loan Applications
        # Active Disbursed Loan
        app1 = LoanApplication(
            org_id=org.id,
            borrower_id=borrower1.id,
            current_stage=6,
            current_owner_id=manager.id,
            status="Active",
            amount=150000.0,
            tenure=4,
            product_type="Individual",
            interest_rate=5.0,
            repayment_frequency="Weekly",
            officer_recommendation="Borrower has high seasonal cash flows from trading."
        )
        db.add(app1)
        db.flush()
        
        # Repayment schedule for active loan
        for i in range(1, 5):
            due = datetime.utcnow() + timedelta(days=7 * i)
            schedule = RepaymentSchedule(
                application_id=app1.id,
                due_date=due,
                amount_due=39375.0, # 150k + 5% interest divided by 4
                status="Pending"
            )
            db.add(schedule)
            
        # Application In Review (Stage 2)
        app2 = LoanApplication(
            org_id=org.id,
            borrower_id=borrower2.id,
            current_stage=2,
            current_owner_id=manager.id,
            status="In Review - Stage 2",
            amount=80000.0,
            tenure=8,
            product_type="Emergency",
            interest_rate=4.0,
            repayment_frequency="Bi-weekly",
            officer_recommendation="Emergency school fee request. Collateral verified."
        )
        db.add(app2)
        
        # Workflow event logs (NDPC compliance log)
        event = WorkflowEvent(
            application_id=app1.id,
            action="Final Approve",
            from_stage=5,
            to_stage=6,
            actor_id=manager.id,
            reason="MCR Committee voted unanimously to approve terms."
        )
        db.add(event)
        
        db.commit()
        print("\n[OK] SUCCESS: Local SQLite database initialized successfully!")
        print("==================================================")
        print("DEMO TEST CREDENTIALS:")
        print("- Phone Number (Login): 08012345678 (Branch Manager)")
        print("- Phone Number (Login): 08087654321 (Loan Officer)")
        print("- Password (All Users): password123")
        print("\nBORROWER PORTAL LOGIN:")
        print(f"- Phone Number: 08022334455")
        print(f"- BVN: 22233344455")
        print("==================================================")
        
    except Exception as e:
        db.rollback()
        print("[ERROR] BOOTSTRAP FAILED: " + str(e))
    finally:
        db.close()

if __name__ == "__main__":
    bootstrap()
