import pytest

from app.domains.loans.repository import LoanRepository


class SnapshotConnection:
    async def fetchrow(self, _query, _loan_id, _org_id):
        return {
            "wizard_data": '{"residential_address":"12 Broad Street"}',
            "visitation_data": '{"status":"completed"}',
            "verification_check": None,
            "bureau_submission": '{"status":"clear"}',
            "aml_check": '{"status":"clear"}',
            "checklist_map": '{"crm:reviewed":true}',
        }


@pytest.mark.asyncio
async def test_application_detail_snapshot_decodes_jsonb_strings():
    snapshot = await LoanRepository(SnapshotConnection()).get_application_detail_snapshot(
        "loan-id", "org-id"
    )

    assert snapshot["wizard_data"]["residential_address"] == "12 Broad Street"
    assert snapshot["visitation_data"] == {"status": "completed"}
    assert snapshot["bureau_submission"] == {"status": "clear"}
    assert snapshot["aml_check"] == {"status": "clear"}
    assert snapshot["checklist_map"] == {"crm:reviewed": True}


@pytest.mark.asyncio
async def test_application_detail_snapshot_tolerates_invalid_json():
    conn = SnapshotConnection()

    async def invalid_row(*_args):
        return {"wizard_data": "not-json", "visitation_data": "not-json"}

    conn.fetchrow = invalid_row
    snapshot = await LoanRepository(conn).get_application_detail_snapshot("loan-id", "org-id")

    assert snapshot["wizard_data"] == {}
    assert snapshot["visitation_data"] is None
