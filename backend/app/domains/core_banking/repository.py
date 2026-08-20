from __future__ import annotations

from uuid import UUID

from app.domains.core_banking.models import CoreBankingInstallment, CoreBankingLoan, CoreBankingTransaction


class CoreBankingRepository:
    def __init__(self, conn):
        self.conn = conn

    async def get_mapping(self, loan_id: UUID, org_id: UUID) -> dict | None:
        row = await self.conn.fetchrow(
            """
            SELECT la.id, la.org_id, la.external_customer_id, la.external_loan_id,
                   COALESCE(la.cbs_provider, $3) AS cbs_provider,
                   la.cbs_sync_status, lp.cbs_enabled
            FROM loan_applications la
            JOIN loan_products lp ON lp.code = la.loan_type
            WHERE la.id = $1 AND la.org_id = $2 AND la.deleted_at IS NULL
            """,
            loan_id, org_id, "mock",
        )
        return dict(row) if row else None

    async def find_mapping_by_external_loan(self, org_id: UUID, provider: str, external_loan_id: str) -> dict | None:
        row = await self.conn.fetchrow(
            """
            SELECT la.id, la.org_id, la.external_customer_id, la.external_loan_id,
                   la.cbs_provider, lp.cbs_enabled
            FROM loan_applications la
            JOIN loan_products lp ON lp.code = la.loan_type
            WHERE la.org_id=$1 AND la.cbs_provider=$2 AND la.external_loan_id=$3
              AND la.deleted_at IS NULL
            """,
            org_id, provider, external_loan_id,
        )
        return dict(row) if row else None

    async def start_run(
        self, *, loan_id: UUID | None, org_id: UUID, provider: str, trigger: str,
        requested_by: UUID | None, external_event_id: str | None,
    ) -> UUID | None:
        row = await self.conn.fetchrow(
            """
            INSERT INTO core_banking_sync_runs
              (loan_id,org_id,provider,trigger_type,status,requested_by,external_event_id)
            VALUES ($1,$2,$3,$4,'started',$5,$6)
            ON CONFLICT (org_id,provider,external_event_id)
              WHERE external_event_id IS NOT NULL DO NOTHING
            RETURNING id
            """,
            loan_id, org_id, provider, trigger, requested_by, external_event_id,
        )
        return row["id"] if row else None

    async def finish_run(
        self, run_id: UUID, *, status: str, transactions: int = 0, schedules: int = 0,
        error_code: str | None = None, error_message: str | None = None,
    ) -> None:
        await self.conn.execute(
            """
            UPDATE core_banking_sync_runs
            SET status=$2, transactions_imported=$3, schedules_imported=$4,
                error_code=$5, error_message=$6, completed_at=NOW()
            WHERE id=$1
            """,
            run_id, status, transactions, schedules, error_code, error_message,
        )

    async def record_issue(
        self, *, loan_id: UUID | None, org_id: UUID, provider: str,
        issue_type: str, external_reference: str | None, details: str,
    ) -> None:
        await self.conn.execute(
            """
            INSERT INTO core_banking_reconciliation_issues
              (loan_id,org_id,provider,issue_type,external_reference,details)
            VALUES ($1,$2,$3,$4,$5,$6)
            """,
            loan_id, org_id, provider, issue_type, external_reference, details[:1000],
        )

    async def mark_failure(self, loan_id: UUID, org_id: UUID, status: str, message: str) -> None:
        await self.conn.execute(
            """UPDATE loan_applications
               SET cbs_sync_status=$3, cbs_sync_error=$4, updated_at=NOW()
               WHERE id=$1 AND org_id=$2""",
            loan_id, org_id, status, message[:500],
        )

    async def apply_snapshot(self, loan_id: UUID, org_id: UUID, provider: str, loan: CoreBankingLoan) -> None:
        await self.conn.execute(
            """
            INSERT INTO core_banking_loan_snapshots
              (loan_id,org_id,provider,external_loan_id,outstanding_balance,
               principal_balance,arrears_amount,days_past_due,loan_status,
               disbursed_amount,disbursed_at,source_updated_at,received_at)
            VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,NOW())
            ON CONFLICT (loan_id) DO UPDATE SET
              provider=EXCLUDED.provider, external_loan_id=EXCLUDED.external_loan_id,
              outstanding_balance=EXCLUDED.outstanding_balance,
              principal_balance=EXCLUDED.principal_balance,
              arrears_amount=EXCLUDED.arrears_amount,
              days_past_due=EXCLUDED.days_past_due, loan_status=EXCLUDED.loan_status,
              disbursed_amount=EXCLUDED.disbursed_amount, disbursed_at=EXCLUDED.disbursed_at,
              source_updated_at=EXCLUDED.source_updated_at, received_at=NOW()
            """,
            loan_id, org_id, provider, loan.external_loan_id,
            loan.outstanding_balance, loan.principal_balance, loan.arrears_amount,
            loan.days_past_due, loan.loan_status, loan.disbursed_amount,
            loan.disbursed_at, loan.source_updated_at,
        )

    async def insert_transaction(
        self, loan_id: UUID, org_id: UUID, provider: str, transaction: CoreBankingTransaction,
    ) -> str:
        row = await self.conn.fetchrow(
            """
            INSERT INTO core_banking_transactions
              (loan_id,org_id,provider,external_transaction_id,transaction_type,
               amount,transaction_at,value_date,currency,source_updated_at)
            VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)
            ON CONFLICT (org_id,provider,external_transaction_id) DO NOTHING
            RETURNING id
            """,
            loan_id, org_id, provider, transaction.external_transaction_id,
            transaction.transaction_type, transaction.amount, transaction.transaction_at,
            transaction.value_date, transaction.currency, transaction.source_updated_at,
        )
        if row:
            return "inserted"
        existing = await self.conn.fetchrow(
            """SELECT loan_id,amount,transaction_at,transaction_type
               FROM core_banking_transactions
               WHERE org_id=$1 AND provider=$2 AND external_transaction_id=$3""",
            org_id, provider, transaction.external_transaction_id,
        )
        if existing and (
            str(existing["loan_id"]) != str(loan_id)
            or existing["amount"] != transaction.amount
            or existing["transaction_at"] != transaction.transaction_at
            or existing["transaction_type"] != transaction.transaction_type
        ):
            return "conflict"
        return "duplicate"

    async def upsert_installment(
        self, loan_id: UUID, org_id: UUID, provider: str, installment: CoreBankingInstallment,
    ) -> None:
        await self.conn.execute(
            """
            INSERT INTO core_banking_schedule
              (loan_id,org_id,provider,external_installment_id,installment_no,due_date,
               principal_due,interest_due,total_due,amount_paid,status,source_updated_at)
            VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)
            ON CONFLICT (loan_id,installment_no) DO UPDATE SET
              external_installment_id=EXCLUDED.external_installment_id,
              due_date=EXCLUDED.due_date, principal_due=EXCLUDED.principal_due,
              interest_due=EXCLUDED.interest_due, total_due=EXCLUDED.total_due,
              amount_paid=EXCLUDED.amount_paid, status=EXCLUDED.status,
              source_updated_at=EXCLUDED.source_updated_at, received_at=NOW()
            """,
            loan_id, org_id, provider, installment.external_installment_id,
            installment.installment_no, installment.due_date, installment.principal_due,
            installment.interest_due, installment.total_due, installment.amount_paid,
            installment.status, installment.source_updated_at,
        )

    async def mark_success(self, loan_id: UUID, org_id: UUID, provider: str, source_updated_at) -> None:
        await self.conn.execute(
            """UPDATE loan_applications
               SET cbs_provider=$3, cbs_sync_status='success', cbs_sync_error=NULL,
                   cbs_last_successful_sync_at=$4, updated_at=NOW()
               WHERE id=$1 AND org_id=$2""",
            loan_id, org_id, provider, source_updated_at,
        )
        for field_name in (
            "outstanding_balance", "principal_balance", "arrears_amount",
            "days_past_due", "loan_status", "disbursed_amount", "disbursed_at",
        ):
            await self.conn.execute(
                """
                INSERT INTO field_value_metadata
                  (org_id,entity_type,entity_id,field_name,source,captured_at,verified,verification_source)
                VALUES ($1,'loan_application',$2,$3,'cbs',$4,TRUE,$5)
                ON CONFLICT (org_id,entity_type,entity_id,field_name) DO UPDATE SET
                  source='cbs', captured_at=EXCLUDED.captured_at,
                  verified=TRUE, verification_source=EXCLUDED.verification_source
                """,
                org_id, loan_id, field_name, source_updated_at, provider,
            )

    async def upsert_field_metadata(
        self, *, org_id: UUID, entity_type: str, entity_id: UUID,
        field_name: str, source: str, captured_by: UUID | None,
    ) -> None:
        await self.conn.execute(
            """
            INSERT INTO field_value_metadata
              (org_id,entity_type,entity_id,field_name,source,captured_by)
            VALUES ($1,$2,$3,$4,$5,$6)
            ON CONFLICT (org_id,entity_type,entity_id,field_name) DO UPDATE SET
              source=EXCLUDED.source, captured_at=NOW(), captured_by=EXCLUDED.captured_by,
              verified=FALSE, verified_by=NULL, verified_at=NULL, verification_source=NULL
            """,
            org_id, entity_type, entity_id, field_name, source, captured_by,
        )

    async def get_view(self, loan_id: UUID, org_id: UUID) -> dict | None:
        snapshot = await self.conn.fetchrow(
            """
            SELECT s.*, la.cbs_last_successful_sync_at, la.cbs_sync_status, la.cbs_sync_error,
                   la.external_customer_id
            FROM core_banking_loan_snapshots s
            JOIN loan_applications la ON la.id=s.loan_id AND la.org_id=s.org_id
            WHERE s.loan_id=$1 AND s.org_id=$2
            """,
            loan_id, org_id,
        )
        if not snapshot:
            return None
        transactions = await self.conn.fetch(
            """SELECT external_transaction_id,transaction_type,amount,transaction_at,
                      value_date,currency,source_updated_at
               FROM core_banking_transactions WHERE loan_id=$1 AND org_id=$2
               ORDER BY transaction_at DESC LIMIT 100""",
            loan_id, org_id,
        )
        schedule = await self.conn.fetch(
            """SELECT external_installment_id,installment_no,due_date,principal_due,
                      interest_due,total_due,amount_paid,status,source_updated_at
               FROM core_banking_schedule WHERE loan_id=$1 AND org_id=$2
               ORDER BY installment_no""",
            loan_id, org_id,
        )
        result = dict(snapshot)
        result["transactions"] = [dict(row) for row in transactions]
        result["schedule"] = [dict(row) for row in schedule]
        return result

    async def list_candidates(self, org_id: UUID, limit: int) -> list[dict]:
        rows = await self.conn.fetch(
            """
            SELECT la.id FROM loan_applications la
            JOIN loan_products lp ON lp.code=la.loan_type
            WHERE la.org_id=$1 AND la.deleted_at IS NULL AND lp.cbs_enabled=TRUE
              AND la.external_customer_id IS NOT NULL AND la.external_loan_id IS NOT NULL
            ORDER BY la.cbs_last_successful_sync_at NULLS FIRST, la.updated_at
            LIMIT $2
            """,
            org_id, limit,
        )
        return [dict(row) for row in rows]
