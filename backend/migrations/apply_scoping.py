import os
import sys
import psycopg2
from uuid import UUID

# Import get_database_url from run_migration
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from run_migration import get_database_url

def main():
    db_url = get_database_url()
    conn = psycopg2.connect(db_url)
    conn.autocommit = False
    cursor = conn.cursor()
    
    try:
        # 1. Run migrations first (using the updated history tracking runner)
        from run_migration import run_migrations
        run_migrations()
        
        print("\n=== VERIFYING CURRENT DATABASE STATE BEFORE SCOPING ===")
        
        # 2. Verify and pull Organization data
        cursor.execute("SELECT id, name FROM organisations LIMIT 1;")
        org_row = cursor.fetchone()
        if not org_row:
            print("Error: No organization found in the database. Seeding is required.")
            sys.exit(1)
        org_id = org_row[0]
        print(f"Verified Org: {org_row[1]} (ID: {org_id})")
        
        # 3. Pull and display current User data
        cursor.execute("SELECT id, full_name, role, branch_id FROM users ORDER BY role, full_name;")
        existing_users = cursor.fetchall()
        print(f"Found {len(existing_users)} existing users in DB:")
        branch_scoped_count = 0
        for u in existing_users:
            b_scoped = f"Branch ID: {u[3]}" if u[3] else "Global / Unassigned"
            print(f"  - User: {u[1]} | Role: {u[2]} | {b_scoped}")
            if u[2] in ('branch_manager', 'loan_officer'):
                branch_scoped_count += 1
                
        if branch_scoped_count == 0:
            print("Warning: No branch-scoped roles ('branch_manager' or 'loan_officer') found to update.")
            
        # 4. Pull and display current Loan Applications data
        cursor.execute("SELECT id, applicant_name, created_by, branch_id FROM loan_applications ORDER BY applicant_name;")
        existing_loans = cursor.fetchall()
        print(f"Found {len(existing_loans)} existing loans in DB:")
        for l in existing_loans:
            b_scoped = f"Branch ID: {l[3]}" if l[3] else "Global / Unassigned"
            print(f"  - Loan: {l[1]} | Created By ID: {l[2]} | {b_scoped}")

        print("\n=== APPLYING SCOPING UPDATES ===")

        # 5. Create the "Test Branch" if it does not exist
        cursor.execute(
            "INSERT INTO branches (org_id, name, code, active) VALUES (%s, %s, %s, TRUE) "
            "ON CONFLICT (org_id, code) DO UPDATE SET name = EXCLUDED.name RETURNING id;",
            (org_id, "Test Branch", "TB001")
        )
        branch_id = cursor.fetchone()[0]
        print(f"Target Branch 'Test Branch' verified (ID: {branch_id})")
        
        # 6. Scope users under Test Branch
        cursor.execute(
            "UPDATE users SET branch_id = %s WHERE role IN ('branch_manager', 'account_officer') RETURNING id, full_name, role;",
            (branch_id,)
        )
        updated_users = cursor.fetchall()
        print(f"Successfully scoped {len(updated_users)} users under Test Branch.")
        
        # 7. Fetch active Relationship Officers (account_officers)
        cursor.execute("SELECT id, full_name FROM users WHERE role = 'account_officer' AND branch_id = %s;", (branch_id,))
        ro_users = cursor.fetchall()
        ro_ids = [r[0] for r in ro_users] if ro_users else []
        print(f"Active ROs available for assignment: {len(ro_ids)}")
        
        # 8. Scope loan applications under Test Branch
        cursor.execute(
            "UPDATE loan_applications SET branch_id = %s RETURNING id, applicant_name;",
            (branch_id,)
        )
        updated_loans = cursor.fetchall()
        print(f"Successfully scoped {len(updated_loans)} loans under Test Branch.")
        
        # 9. Distribute loans round-robin style to available ROs
        if ro_ids and updated_loans:
            for idx, loan in enumerate(updated_loans):
                loan_id = loan[0]
                target_ro_id = ro_ids[idx % len(ro_ids)]
                cursor.execute(
                    "UPDATE loan_applications SET created_by = %s, current_owner_id = %s WHERE id = %s;",
                    (target_ro_id, target_ro_id, loan_id)
                )
            print(f"Successfully distributed {len(updated_loans)} loans among available ROs.")
        else:
            print("Skipped loan distribution (no ROs or loans found for assignment).")
            
        conn.commit()
        print("\nScoping and migration successfully applied and committed!")
    except Exception as e:
        conn.rollback()
        print(f"\nFailed to apply scoping updates: {e}")
        raise
    finally:
        conn.close()

if __name__ == "__main__":
    main()
