import httpx
import logging
import json
from decimal import Decimal
from app.config import settings

logger = logging.getLogger("CreditBureauService")

class CreditRegistryProvider:
    def __init__(self, conn, service):
        self.conn = conn
        self.service = service
        self.base_url = settings.CREDIT_REGISTRY_BASE_URL.rstrip('/')
        self.name = "creditregistry"

    async def get_session_code(self) -> str:
        """Acquires a session code from CreditRegistry /api/Login."""
        if not settings.CREDIT_REGISTRY_USERNAME or not settings.CREDIT_REGISTRY_PASSWORD:
            return "mock_session_code_12345"

        url = f"{self.base_url}/api/Login"
        payload = {
            "Username": settings.CREDIT_REGISTRY_USERNAME,
            "Password": settings.CREDIT_REGISTRY_PASSWORD
        }
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(url, json=payload)
                if response.status_code == 200:
                    data = response.json()
                    return data.get("SessionCode") or data.get("Token") or "mock_session_code_response"
                else:
                    logger.error(f"CreditRegistry Login failed: {response.status_code} - {response.text}")
                    return ""
        except Exception as e:
            logger.error(f"CreditRegistry Login error: {e}")
            return ""

    async def find_customer(self, session_code: str, bvn: str, phone: str = None, name: str = None) -> str:
        """Finds RegistryID for customer via /api/FindSummary."""
        if not settings.CREDIT_REGISTRY_USERNAME or session_code.startswith("mock_"):
            return "mock_registry_id_999888"

        url = f"{self.base_url}/api/FindSummary"
        headers = {"Authorization": f"Bearer {session_code}"}
        payload = {
            "BVN": bvn,
            "PhoneNumber": phone,
            "FullName": name
        }
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(url, json=payload, headers=headers)
                if response.status_code == 200:
                    data = response.json()
                    return data.get("RegistryID") or data.get("customer", {}).get("RegistryID") or "mock_registry_id_from_api"
                else:
                    logger.error(f"Find customer failed: {response.status_code} - {response.text}")
                    return ""
        except Exception as e:
            logger.error(f"Find customer error: {e}")
            return ""

    async def get_report(self, loan_application_id: str, registry_id: str, session_code: str) -> dict:
        """Retrieves credit report via /api/GetReport200 and persists to DB."""
        if not settings.CREDIT_REGISTRY_USERNAME or session_code.startswith("mock_"):
            raw_report = {
                "status": "success",
                "registry_id": registry_id,
                "report_type": "AutoCred_v8_Summary",
                "data": {
                    "is_approximate_placeholder": True,
                    "score": 680,
                    "active_loans_count": 2,
                    "total_outstanding_balance": 1200000.0,
                    "total_monthly_repayments": 85000.0,
                    "total_delinquent_accounts": 0,
                    "worst_payment_status": "performing"
                }
            }
            await self.service._save_submission(loan_application_id, registry_id, "success", "AutoCred_v8_Summary", raw_report, self.name)
            return raw_report

        url = f"{self.base_url}/api/GetReport200"
        headers = {"Authorization": f"Bearer {session_code}"}
        payload = {"RegistryID": registry_id}
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                response = await client.post(url, json=payload, headers=headers)
                if response.status_code == 200:
                    raw_report = response.json()
                    await self.service._save_submission(loan_application_id, registry_id, "success", "AutoCred_v8_Summary", raw_report, self.name)
                    return raw_report
                else:
                    raw_err = {"status_code": response.status_code, "body": response.text}
                    await self.service._save_submission(loan_application_id, registry_id, "failed", "AutoCred_v8_Summary", raw_err, self.name)
                    return raw_err
        except Exception as e:
            logger.error(f"Get credit report error: {e}")
            raw_err = {"error": str(e)}
            await self.service._save_submission(loan_application_id, registry_id, "error", "AutoCred_v8_Summary", raw_err, self.name)
            return raw_err

    async def submit_account(self, loan_application_id: str, session_code: str, loan_data: dict) -> dict:
        """Pushes disbursement details outward via /api/AddUpdateAccount."""
        if not settings.CREDIT_REGISTRY_USERNAME or session_code.startswith("mock_"):
            raw_response = {
                "success": True,
                "message": "Disbursed loan reported to CreditRegistry successfully (mock mode).",
                "reported_at": "now"
            }
            await self.service._save_submission(loan_application_id, "mock_registry_id_999888", "disbursed_reported", "add_update_account", raw_response, self.name)
            return raw_response

        person_url = f"{self.base_url}/api/AddUpdatePerson"
        account_url = f"{self.base_url}/api/AddUpdateAccount"
        headers = {"Authorization": f"Bearer {session_code}"}

        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                person_res = await client.post(person_url, json=loan_data.get("person", {}), headers=headers)
                account_res = await client.post(account_url, json=loan_data.get("account", {}), headers=headers)
                
                raw_response = {
                    "person_response": person_res.json() if person_res.status_code == 200 else person_res.text,
                    "account_response": account_res.json() if account_res.status_code == 200 else account_res.text,
                    "status_code_person": person_res.status_code,
                    "status_code_account": account_res.status_code
                }
                
                status_str = "success" if (person_res.status_code == 200 and account_res.status_code == 200) else "failed"
                await self.service._save_submission(
                    loan_application_id,
                    loan_data.get("person", {}).get("RegistryID"),
                    status_str,
                    "add_update_account",
                    raw_response,
                    self.name
                )
                return raw_response
        except Exception as e:
            logger.error(f"Submit account to credit registry error: {e}")
            raw_err = {"error": str(e)}
            await self.service._save_submission(loan_application_id, None, "error", "add_update_account", raw_err, self.name)
            return raw_err


