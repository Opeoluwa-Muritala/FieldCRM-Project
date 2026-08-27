"""Validated form contract for the CAM-grounded feasibility screen."""
from __future__ import annotations

from datetime import date
from decimal import Decimal, InvalidOperation
import re
from urllib.parse import urlparse
from uuid import UUID


MAX_ROWS = 50
MAX_MONEY = Decimal("9999999999999.99")
HISTORY_STATUSES = {"current", "closed", "past_due", "restructured", "disputed"}
HISTORY_CLASSIFICATIONS = {"performing", "watchlist", "substandard", "doubtful", "lost", "closed"}
COLLATERAL_OWNERS = {"client", "guarantor", "client_asset"}
GOOGLE_MAP_HOSTS = {"google.com", "www.google.com", "maps.google.com", "maps.app.goo.gl", "goo.gl"}
CAM_METRIC_NOTE_KEYS = {
    "total_assets", "gross_profit", "net_profit", "installment",
    "external_rental", "total_rental", "dti", "outstanding",
    "gearing", "collateral_total", "collateral_coverage", "asset_to_loan",
}


class CAMValidationError(ValueError):
    pass


def _value(form, name: str) -> str:
    value = form.get(name, "")
    return str(value).strip() if value is not None else ""


def _text(form, name: str, max_length: int, *, required: bool = False) -> str:
    value = _value(form, name)
    if required and not value:
        raise CAMValidationError(f"{name.replace('_', ' ').title()} is required")
    if len(value) > max_length:
        raise CAMValidationError(f"{name.replace('_', ' ').title()} is too long")
    return value


def _decimal(
    value: object,
    label: str,
    *,
    minimum: Decimal = Decimal("0"),
    maximum: Decimal = MAX_MONEY,
) -> Decimal:
    try:
        parsed = Decimal(str(value or "0"))
    except (InvalidOperation, ValueError):
        raise CAMValidationError(f"{label} must be a valid number") from None
    if not parsed.is_finite() or parsed < minimum or parsed > maximum:
        raise CAMValidationError(f"{label} must be between {minimum} and {maximum}")
    return parsed


def _integer(value: object, label: str, minimum: int, maximum: int) -> int:
    parsed = _decimal(value, label, minimum=Decimal(minimum), maximum=Decimal(maximum))
    if parsed != parsed.to_integral_value():
        raise CAMValidationError(f"{label} must be a whole number")
    return int(parsed)


def _optional_date(value: object, label: str) -> date | None:
    text = str(value or "").strip()
    if not text:
        return None
    try:
        return date.fromisoformat(text)
    except ValueError:
        raise CAMValidationError(f"{label} must be a valid date") from None


def _at(values: list, index: int, default: str = "") -> str:
    return str(values[index]).strip() if index < len(values) and values[index] is not None else default


def _history_rows(form, prefix: str) -> list[dict]:
    institutions = form.getlist(f"{prefix}_institution[]")
    if len(institutions) > MAX_ROWS:
        raise CAMValidationError("Credit history is limited to 50 rows")
    fields = {
        key: form.getlist(f"{prefix}_{key}[]")
        for key in (
            "start_date", "end_date", "amount", "rental", "tenure",
            "outstanding", "classification", "status",
        )
    }
    rows = []
    for index, raw_institution in enumerate(institutions):
        institution = str(raw_institution or "").strip()
        if not institution:
            continue
        if len(institution) > 160:
            raise CAMValidationError("Institution name is too long")
        start_date = _optional_date(_at(fields["start_date"], index), "Start date")
        end_date = _optional_date(_at(fields["end_date"], index), "End date")
        if start_date and end_date and end_date < start_date:
            raise CAMValidationError("Credit history end date cannot precede start date")
        status = _at(fields["status"], index, "current") or "current"
        classification = _at(fields["classification"], index, "performing") or "performing"
        if status not in HISTORY_STATUSES or classification not in HISTORY_CLASSIFICATIONS:
            raise CAMValidationError("Invalid credit history status or classification")
        rows.append({
            "lender_name": institution,
            "start_date": start_date,
            "end_date": end_date,
            "facility_amount": _decimal(_at(fields["amount"], index), "Facility amount"),
            "periodic_payment": _decimal(_at(fields["rental"], index), "Rental"),
            "payment_frequency": "monthly",
            "remaining_tenor_months": _integer(_at(fields["tenure"], index, "0"), "Tenure", 0, 1200) or None,
            "outstanding_balance": _decimal(_at(fields["outstanding"], index), "Outstanding loan"),
            "classification": classification,
            "status": status,
        })
    return rows


def _turnover_rows(form) -> list[dict]:
    banks = form.getlist("turnover_bank[]")
    if len(banks) > MAX_ROWS:
        raise CAMValidationError("Bank turnover is limited to 50 rows")
    months = form.getlist("turnover_month[]")
    amounts = form.getlist("turnover_amount[]")
    counts = form.getlist("turnover_transaction_count[]")
    rows = []
    for index, raw_bank in enumerate(banks):
        bank = str(raw_bank or "").strip()
        if not bank:
            continue
        if len(bank) > 120:
            raise CAMValidationError("Bank name is too long")
        month_text = _at(months, index)
        if not re.fullmatch(r"\d{4}-\d{2}", month_text):
            raise CAMValidationError("Each bank turnover row requires a valid month")
        entry_date = _optional_date(f"{month_text}-01", "Turnover month")
        rows.append({
            "channel": bank,
            "amount": _decimal(_at(amounts, index), "Bank inflow"),
            "transaction_count": _integer(_at(counts, index, "0"), "Transaction count", 0, 1_000_000),
            "entry_date": entry_date,
            "description": month_text,
        })
    return rows


