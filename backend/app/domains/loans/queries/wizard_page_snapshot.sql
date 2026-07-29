-- Application, intake data, and signing state for one wizard page.
-- Params: $1=loan_id, $2=org_id

SELECT
    la.*,
    officer.full_name AS created_by_name,
    officer.role AS created_by_role,
    owner.full_name AS current_owner_name,
    credit.full_name AS credit_officer_name,
    manager.full_name AS branch_manager_name,
    COALESCE(stage_data.data_json, '{}'::jsonb) AS wizard_data,
    CASE
        WHEN version.id IS NULL THEN NULL
        ELSE to_jsonb(version)
    END AS latest_version,
    COALESCE(signatures.events, '[]'::jsonb) AS signature_events
FROM loan_applications la
LEFT JOIN users officer ON officer.id = la.created_by
LEFT JOIN users owner ON owner.id = la.current_owner_id
LEFT JOIN users credit ON credit.id = la.credit_officer_id
LEFT JOIN users manager ON manager.id = la.branch_manager_id
LEFT JOIN LATERAL (
    SELECT sd.data_json
    FROM stage_data sd
    WHERE sd.loan_id = la.id
      AND sd.stage = 'intake'
    ORDER BY sd.saved_at DESC, sd.id DESC
    LIMIT 1
) stage_data ON TRUE
LEFT JOIN LATERAL (
    SELECT dv.*
    FROM document_versions dv
    WHERE dv.application_id = la.id
      AND dv.subject_type = 'applicant_stage'
      AND dv.subject_id = 'intake'
      AND dv.status IN ('draft', 'sent', 'signed')
    ORDER BY dv.version_number DESC, dv.id DESC
    LIMIT 1
) version ON TRUE
LEFT JOIN LATERAL (
    SELECT jsonb_agg(to_jsonb(se) ORDER BY se.signed_at, se.id) AS events
    FROM signature_events se
    WHERE se.document_version_id = version.id
) signatures ON version.id IS NOT NULL
WHERE la.id = $1
  AND la.org_id = $2
  AND la.deleted_at IS NULL;
