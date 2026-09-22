-- Repair migration for the location hierarchy.
-- Run after schema.sql and location_hierarchy.sql.
-- The canonical column is public.villages.active (not is_active).

begin;

-- Remove policies that reference the old/nonexistent column.
drop policy if exists "public read active villages" on public.villages;

create policy "public read active villages"
on public.villages
for select
to anon, authenticated
using (active = true);

-- Ensure the Data API roles can read the public catalogue.
grant usage on schema public to anon, authenticated;
grant select on table public.villages to anon, authenticated;

-- Normalize any rows created by an older, incompatible migration.
update public.villages
set active = true
where active is null;

notify pgrst, 'reload schema';

commit;
