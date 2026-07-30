import os
import sys
import psycopg2
from fastapi.testclient import TestClient

sys.path.insert(0, "backend")
from app.main import app as fastapi_app
from app.core.security import get_password_hash

# Let's override password verification for the test to ensure we can log in with any user!
# We can mock verify_password to always return True for this test script!
import app.core.security
app.core.security.verify_password = lambda plain, hashed: True

client = TestClient(fastapi_app)

USERS_TO_TEST = [
    ("mezeobidi@mainstreetmfb.com", "account_officer", "/dashboard"),
    ("danielbakare550@gmail.com", "auditor", "/dashboard"),
    ("sadewale@mainstreetmfb.com", "branch_manager", "/dashboard"),
    ("juchenna@mainstreetmfb.com", "branch_supervisor", "/dashboard"),
    ("m.o.j.muritalaopeoluwajoel@gmail.com", "credit_analyst", "/dashboard"),
    ("ooyewole@mainstreetmfb.com", "crm", "/crm-dashboard"),
    ("unnenna@mainstreetmfb.com", "ed", "/ed-dashboard"),
    ("sukaogo@mainstreetmfb.com", "md", "/md-dashboard"),
    ("muritalaopeoluwa10@gmail.com", "system_admin", "/dashboard")
]

def test_dashboards():
    for email, role, path in USERS_TO_TEST:
        login_res = client.post("/api/v1/auth/login", data={"username": email, "password": "any"})
        if login_res.status_code != 200:
            print(f"Failed to login {email}: {login_res.status_code}")
            continue
        
        token = login_res.json().get("access_token")
        client.headers.update({"Authorization": f"Bearer {token}"})
        
        # Test full page
        res = client.get(path, headers={"X-Progressive-Load": "true"})
        print(f"Role {role:20} Path {path:15} Progressive Status: {res.status_code} Content Length: {len(res.text)}")
        if res.status_code != 200:
            print(res.text[:500])

if __name__ == "__main__":
    test_dashboards()
