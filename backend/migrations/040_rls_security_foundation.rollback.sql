-- Emergency rollback for migration 040. Run only as schema owner.
-- Application-layer authorization remains required while RLS is disabled.
DO $$
DECLARE target record;
BEGIN
  FOR target IN
    SELECT schemaname, tablename
    FROM pg_tables
    WHERE schemaname = 'public' AND rowsecurity
  LOOP
    EXECUTE format('ALTER TABLE %I.%I NO FORCE ROW LEVEL SECURITY', target.schemaname, target.tablename);
    EXECUTE format('ALTER TABLE %I.%I DISABLE ROW LEVEL SECURITY', target.schemaname, target.tablename);
  END LOOP;
END $$;
