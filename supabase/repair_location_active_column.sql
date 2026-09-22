-- Repair migration for the location hierarchy.
-- Run after schema.sql and location_hierarchy.sql.
-- The canonical column is public.villages.active (not is_active).

begin;

-- Ensure the canonical column exists.
alter table public.villages add column if not exists active boolean not null default true;

-- Remove stale or incompatible policy definitions.
drop policy if exists "public read active villages" on public.villages;

-- Recreate policy using the canonical column.
create policy "public read active villages"
on public.villages
for select
to anon, authenticated
using (active = true);

-- Normalize any rows created by an earlier migration using is_active.
update public.villages
set active = true
where active is null;

-- Keep Data API access working for public lookup tables.
grant usage on schema public to anon, authenticated;
grant select on table public.villages to anon, authenticated;

notify pgrst, 'reload schema';

commit;