class CrcProvider:
    def __init__(self, conn, service):
        self.conn = conn
        self.service = service
        self.base_url = settings.CRC_BASE_URL.rstrip('/')
        self.name = "crc"

    async def get_session_code(self) -> str:
        """Acquires a session code/token from CRC."""
        if not settings.CRC_API_KEY:
            return "mock_session_code_crc_123"

        url = f"{self.base_url}/api/v1/token"
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(url, json={"api_key": settings.CRC_API_KEY})
                if response.status_code == 200:
                    data = response.json()
                    return data.get("token") or "mock_crc_token"
                else:
                    logger.error(f"CRC Token fetch failed: {response.status_code} - {response.text}")
                    return ""
        except Exception as e:
            logger.error(f"CRC Token fetch error: {e}")
            return ""

    async def find_customer(self, session_code: str, bvn: str, phone: str = None, name: str = None) -> str:
        """Finds CRC ID for customer."""
        if not settings.CRC_API_KEY or session_code.startswith("mock_"):
            return "mock_crc_id_777666"

        url = f"{self.base_url}/api/v1/search"
        headers = {"Authorization": f"Bearer {session_code}"}
        payload = {"bvn": bvn}
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(url, json=payload, headers=headers)
                if response.status_code == 200:
                    data = response.json()
                    return data.get("crc_id") or "mock_crc_id_from_api"
                else:
                    logger.error(f"CRC Find customer failed: {response.status_code}")
                    return ""
        except Exception as e:
            logger.error(f"CRC Find customer error: {e}")
            return ""

    async def get_report(self, loan_application_id: str, registry_id: str, session_code: str) -> dict:
        """Retrieves credit report from CRC and persists to DB."""
        if not settings.CRC_API_KEY or session_code.startswith("mock_"):
            raw_report = {
                "status": "success",
                "registry_id": registry_id,
                "report_type": "CRC_Credit_Report_Summary",
                "data": {
                    "is_approximate_placeholder": True,
                    "score": 710,
                    "active_loans_count": 1,
                    "total_outstanding_balance": 800000.0,
                    "total_monthly_repayments": 50000.0,
                    "total_delinquent_accounts": 0,
                    "worst_payment_status": "performing"
                }
            }
            await self.service._save_submission(loan_application_id, registry_id, "success", "CRC_Credit_Report_Summary", raw_report, self.name)
            return raw_report

        url = f"{self.base_url}/api/v1/report"
        headers = {"Authorization": f"Bearer {session_code}"}
        payload = {"crc_id": registry_id}
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                response = await client.post(url, json=payload, headers=headers)
                if response.status_code == 200:
                    raw_report = response.json()
                    await self.service._save_submission(loan_application_id, registry_id, "success", "CRC_Credit_Report_Summary", raw_report, self.name)
                    return raw_report
                else:
                    raw_err = {"status_code": response.status_code, "body": response.text}
                    await self.service._save_submission(loan_application_id, registry_id, "failed", "CRC_Credit_Report_Summary", raw_err, self.name)
                    return raw_err
        except Exception as e:
            logger.error(f"Get CRC credit report error: {e}")
            raw_err = {"error": str(e)}
            await self.service._save_submission(loan_application_id, registry_id, "error", "CRC_Credit_Report_Summary", raw_err, self.name)
            return raw_err

    async def submit_account(self, loan_application_id: str, session_code: str, loan_data: dict) -> dict:
        """Pushes disbursement details outward to CRC."""
        if not settings.CRC_API_KEY or session_code.startswith("mock_"):
            raw_response = {
                "success": True,
                "message": "Disbursed loan reported to CRC successfully (mock mode).",
                "reported_at": "now"
            }
            await self.service._save_submission(loan_application_id, "mock_crc_id_777666", "disbursed_reported", "crc_add_update_account", raw_response, self.name)
            return raw_response

        url = f"{self.base_url}/api/v1/account/submit"
        headers = {"Authorization": f"Bearer {session_code}"}

        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                response = await client.post(url, json=loan_data, headers=headers)
                raw_response = response.json() if response.status_code == 200 else {"body": response.text}
                status_str = "success" if response.status_code == 200 else "failed"
                await self.service._save_submission(
                    loan_application_id,
                    loan_data.get("person", {}).get("RegistryID"),
                    status_str,
                    "crc_add_update_account",
                    raw_response,
                    self.name
                )
                return raw_response
        except Exception as e:
            logger.error(f"Submit account to CRC error: {e}")
            raw_err = {"error": str(e)}
            await self.service._save_submission(loan_application_id, None, "error", "crc_add_update_account", raw_err, self.name)
            return raw_err


