from app.core.exceptions import DomainException

NOTE_VISIBILITY={"audit":{"auditor"},"legal":{"legal","auditor"},"internal_confidential":{"credit_analyst","crm","head_crm","ed","md","auditor"}}
class TaskService:
    def __init__(self,conn):self.conn=conn
    async def my_work(self,user):
        return await self.conn.fetch("""SELECT wt.*,la.ref_no,la.applicant_name FROM work_tasks wt LEFT JOIN loan_applications la ON la.id=wt.application_id
          WHERE wt.org_id=$1 AND (wt.assigned_user_id=$2 OR wt.assigned_role=$3) AND wt.status NOT IN('completed','cancelled')
          ORDER BY CASE wt.priority WHEN 'urgent' THEN 0 WHEN 'high' THEN 1 ELSE 2 END,wt.due_at NULLS LAST""",user.org_id,user.id,user.role)
    async def add_note(self,user,application_id,category,body,mentions):
        allowed=NOTE_VISIBILITY.get(category)
        if allowed and user.role not in allowed:raise DomainException("You cannot create this note category.",403)
        if not body.strip() or len(body)>5000:raise DomainException("Enter a note up to 5,000 characters.",422)
        async with self.conn.transaction():
            note=await self.conn.fetchrow("INSERT INTO application_notes(org_id,application_id,category,body,created_by) VALUES($1,$2,$3,$4,$5) RETURNING *",user.org_id,application_id,category,body.strip(),user.id)
            for mentioned in set(mentions):
                valid=await self.conn.fetchval("SELECT id FROM users WHERE id=$1 AND org_id=$2 AND active=TRUE",mentioned,user.org_id)
                if valid:
                    await self.conn.execute("INSERT INTO note_mentions(note_id,user_id) VALUES($1,$2) ON CONFLICT DO NOTHING",note["id"],mentioned)
                    await self.conn.execute("INSERT INTO notifications(user_id,org_id,application_id,title,message,notification_type) VALUES($1,$2,$3,'You were mentioned',$4,'mention')",mentioned,user.org_id,application_id,body.strip()[:300])
        return note
    async def require_conditions(self,application_id,org_id):
        missing=await self.conn.fetch("SELECT condition_type,description FROM disbursement_conditions WHERE application_id=$1 AND org_id=$2 AND mandatory=TRUE AND satisfied_at IS NULL",application_id,org_id)
        if missing:raise DomainException("Conditions precedent are incomplete: "+", ".join(row["description"] for row in missing),409)
