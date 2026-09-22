-- Idempotent repair migration for the canonical active flag.
-- Run after schema.sql when upgrading an existing database.
-- Canonical column: active. Legacy column: is_active.

begin;

do $$
begin
  if to_regclass('public.villages') is not null then
    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='villages' and column_name='is_active')
       and not exists (select 1 from information_schema.columns where table_schema='public' and table_name='villages' and column_name='active') then
      alter table public.villages rename column is_active to active;
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='villages' and column_name='is_active')
       and exists (select 1 from information_schema.columns where table_schema='public' and table_name='villages' and column_name='active') then
      update public.villages set active = coalesce(active, is_active);
      alter table public.villages drop column is_active;
    end if;
  end if;

  if to_regclass('public.services') is not null then
    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='services' and column_name='is_active')
       and not exists (select 1 from information_schema.columns where table_schema='public' and table_name='services' and column_name='active') then
      alter table public.services rename column is_active to active;
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='services' and column_name='is_active')
       and exists (select 1 from information_schema.columns where table_schema='public' and table_name='services' and column_name='active') then
      update public.services set active = coalesce(active, is_active);
      alter table public.services drop column is_active;
    end if;
  end if;

  if to_regclass('public.providers') is not null then
    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='providers' and column_name='is_active')
       and not exists (select 1 from information_schema.columns where table_schema='public' and table_name='providers' and column_name='active') then
      alter table public.providers rename column is_active to active;
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='providers' and column_name='is_active')
       and exists (select 1 from information_schema.columns where table_schema='public' and table_name='providers' and column_name='active') then
      update public.providers set active = coalesce(active, is_active);
      alter table public.providers drop column is_active;
    end if;
  end if;
end;
$$;

drop policy if exists "public read active villages" on public.villages;
create policy "public read active villages"
on public.villages for select
to anon, authenticated
using (active = true);

drop policy if exists "public read active services" on public.services;
create policy "public read active services"
on public.services for select
to anon, authenticated
using (active = true);

drop policy if exists "public read active providers" on public.providers;
create policy "public read active providers"
on public.providers for select
to anon, authenticated
using (active = true);

grant usage on schema public to anon, authenticated;
grant select on table public.villages, public.services, public.providers to anon, authenticated;

notify pgrst, 'reload schema';

commit;
