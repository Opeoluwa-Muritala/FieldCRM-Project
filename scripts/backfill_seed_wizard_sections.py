"""Complete wizard sections 1-8 for existing seeded applications."""

import asyncio
import os

import asyncpg
from dotenv import load_dotenv


load_dotenv("backend/.env")


async def main() -> None:
    database_url = os.getenv("DATABASE_URL")
    if not database_url:
        raise SystemExit("DATABASE_URL is not configured in backend/.env")

    conn = await asyncpg.connect(database_url)
    try:
        result = await conn.execute(
            """
            UPDATE stage_data sd
            SET data_json = sd.data_json || jsonb_build_object(
                'photo_url', '/static/uploads/demo/generated/' || la.ref_no || '_passport_photo.pdf',
                'education', 'Bachelor''s degree',
                'amount_words', 'Amount equal to requested facility',
                'loan_purpose_other', CASE WHEN la.loan_type = 'other' THEN la.purpose ELSE '' END,
                'sort_code', '090171',
                'pnl_period_label', COALESCE(bp.period_label, 'Average month'),
                'pnl_revenue', COALESCE(bp.revenue, 0)::text,
                'pnl_expenses', COALESCE(bp.expenses, 0)::text,
                'business_location_address', jsonb_build_array(
                    COALESCE(sd.data_json->>'business_address', 'Primary business location'),
                    'Warehouse for ' || la.ref_no || ', Industrial Estate, Lagos'
                ),
                'business_location_city', jsonb_build_array('Lagos', 'Ikeja'),
                'business_location_state', jsonb_build_array('Lagos', 'Lagos'),
                'business_location_function', jsonb_build_array('retail_outlet', 'warehouse'),
                'collateral_type', jsonb_build_array('inventory', 'cash'),
                'collateral_narration', jsonb_build_array('Trading stock', 'Cash collateral'),
                'collateral_market_value', jsonb_build_array((la.amount * 0.75)::text, (la.amount * 0.20)::text),
                'collateral_fsv', jsonb_build_array((la.amount * 0.525)::text, (la.amount * 0.20)::text),
                'collateral_security', jsonb_build_array('Stock lien', 'Cash lien'),
                'pledge_borrower', la.applicant_name,
                'pledge_obligor', la.applicant_name,
                'pledge_location', COALESCE(sd.data_json->>'business_address', 'Primary business location'),
                'pledge_date', CURRENT_DATE::text,
                'pledge_item_name', jsonb_build_array('Shop Stock', 'Display Refrigerator', 'POS Terminal'),
                'pledge_item_desc', jsonb_build_array('Trading inventory', 'Cold-chain equipment', 'Payment terminal'),
                'pledge_item_qty', jsonb_build_array('1 lot', '1', '1'),
                'pledge_item_val', jsonb_build_array(
                    (la.amount * 0.45)::text, (la.amount * 0.20)::text, (la.amount * 0.10)::text
                ),
                'completed_steps', jsonb_build_array(1,2,3,4,5,6,7,8),
                'section_1_completed', true,
                'section_2_completed', true,
                'section_3_completed', true,
                'section_4_completed', true,
                'section_5_completed', true,
                'section_6_completed', true,
                'section_7_completed', true,
                'section_8_completed', true
            ),
            saved_at = NOW()
            FROM loan_applications la
            LEFT JOIN business_pnl bp ON bp.application_id = la.id
            WHERE sd.loan_id = la.id AND sd.stage = 'intake'
            """
        )
        completed = await conn.fetchval(
            """SELECT count(*) FROM stage_data
               WHERE stage='intake'
                 AND data_json @> '{"completed_steps":[1,2,3,4,5,6,7,8]}'::jsonb"""
        )
        print(f"{result}; completed_sections={completed}")
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
