import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from uuid import uuid4
from starlette.requests import Request
from fastapi import HTTPException
from app.domains.loans.router import render_wizard_step, process_wizard_step

def make_request(method="GET", path="/"):
    request = Request(
        {
            "type": "http",
            "method": method,
            "path": path,
            "raw_path": path.encode("utf-8"),
            "query_string": b"",
            "headers": [],
            "scheme": "https",
            "server": ("testserver", 443),
            "client": ("127.0.0.1", 1234),
            "root_path": "",
        }
    )
    request.state.csp_nonce = "test-nonce"
    return request

@pytest.mark.asyncio
async def test_step4_readonly_is_false_for_credit_analyst():
    req = make_request()
    app_uuid = uuid4()
    
    org_id = uuid4()
    current_user = MagicMock()
    current_user.role = "Credit Analyst"
    current_user.id = uuid4()
    current_user.org_id = org_id
    current_user.is_active = True
    
    app_record = MagicMock()
    app_record.org_id = org_id
    app_record.stage = "credit_analyst_review"
    app_record.loan_type = "elms2"
    app_record.amount = 500000
    app_record.tenor_months = 12
    app_record.applicant_name = "Test Client"
    
    conn = MagicMock()
    conn.fetchrow = AsyncMock()
    conn.fetch = AsyncMock()
    conn.execute = AsyncMock()
    tx_mock = MagicMock()
    tx_mock.__aenter__ = AsyncMock()
    tx_mock.__aexit__ = AsyncMock()
    conn.transaction.return_value = tx_mock
    conn.fetchrow.return_value = {
        "revenue": 100000,
        "expenses": 40000,
        "period_label": "test",
        "min_amount": 10000,
        "max_amount": 1000000,
        "min_tenor_months": 3,
        "max_tenor_months": 24,
        "name": "elms2"
    }
    conn.fetch.return_value = []
    
    snapshot = (app_record, {}, None, [])
    
    with patch("app.domains.loans.router.LoanRepository") as mock_repo_cls, \
         patch("app.domains.loans.router.FeasibilityRepository") as mock_feas_cls, \
         patch("app.domains.loans.router.templates") as mock_templates:
         
        mock_repo = MagicMock()
        mock_repo.get_wizard_page_snapshot = AsyncMock(return_value=snapshot)
        mock_repo_cls.return_value = mock_repo
        
        mock_feas = MagicMock()
        mock_feas.get_inputs = AsyncMock(return_value=([], {}, []))
        mock_feas_cls.return_value = mock_feas
        
        await render_wizard_step(
            request=req,
            application_id=str(app_uuid),
            step=4,
            conn=conn,
            current_user=current_user
        )
        
        args, kwargs = mock_templates.TemplateResponse.call_args
        context = args[2] if len(args) > 2 else kwargs["context"]
        
        assert context["readonly"] is False

@pytest.mark.asyncio
async def test_step4_process_allowed_for_credit_analyst():
    req = make_request(method="POST")
    
    form_data = MagicMock()
    form_data.multi_items = AsyncMock(return_value=[])
    
    async def get_form():
        return form_data
    
    req.form = get_form
    
    app_uuid = uuid4()
    org_id = uuid4()
    current_user = MagicMock()
    current_user.role = "Credit Analyst"
    current_user.id = uuid4()
    current_user.org_id = org_id
    current_user.is_active = True
    
    app_record = MagicMock()
    app_record.org_id = org_id
    app_record.stage = "credit_analyst_review"
    app_record.amount = 500000
    app_record.tenor_months = 12
    
    conn = MagicMock()
    conn.fetchrow = AsyncMock()
    conn.fetch = AsyncMock()
    conn.execute = AsyncMock()
    tx_mock = MagicMock()
    tx_mock.__aenter__ = AsyncMock()
    tx_mock.__aexit__ = AsyncMock()
    conn.transaction.return_value = tx_mock
    
    with patch("app.domains.loans.router.LoanRepository") as mock_repo_cls, \
         patch("app.domains.loans.router.require_intake_edit") as mock_require, \
         patch("app.domains.loans.router.LoanService") as mock_svc_cls:
         
        mock_repo = MagicMock()
        mock_repo.get_by_id = AsyncMock(return_value=app_record)
        mock_repo_cls.return_value = mock_repo
        
        mock_svc = MagicMock()
        mock_svc.save_wizard_step = AsyncMock()
        mock_svc_cls.return_value = mock_svc
        
        await process_wizard_step(
            request=req,
            application_id=str(app_uuid),
            step=4,
            service=mock_svc,
            conn=conn,
            current_user=current_user
        )
        
        mock_require.assert_not_called()
