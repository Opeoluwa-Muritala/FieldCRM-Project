from decimal import Decimal
from uuid import uuid4

import pytest
from starlette.datastructures import FormData

from app.domains.feasibility.cam import CAMValidationError, parse_cam_form


def valid_form(*extra):
    values = [
        ("cash_at_bank", "100000"), ("stock", "200000"),
        ("prepayment", "50000"), ("fixed_assets", "150000"),
        ("monthly_turnover", "1000000"), ("margin", "15"),
        ("monthly_expenses", "50000"), ("recommended_amount", "200000"),
        ("interest_rate", "3.3"), ("proposed_tenor", "12"),
        ("analyst_name", "Ada Analyst"),
        ("analyst_recommendation", "Recommend subject to stated conditions."),
    ]
    values.extend(extra)
    return FormData(values)


def test_cam_form_maps_exact_external_history_and_turnover_fields():
    form = valid_form(
        ("external_institution[]", "Sterling Bank Plc"),
        ("external_start_date[]", "2025-01-01"),
        ("external_end_date[]", "2025-12-31"),
        ("external_amount[]", "500000"),
        ("external_rental[]", "25000"),
        ("external_tenure[]", "12"),
        ("external_outstanding[]", "200000"),
        ("external_classification[]", "performing"),
        ("external_status[]", "current"),
        ("turnover_month[]", "2026-07"),
        ("turnover_bank[]", "First Bank"),
        ("turnover_amount[]", "750000"),
        ("turnover_transaction_count[]", "18"),
    )

    payload = parse_cam_form(form, [])

    external = payload["external_obligations"][0]
    assert external["facility_amount"] == Decimal("500000")
    assert external["periodic_payment"] == Decimal("25000")
    assert external["outstanding_balance"] == Decimal("200000")
    assert payload["bank_turnovers"][0]["entry_date"].isoformat() == "2026-07-01"
    assert payload["profile"]["margin"] == Decimal("0.15")


@pytest.mark.parametrize(
    "field,value,message",
    [
        ("recommended_amount", "NaN", "Recommended Amount"),
        ("margin", "101", "Margin"),
        ("property_coordinates_link", "javascript:alert(1)", "Google Maps"),
    ],
)
def test_cam_form_rejects_malformed_financial_and_url_inputs(field, value, message):
    with pytest.raises(CAMValidationError, match=message):
        parse_cam_form(valid_form((field, value)), [])


def test_cam_form_accepts_only_server_supplied_collateral_ids():
    item_id = uuid4()
    payload = parse_cam_form(
        valid_form(
            (f"collateral_owner_{item_id}", "client"),
            (f"collateral_fsv_{item_id}", "175000"),
            ("collateral_owner_00000000-0000-0000-0000-000000000000", "guarantor"),
        ),
        [item_id],
    )

    assert payload["collateral_items"] == [{
        "id": str(item_id), "owner_type": "client", "chassis_no": "",
        "registration_no": "", "colour": "", "year": None,
        "forced_sale_value": Decimal("175000"),
    }]


def test_cam_form_accepts_only_allowlisted_bounded_metric_notes():
    payload = parse_cam_form(
        valid_form(
            ("metric_note_dti", "High ratio reflects the proposed short tenor."),
            ("metric_note_untrusted", "Must not be persisted"),
        ),
        [],
    )

    assert payload["profile"]["analyst_metric_notes"] == {
        "dti": "High ratio reflects the proposed short tenor."
    }

    with pytest.raises(CAMValidationError, match="Metric Note Dti is too long"):
        parse_cam_form(valid_form(("metric_note_dti", "x" * 501)), [])
