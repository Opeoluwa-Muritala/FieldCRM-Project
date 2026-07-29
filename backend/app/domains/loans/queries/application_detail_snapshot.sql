-- One-application detail metadata that previously required six round trips.
-- Tenant scope is established once and reused by every correlated subquery.
-- Params: $1=loan_id, $2=org_id

WITH loan_scope AS (
    SELECT id, org_id
    FROM loan_applications
    WHERE id = $1
      AND org_id = $2
      AND deleted_at IS NULL
)
SELECT
    COALESCE(
        (
            SELECT sd.data_json
            FROM stage_data sd
            JOIN loan_scope ls ON ls.id = sd.loan_id
            WHERE sd.stage = 'intake'
            ORDER BY sd.saved_at DESC, sd.id DESC
            LIMIT 1
        ),
        '{}'::jsonb
    ) AS wizard_data,
    (
        SELECT to_jsonb(visit)
        FROM (
            SELECT
                vr.id,
                vr.loan_id,
                vr.org_id,
                vr.visit_date,
                vr.visit_time,
                vr.met_with,
                vr.relationship,
                vr.premises_description,
                vr.direction_from_branch,
                vr.business_condition,
                vr.visiting_officer_id,
                vr.visiting_officer_signature,
                vr.visiting_officer_name,
                vr.visiting_officer_signature_data AS visiting_officer_sig,
                vr.account_officer_id,
                vr.account_officer_name AS account_officer,
                vr.account_officer_signature_data AS account_officer_sig,
                vr.gps_coordinates,
                vr.site_photo_url,
                vr.manager_concurrence,
                vr.manager_id,
                vr.manager_notes,
                vr.manager_signature_data AS bm_sig,
                vr.concurrence_return_reason,
                vr.manager_concurred_at,
                vr.status,
                vr.created_at,
                vr.updated_at
            FROM visitation_reports vr
            JOIN loan_scope ls
              ON ls.id = vr.loan_id
             AND ls.org_id = vr.org_id
            LIMIT 1
        ) visit
    ) AS visitation_data,
    (
        SELECT to_jsonb(verification)
        FROM (
            SELECT vc.status, vc.is_valid, vc.checked_at
            FROM verification_checks vc
            JOIN loan_scope ls ON ls.id = vc.loan_application_id
            ORDER BY vc.checked_at DESC, vc.id DESC
            LIMIT 1
        ) verification
    ) AS verification_check,
    (
        SELECT to_jsonb(bureau)
        FROM (
            SELECT bs.status, bs.registry_id, bs.provider, bs.submitted_at
            FROM bureau_submissions bs
            JOIN loan_scope ls ON ls.id = bs.loan_application_id
            ORDER BY bs.submitted_at DESC, bs.id DESC
            LIMIT 1
        ) bureau
    ) AS bureau_submission,
    (
        SELECT to_jsonb(aml)
        FROM (
            SELECT sc.status, sc.category_count, sc.checked_at
            FROM sanctions_checks sc
            JOIN loan_scope ls ON ls.id = sc.loan_application_id
            ORDER BY sc.checked_at DESC, sc.id DESC
            LIMIT 1
        ) aml
    ) AS aml_check,
    COALESCE(
        (
            SELECT jsonb_object_agg(
                ci.context || ':' || ci.item_key,
                ci.is_checked
                ORDER BY ci.context, ci.item_key
            )
            FROM checklist_items ci
            JOIN loan_scope ls ON ls.id = ci.loan_application_id
        ),
        '{}'::jsonb
    ) AS checklist_map
FROM loan_scope;
