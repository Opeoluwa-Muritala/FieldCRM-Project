from fastapi import HTTPException


MCC_MIN_DISTINCT_VOTES = 2
MCC_EXECUTIVE_ROLES = {"ed", "md"}


async def require_mcc_quorum(conn, application_id, org_id) -> dict:
    """Require independent committee input before CRM records the final amount."""
    row = await conn.fetchrow(
        """
        SELECT COUNT(DISTINCT cv.member_id) AS vote_count,
               COUNT(DISTINCT cv.member_id) FILTER (WHERE u.role IN ('ed', 'md')) AS executive_vote_count
        FROM committee_votes cv
        JOIN users u ON u.id = cv.member_id AND u.org_id = cv.org_id
        WHERE cv.loan_id = $1 AND cv.org_id = $2
        """,
        application_id,
        org_id,
    )
    vote_count = int((row or {}).get("vote_count") or 0)
    executive_vote_count = int((row or {}).get("executive_vote_count") or 0)
    if vote_count < MCC_MIN_DISTINCT_VOTES or executive_vote_count < 1:
        raise HTTPException(
            status_code=409,
            detail=(
                "MCC finalization requires at least two distinct recommendations, "
                "including one from an Executive Director or Managing Director."
            ),
        )
    return {"vote_count": vote_count, "executive_vote_count": executive_vote_count}