class CreditBureauService:
    def __init__(self, conn=None):
        self.conn = conn
        # Determine the active provider client
        if settings.CRC_API_KEY:
            self.provider = CrcProvider(conn, self)
        else:
            self.provider = CreditRegistryProvider(conn, self)

    @property
    def provider_name(self) -> str:
        return self.provider.name

    async def get_session_code(self) -> str:
        return await self.provider.get_session_code()

    async def find_customer(self, session_code: str, bvn: str, phone: str = None, name: str = None) -> str:
        return await self.provider.find_customer(session_code, bvn, phone, name)

    async def get_report(self, loan_application_id: str, registry_id: str, session_code: str) -> dict:
        return await self.provider.get_report(loan_application_id, registry_id, session_code)

    async def submit_account(self, loan_application_id: str, session_code: str, loan_data: dict) -> dict:
        return await self.provider.submit_account(loan_application_id, session_code, loan_data)

    async def _save_submission(self, loan_application_id, registry_id, status, report_type, raw_response, provider_name):
        if not loan_application_id or not self.conn:
            return
        await self.conn.execute(
            """
            INSERT INTO bureau_submissions (loan_application_id, registry_id, status, report_type, raw_response, provider, submitted_at)
            VALUES ($1, $2, $3, $4, $5, $6, NOW());
            """,
            loan_application_id,
            registry_id,
            status,
            report_type,
            json.dumps(raw_response),
            provider_name
        )
        # Write workflow event
        await self.conn.execute(
            """
            INSERT INTO workflow_events (loan_id, org_id, event_type, from_stage, to_stage, triggered_by, triggered_role, notes, created_at)
            SELECT id, org_id, 'bureau.submitted', stage, stage, created_by, 'system', $2, NOW()
            FROM loan_applications WHERE id = $1;
            """,
            loan_application_id,
            f"Credit bureau provider '{provider_name}' action '{report_type}' completed with status: {status}."
        )
