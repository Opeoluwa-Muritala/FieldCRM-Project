import asyncio
import inspect
from types import SimpleNamespace
from uuid import uuid4

from app.domains.loans import router


class Request:
    async def form(self):
        return {
            "amount": "300000",
            "consent_credit_bureau": "1",
            "consent_credit_check": "1",
            "consent_cheque": "1",
            "consent_gsi": "1",
            "final_declaration": "1",
        }


class Connection:
    def __init__(self):
        self.executions = []

    async def execute(self, query, *args):
        self.executions.append((query, args))


def test_crm_form_action_defaults_to_advance_instead_of_422():
    default = inspect.signature(router.process_crm_review).parameters["action"].default
    assert default.default == "advance"


def test_crm_advances_without_customer_signature(monkeypatch):
    application_id = uuid4()
    org_id = uuid4()
    observed = {}

    class Repository:
        def __init__(self, conn):
            pass

        async def get_by_id(self, requested_id, requested_org):
            return SimpleNamespace(stage="crm_review")

        async def get_stage_data(self, requested_id, stage):
            return None

        async def save_stage_data(self, requested_id, stage, data, user_id):
            observed["consents"] = data

        async def advance_stage(self, requested_id, requested_org, next_stage):
            observed["next_stage"] = next_stage
            return SimpleNamespace(stage=next_stage)

    class Audit:
        def __init__(self, conn):
            pass

        async def log(self, **kwargs):
            observed["audit"] = kwargs

    monkeypatch.setattr(router, "LoanRepository", Repository)
    monkeypatch.setattr(router, "AuditService", Audit)
    user = SimpleNamespace(id=uuid4(), org_id=org_id, role="crm")
    conn = Connection()

    response = asyncio.run(
        router.process_crm_review(
            request=Request(),
            application_id=str(application_id),
            action="advance",
            crm_notes="Complete dossier",
            conn=conn,
            current_user=user,
        )
    )

    assert response.status_code == 303
    assert observed["next_stage"] == "head_crm_review"
    assert observed["consents"]["final_declaration"] == "true"
    assert "applicant_signature" not in observed["consents"]
    assert "AND org_id = $3" in conn.executions[0][0]
