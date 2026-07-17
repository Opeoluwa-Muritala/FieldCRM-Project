import asyncio
import os
import sys
from pathlib import Path
from unittest.mock import patch

# Add backend directory to Python path
sys.path.insert(0, str(Path(__file__).resolve().parent))

from app.core.database import init_pool, close_pool, get_connection
from app.config import settings
from app.domains.credit_bureau.service import CreditBureauService, CreditRegistryProvider, CrcProvider

async def test_bureau_provider_routing():
    print("Testing Bureau Provider Routing...")
    
    # 1. Test when CRC_API_KEY is None (default CreditRegistry)
    with patch.object(settings, "CRC_API_KEY", None):
        svc = CreditBureauService()
        assert isinstance(svc.provider, CreditRegistryProvider), "Should use CreditRegistry by default"
        assert svc.provider_name == "creditregistry", "Provider name should be creditregistry"
        
    # 2. Test when CRC_API_KEY is configured
    with patch.object(settings, "CRC_API_KEY", "mock_crc_api_key_123"):
        svc = CreditBureauService()
        assert isinstance(svc.provider, CrcProvider), "Should use CRC when API key is set"
        assert svc.provider_name == "crc", "Provider name should be crc"
        
    print("[OK] Bureau Provider Routing works perfectly.")

async def test_mock_responses():
    print("\nTesting Provider Mock Responses...")
    
    # Init pool to query database later
    await init_pool()
    try:
        async with get_connection() as conn:
            # Get an application ID to test database inserts
            row = await conn.fetchrow("SELECT id, applicant_name, bvn, phone FROM loan_applications LIMIT 1;")
            if not row:
                print("[SKIP] No loan applications found in database to run integration test.")
                return
                
            loan_id = str(row["id"])
            applicant_name = row["applicant_name"]
            bvn = row["bvn"]
            phone = row["phone"]
            
            # --- Test CreditRegistry ---
            with patch.object(settings, "CRC_API_KEY", None):
                svc = CreditBureauService(conn)
                session = await svc.get_session_code()
                assert session.startswith("mock_"), "CreditRegistry session code should be mock"
                
                registry_id = await svc.find_customer(session, bvn, phone, applicant_name)
                assert registry_id == "mock_registry_id_999888", "CreditRegistry registry id mismatch"
                
                report = await svc.get_report(loan_id, registry_id, session)
                assert report["status"] == "success", "CreditRegistry pull failed"
                assert report["data"]["is_approximate_placeholder"] is True, "Should be approximate placeholder"
                
                # Check DB insert
                db_row = await conn.fetchrow(
                    "SELECT provider, report_type FROM bureau_submissions WHERE loan_application_id = $1 ORDER BY submitted_at DESC LIMIT 1;",
                    row["id"]
                )
                assert db_row["provider"] == "creditregistry", "DB provider should be creditregistry"
                
            # --- Test CRC Mock Flow ---
            # Instantiate service and manually force CrcProvider with CRC_API_KEY as None (mock mode)
            svc = CreditBureauService(conn)
            svc.provider = CrcProvider(conn, svc)
            
            session = await svc.get_session_code()
            assert session == "mock_session_code_crc_123", "CRC session code should be mock"
            
            crc_id = await svc.find_customer(session, bvn, phone, applicant_name)
            assert crc_id == "mock_crc_id_777666", "CRC customer find mismatch"
            
            report = await svc.get_report(loan_id, crc_id, session)
            assert report["status"] == "success", "CRC pull failed"
            assert report["data"]["is_approximate_placeholder"] is True, "Should be approximate placeholder"
            
            # Check DB insert
            db_row = await conn.fetchrow(
                "SELECT provider, report_type FROM bureau_submissions WHERE loan_application_id = $1 ORDER BY submitted_at DESC LIMIT 1;",
                row["id"]
            )
            assert db_row["provider"] == "crc", "DB provider should be crc"
                
            print("[OK] Provider Mock Responses and DB inserts verified.")
    finally:
        await close_pool()

async def main():
    try:
        await test_bureau_provider_routing()
        await test_mock_responses()
        print("\nAll Credit Bureau provider tests passed successfully!")
    except Exception as e:
        import traceback
        traceback.print_exc()
        print(f"\n[FAIL] Test suite failed: {e}")
        sys.exit(1)

if __name__ == "__main__":
    asyncio.run(main())
