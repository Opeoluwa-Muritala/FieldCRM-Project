import sys
import asyncio
sys.path.insert(0, r"C:\Users\LENOVO\Desktop\FieldCRM\backend")

print("Importing edge case tests...")
from tests.test_signing_evidence_edge_cases import (
    test_part1_draft_edits_succeed_and_log,
    test_part1_frozen_edits_rejected_with_409,
    test_part1_concurrent_edits_prevention,
    test_part1_freeze_idempotency,
    test_part1_correction_supersedes_and_invalidates_only_affected,
    test_part1_correction_after_signed_allowed,
    test_part1_payload_hash_deterministic,
    test_part1_payload_hash_whitespace_insignificant,
    test_part1_version_number_sequencing,
    test_part1_freeze_empty_payload_behavior,
    test_part2_step9_reachability,
    test_part2_review_before_sign_api_bypass_rejected,
    test_part2_crm_staff_cannot_write_signature,
    test_part2_guarantor_link_authorization,
    test_part2_authentication_otp_replay_prevention,
    test_part3_signed_at_utc_only,
    test_part4_pdf_evidential_wording_values,
    test_part5_assisted_flow_missing_attestation_rejected,
    test_transparent_png_alpha_channel_required,
    test_part6_storage_never_overwrites,
    test_part7_unassigned_staff_access_rejected
)

print("Running Edge Case Tests:")

print("1. test_part1_draft_edits_succeed_and_log")
asyncio.run(test_part1_draft_edits_succeed_and_log())

print("2. test_part1_frozen_edits_rejected_with_409")
asyncio.run(test_part1_frozen_edits_rejected_with_409())

print("3. test_part1_concurrent_edits_prevention")
asyncio.run(test_part1_concurrent_edits_prevention())

print("4. test_part1_freeze_idempotency")
asyncio.run(test_part1_freeze_idempotency())

print("5. test_part1_correction_supersedes_and_invalidates_only_affected")
asyncio.run(test_part1_correction_supersedes_and_invalidates_only_affected())

print("6. test_part1_correction_after_signed_allowed")
asyncio.run(test_part1_correction_after_signed_allowed())

print("7. test_part1_payload_hash_deterministic")
test_part1_payload_hash_deterministic()

print("8. test_part1_payload_hash_whitespace_insignificant")
test_part1_payload_hash_whitespace_insignificant()

print("9. test_part1_version_number_sequencing")
asyncio.run(test_part1_version_number_sequencing())

print("10. test_part1_freeze_empty_payload_behavior")
asyncio.run(test_part1_freeze_empty_payload_behavior())

print("11. test_part2_step9_reachability")
asyncio.run(test_part2_step9_reachability())

print("12. test_part2_review_before_sign_api_bypass_rejected")
asyncio.run(test_part2_review_before_sign_api_bypass_rejected())

print("13. test_part2_crm_staff_cannot_write_signature")
asyncio.run(test_part2_crm_staff_cannot_write_signature())

print("14. test_part2_guarantor_link_authorization")
asyncio.run(test_part2_guarantor_link_authorization())

print("15. test_part2_authentication_otp_replay_prevention")
asyncio.run(test_part2_authentication_otp_replay_prevention())

print("16. test_part3_signed_at_utc_only")
asyncio.run(test_part3_signed_at_utc_only())

print("17. test_part4_pdf_evidential_wording_values")
test_part4_pdf_evidential_wording_values()

print("18. test_part5_assisted_flow_missing_attestation_rejected")
asyncio.run(test_part5_assisted_flow_missing_attestation_rejected())

print("19. test_transparent_png_alpha_channel_required")
test_transparent_png_alpha_channel_required()

print("20. test_part6_storage_never_overwrites")
test_part6_storage_never_overwrites()

print("21. test_part7_unassigned_staff_access_rejected")
asyncio.run(test_part7_unassigned_staff_access_rejected())

print("ALL EDGE CASES PASSED SUCCESSFULLY!")
