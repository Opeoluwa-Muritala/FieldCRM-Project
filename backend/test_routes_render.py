import asyncio
import sys
import os
import httpx

# Add backend directory to Python path
sys.path.insert(0, os.path.dirname(__file__))

from app.core.database import init_pool, close_pool, get_connection
from app.config import settings

ROLES = {
    "system_admin": ("admin@mmfb.com", "password123"),
    "branch_manager": ("adebayo@mmfb.com", "password123"),
    "loan_officer": ("chidi@mmfb.com", "password123"),
    "credit_officer": ("fatima@mmfb.com", "password123"),
    "auditor": ("samuel@mmfb.com", "password123"),
}

UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
UA_MOBILE = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1"

async def get_test_data():
    await init_pool()
    loan_id = None
    try:
        async with get_connection() as conn:
            # Get the first loan application ID from the database
            row = await conn.fetchrow("SELECT id FROM loan_applications LIMIT 1;")
            if row:
                loan_id = str(row["id"])
    except Exception as e:
        print(f"Error querying database: {e}")
    finally:
        await close_pool()
    return loan_id

async def test_role_views(loan_id):
    base_url = "http://127.0.0.1:8000"
    errors = []

    print("\n==================================================")
    print("STARTING ROLE-BASED & DEVICE-AWARE RENDER TESTS")
    print("==================================================")

    for role, (email, password) in ROLES.items():
        print(f"\n--- Testing Role: {role} ({email}) ---")

        # Create httpx client with cookies enabled
        async with httpx.AsyncClient(base_url=base_url, follow_redirects=False, timeout=60.0) as client:
            # 1. Login
            login_data = {"username": email, "password": password}
            res = await client.post("/login", data=login_data)
            if res.status_code != 303:
                errors.append(f"[{role}] Login failed: status {res.status_code}")
                continue
            
            # Check session cookie exists
            session_cookie = client.cookies.get("session")
            if not session_cookie:
                errors.append(f"[{role}] Session cookie not set after login")
                continue

            # 2. Test Dashboard on Desktop
            headers_desktop = {"User-Agent": UA_DESKTOP}
            res_dash_desk = await client.get("/dashboard", headers=headers_desktop)
            if res_dash_desk.status_code != 200:
                errors.append(f"[{role}] [Desktop] Dashboard failed: status {res_dash_desk.status_code}")
            else:
                body = res_dash_desk.text
                # Assert desktop shell is present
                if "desktop-shell" not in body or "desktop-sidebar" not in body:
                    errors.append(f"[{role}] [Desktop] Dashboard missing desktop-shell or desktop-sidebar")
                # Assert mobile elements are not present (not rendering tab bar)
                if "mobile-shell" in body or "mobile-tabbar" in body:
                    errors.append(f"[{role}] [Desktop] Dashboard incorrectly contains mobile elements")
                
                # Check role visibility specifics
                if role == "loan_officer":
                    if "Concur" in body or "Approve" in body or "Reject" in body:
                        errors.append(f"[{role}] [Desktop] Loan Officer dashboard incorrectly contains concur/approve/reject actions")
                elif role == "auditor":
                    if "Save" in body or "Submit" in body or "Upload" in body:
                        errors.append(f"[{role}] [Desktop] Auditor dashboard incorrectly contains save/submit/upload actions")

            # 3. Test Dashboard on Mobile
            headers_mobile = {"User-Agent": UA_MOBILE}
            res_dash_mob = await client.get("/dashboard", headers=headers_mobile)
            if res_dash_mob.status_code != 200:
                errors.append(f"[{role}] [Mobile] Dashboard failed: status {res_dash_mob.status_code}")
            else:
                body = res_dash_mob.text
                # Assert mobile shell and tab bar are present
                if "mobile-shell" not in body or "mobile-tabbar" not in body:
                    errors.append(f"[{role}] [Mobile] Dashboard missing mobile-shell or mobile-tabbar")
                # Assert desktop elements are not present (no desktop sidebar)
                if "desktop-sidebar" in body:
                    errors.append(f"[{role}] [Mobile] Dashboard incorrectly contains desktop sidebar")

            # 4. Test Application Detail Page (Desktop)
            if loan_id:
                res_detail_desk = await client.get(f"/applications/{loan_id}", headers=headers_desktop)
                if res_detail_desk.status_code != 200:
                    errors.append(f"[{role}] [Desktop] Detail page failed: status {res_detail_desk.status_code}")
                else:
                    body = res_detail_desk.text
                    if "desktop-shell" not in body or "app-detail-layout" not in body:
                        errors.append(f"[{role}] [Desktop] Detail page missing desktop shell or detail layout")
                    
                    # Verify role isolation on the detail page
                    if role == "loan_officer":
                        if "Concur" in body or "Approve" in body or "Reject" in body:
                            errors.append(f"[{role}] [Desktop] Detail: Loan Officer has concurrence/approval buttons")
                    elif role == "auditor":
                        if "Save" in body or "Submit" in body or "Upload" in body or "Concur" in body:
                            errors.append(f"[{role}] [Desktop] Detail: Auditor has active modifications/approval buttons")

                # 5. Test Application Detail Page (Mobile - Redirects to appropriate stage view)
                res_detail_mob = await client.get(f"/applications/{loan_id}", headers=headers_mobile)
                # Should be a 303 Redirect to a stage-specific page
                if res_detail_mob.status_code != 303:
                    errors.append(f"[{role}] [Mobile] Detail page did not redirect: status {res_detail_mob.status_code}")
                else:
                    redirect_url = res_detail_mob.headers.get("Location")
                    print(f"[{role}] [Mobile] Detail page redirected successfully to: {redirect_url}")
                    # Follow the redirect
                    res_redirected = await client.get(redirect_url, headers=headers_mobile, follow_redirects=True)
                    if res_redirected.status_code != 200:
                        errors.append(f"[{role}] [Mobile] Redirected view {redirect_url} failed: status {res_redirected.status_code}")
                    else:
                        body = res_redirected.text
                        if "mobile-shell" not in body or "mobile-tabbar" not in body:
                            errors.append(f"[{role}] [Mobile] Redirected view {redirect_url} missing mobile elements")

    print("\n==================================================")
    print("VERIFICATION COMPLETED")
    if errors:
        print(f"FAILED WITH {len(errors)} ERRORS:")
        for err in errors:
            print(f"- {err}")
        sys.exit(1)
    else:
        print("ALL TESTS PASSED SUCCESSFULLY! NO ERRORS DETECTED.")
        sys.exit(0)

if __name__ == "__main__":
    loan_id = asyncio.run(get_test_data())
    print(f"Found active loan application ID for testing: {loan_id}")
    asyncio.run(test_role_views(loan_id))
