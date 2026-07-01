-- Extra test data for tables not covered by the main demo seed.
-- Safe to run after 001-004 on a demo database.

INSERT INTO pledged_items (
    loan_id,
    item_number,
    item_name,
    serial_number,
    description,
    estimated_value,
    created_at
)
SELECT
    la.id,
    item_no,
    CASE item_no
        WHEN 1 THEN 'Shop Stock'
        WHEN 2 THEN 'Display Refrigerator'
        ELSE 'Point of Sale Terminal'
    END,
    la.ref_no || '-PLG-' || item_no,
    CASE item_no
        WHEN 1 THEN 'Inventory and trade goods pledged for facility security'
        WHEN 2 THEN 'Commercial refrigerator used by the business'
        ELSE 'POS device and daily business receivables'
    END,
    CASE item_no
        WHEN 1 THEN GREATEST(COALESCE(la.amount, 0) * 0.70, 100000)
        WHEN 2 THEN GREATEST(COALESCE(la.amount, 0) * 0.25, 50000)
        ELSE 75000
    END,
    la.created_at + INTERVAL '90 minutes'
FROM loan_applications la
CROSS JOIN (VALUES (1), (2), (3)) AS items(item_no)
WHERE la.org_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
  AND la.stage IN ('credit_review', 'branch_approval', 'disbursement_ready', 'disbursed', 'returned')
  AND NOT EXISTS (
      SELECT 1
      FROM pledged_items pi
      WHERE pi.loan_id = la.id
  );

INSERT INTO notifications (
    id,
    user_id,
    org_id,
    application_id,
    title,
    message,
    type,
    is_read,
    created_at
)
VALUES
(
    'notif_demo_returned_001',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'c0000000-0000-4000-8000-000000000019',
    'Application Returned',
    'MMFB-2026-01019 was returned by Credit Officer for correction',
    'application_returned',
    FALSE,
    NOW() - INTERVAL '10 minutes'
),
(
    'notif_demo_signoff_001',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'c0000000-0000-4000-8000-000000000012',
    'Visitation Signed Off',
    'Branch Manager concurred on site visit for Halima Yusuf',
    'visitation_signoff',
    FALSE,
    NOW() - INTERVAL '25 minutes'
),
(
    'notif_demo_credit_001',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'c0000000-0000-4000-8000-000000000012',
    'Credit Review Done',
    'Credit review completed for Halima Yusuf (MMFB-2026-01012)',
    'credit_review',
    TRUE,
    NOW() - INTERVAL '1 hour'
),
(
    'notif_demo_approved_001',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'c0000000-0000-4000-8000-000000000015',
    'Loan Approved',
    'MMFB-2026-01015 was approved for disbursement',
    'approved',
    FALSE,
    NOW() - INTERVAL '2 hours'
),
(
    'notif_demo_docs_001',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'c0000000-0000-4000-8000-000000000008',
    'Documents Verified',
    'Documents verified for Maryam Sani (MMFB-2026-01008)',
    'document_verified',
    TRUE,
    NOW() - INTERVAL '3 hours'
),
(
    'notif_demo_system_001',
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a00',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    NULL,
    'System Ready',
    'Demo notification data has been loaded for mobile testing',
    'system',
    FALSE,
    NOW() - INTERVAL '4 hours'
)
ON CONFLICT (id) DO NOTHING;