def _maps_url(form) -> str:
    value = _text(form, "property_coordinates_link", 500)
    if not value:
        return ""
    parsed = urlparse(value)
    if parsed.scheme != "https" or (parsed.hostname or "").lower() not in GOOGLE_MAP_HOSTS:
        raise CAMValidationError("Property coordinates must be an HTTPS Google Maps link")
    return value


def parse_cam_form(form, collateral_ids: list[UUID]) -> dict:
    margin_percent = _decimal(form.get("margin", "0"), "Margin", maximum=Decimal("100"))
    profile = {
        "cash_at_bank": _decimal(form.get("cash_at_bank"), "Cash at Bank"),
        "stock": _decimal(form.get("stock"), "Stock"),
        "prepayment": _decimal(form.get("prepayment"), "Prepayment"),
        "fixed_assets": _decimal(form.get("fixed_assets"), "Fixed Asset"),
        "monthly_turnover": _decimal(form.get("monthly_turnover"), "Monthly Turnover"),
        "margin": margin_percent / Decimal("100"),
        "monthly_expenses": _decimal(form.get("monthly_expenses"), "Monthly Expenses"),
        "recommended_amount": _decimal(form.get("recommended_amount"), "Recommended Amount", minimum=Decimal("0.01")),
        "interest_rate": _decimal(form.get("interest_rate"), "Interest rate", maximum=Decimal("100")),
        "proposed_tenor": _integer(form.get("proposed_tenor"), "Proposed tenor", 1, 120),
        "remita_email": _text(form, "remita_email", 254),
        "remita_account_no": _text(form, "remita_account_no", 30),
        "remita_account_name": _text(form, "remita_account_name", 160),
        "remita_bank": _text(form, "remita_bank", 120),
        "property_coordinates_link": _maps_url(form),
        "property_description": _text(form, "property_description", 2000),
        "analyst_name": _text(form, "analyst_name", 160, required=True),
        "analyst_recommendation": _text(form, "analyst_recommendation", 5000, required=True),
        "pre_disbursement_conditions": _text(form, "pre_disbursement_conditions", 5000),
        "shop_allocation": _text(form, "shop_allocation", 160),
        "shop_allowance": _decimal(form.get("shop_allowance"), "Shop allowance"),
        "shop_allowance_verified": _decimal(form.get("shop_allowance_verified"), "Verified shop allowance"),
        "analyst_metric_notes": {
            key: note
            for key in CAM_METRIC_NOTE_KEYS
            if (note := _text(form, f"metric_note_{key}", 500))
        },
    }
    if profile["remita_email"] and not re.fullmatch(r"[^@\s]+@[^@\s]+\.[^@\s]+", profile["remita_email"]):
        raise CAMValidationError("Remita email is invalid")

    guarantors = []
    for slot in (1, 2):
        prefix = f"g{slot}_"
        name = _text(form, f"{prefix}full_name", 160)
        if not name:
            continue
        bvn = _text(form, f"{prefix}bvn", 11)
        phone = _text(form, f"{prefix}phone", 20)
        if bvn and not re.fullmatch(r"\d{11}", bvn):
            raise CAMValidationError(f"Guarantor {slot} BVN must contain 11 digits")
        if phone and not re.fullmatch(r"[+0-9 ()-]{7,20}", phone):
            raise CAMValidationError(f"Guarantor {slot} phone number is invalid")
        guarantors.append({
            "slot": slot,
            "full_name": name,
            "phone": phone,
            "bvn": bvn,
            "relationship_to_client": "",
            "business_name": _text(form, f"{prefix}business_name", 160),
            "business_address": _text(form, f"{prefix}business_address", 500),
            "description_landmark": _text(form, f"{prefix}landmark", 500),
        })

    collateral = []
    for item_id in collateral_ids[:MAX_ROWS]:
        suffix = str(item_id)
        owner = _value(form, f"collateral_owner_{suffix}")
        if owner and owner not in COLLATERAL_OWNERS:
            raise CAMValidationError("Invalid collateral owner")
        collateral.append({
            "id": suffix,
            "owner_type": owner,
            "chassis_no": _text(form, f"collateral_chassis_{suffix}", 120),
            "registration_no": _text(form, f"collateral_reg_{suffix}", 120),
            "colour": _text(form, f"collateral_colour_{suffix}", 80),
            "year": _integer(form.get(f"collateral_year_{suffix}"), "Collateral year", 1900, 2200) if form.get(f"collateral_year_{suffix}") else None,
            "forced_sale_value": _decimal(form.get(f"collateral_fsv_{suffix}"), "Forced Sale Value"),
        })

    return {
        "internal_obligations": _history_rows(form, "internal"),
        "external_obligations": _history_rows(form, "external"),
        "bank_turnovers": _turnover_rows(form),
        "profile": profile,
        "guarantors": guarantors,
        "collateral_items": collateral,
    }
