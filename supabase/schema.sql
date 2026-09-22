-- Village Companion V1 database schema
-- Supabase / PostgreSQL

create extension if not exists pgcrypto;

create table if not exists public.villages (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    district text not null,
    state text not null default 'Rajasthan',
    active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (name, district, state)
);

create table if not exists public.services (
    id uuid primary key default gen_random_uuid(),
    name text not null unique,
    subtitle text,
    emoji text,
    sort_order integer not null default 0,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

-- Cascading location hierarchy used by the Android app.
create table if not exists public.districts (
    id uuid primary key default gen_random_uuid(),
    name text not null unique,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

create table if not exists public.blocks (
    id uuid primary key default gen_random_uuid(),
    district_id uuid not null references public.districts(id) on delete cascade,
    name text not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (district_id, name)
);

create table if not exists public.gram_panchayats (
    id uuid primary key default gen_random_uuid(),
    block_id uuid not null references public.blocks(id) on delete cascade,
    name text not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (block_id, name)
);

-- Keep the original villages table compatible while adding hierarchy references.
alter table public.villages add column if not exists block_id uuid references public.blocks(id) on delete set null;
alter table public.villages add column if not exists gram_panchayat_id uuid references public.gram_panchayats(id) on delete set null;
alter table public.villages add column if not exists lgd_code text;

-- Existing databases may use the legacy is_active name. Normalize it once so
-- the canonical API contract is always active across location/services/providers.
do $
begin
    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='villages' and column_name='is_active')
       and not exists (select 1 from information_schema.columns where table_schema='public' and table_name='villages' and column_name='active') then
        alter table public.villages rename column is_active to active;
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='villages' and column_name='is_active')
       and exists (select 1 from information_schema.columns where table_schema='public' and table_name='villages' and column_name='active') then
        update public.villages set active = coalesce(active, is_active);
        alter table public.villages drop column is_active;
    end if;

    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='services' and column_name='is_active')
       and not exists (select 1 from information_schema.columns where table_schema='public' and table_name='services' and column_name='active') then
        alter table public.services rename column is_active to active;
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='services' and column_name='is_active')
       and exists (select 1 from information_schema.columns where table_schema='public' and table_name='services' and column_name='active') then
        update public.services set active = coalesce(active, is_active);
        alter table public.services drop column is_active;
    end if;

    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='providers' and column_name='is_active')
       and not exists (select 1 from information_schema.columns where table_schema='public' and table_name='providers' and column_name='active') then
        alter table public.providers rename column is_active to active;
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='providers' and column_name='is_active')
       and exists (select 1 from information_schema.columns where table_schema='public' and table_name='providers' and column_name='active') then
        update public.providers set active = coalesce(active, is_active);
        alter table public.providers drop column is_active;
    end if;
end;
$;

create index if not exists idx_blocks_district_id on public.blocks(district_id);
create index if not exists idx_gram_panchayats_block_id on public.gram_panchayats(block_id);
create index if not exists idx_villages_gram_panchayat_id on public.villages(gram_panchayat_id);

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    full_name text,
    village_id uuid references public.villages(id) on delete set null,
    role text not null default 'customer' check (role in ('customer', 'provider', 'admin')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.providers (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null unique references public.profiles(id) on delete cascade,
    service_id uuid not null references public.services(id),
    village_id uuid not null references public.villages(id),
    phone text not null check (phone ~ '^[0-9]{10}$'),
    availability text not null default 'available_now' check (availability in ('available_now', 'available_today', 'unavailable')),
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.reports (
    id uuid primary key default gen_random_uuid(),
    reporter_id uuid not null references public.profiles(id) on delete cascade,
    provider_id uuid not null references public.providers(id) on delete cascade,
    reason text not null,
    details text,
    status text not null default 'open' check (status in ('open', 'reviewing', 'resolved', 'rejected')),
    created_at timestamptz not null default now()
);

create index if not exists idx_providers_service_village on public.providers(service_id, village_id);
create index if not exists idx_providers_active on public.providers(active);
create index if not exists idx_reports_provider on public.reports(provider_id);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = pg_catalog
as $
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
before update on public.profiles
for each row execute function public.set_updated_at();

drop trigger if exists providers_set_updated_at on public.providers;
create trigger providers_set_updated_at
before update on public.providers
for each row execute function public.set_updated_at();

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    insert into public.profiles (id, full_name)
    values (new.id, nullif(new.raw_user_meta_data ->> 'full_name', ''))
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- Seed V1 services.
insert into public.services (name, subtitle, emoji, sort_order)
values
    ('कृषि मजदूर', 'खेत का काम', '🌾', 1),
    ('Tractor / मशीन', 'किराये पर मशीन', '🚜', 2),
    ('Electrician', 'बिजली का काम', '⚡', 3),
    ('Motor / Pump', 'मरम्मत सेवा', '⚙️', 4),
    ('Plumber', 'पानी की लाइन', '🚰', 5),
    ('Mason / Welding', 'निर्माण काम', '🧱', 6)
on conflict (name) do update set subtitle = excluded.subtitle, emoji = excluded.emoji, sort_order = excluded.sort_order, active = true;

-- RLS
alter table public.districts enable row level security;
alter table public.blocks enable row level security;
alter table public.gram_panchayats enable row level security;
alter table public.villages enable row level security;
alter table public.services enable row level security;
alter table public.profiles enable row level security;
alter table public.providers enable row level security;
alter table public.reports enable row level security;

drop policy if exists "public read active districts" on public.districts;
create policy "public read active districts" on public.districts for select using (active = true);

drop policy if exists "public read active blocks" on public.blocks;
create policy "public read active blocks" on public.blocks for select using (active = true);

drop policy if exists "public read active gram panchayats" on public.gram_panchayats;
create policy "public read active gram panchayats" on public.gram_panchayats for select using (active = true);

drop policy if exists "public read active villages" on public.villages;
create policy "public read active villages" on public.villages for select using (active = true);

drop policy if exists "public read active services" on public.services;
create policy "public read active services" on public.services for select using (active = true);

drop policy if exists "authenticated own profile select" on public.profiles;
create policy "authenticated own profile select" on public.profiles for select to authenticated using (id = auth.uid());

drop policy if exists "authenticated own profile insert" on public.profiles;
create policy "user insert own profile" on public.profiles for insert to authenticated with check (id = auth.uid() and role in ('customer', 'provider'));

drop policy if exists "authenticated own profile update" on public.profiles;
create policy "authenticated own profile update" on public.profiles for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

drop policy if exists "authenticated own provider insert" on public.providers;
create policy "authenticated own provider insert" on public.providers for insert to authenticated with check (profile_id = auth.uid());

drop policy if exists "authenticated own provider update" on public.providers;
create policy "authenticated own provider update" on public.providers for update to authenticated using (profile_id = auth.uid()) with check (profile_id = auth.uid());

drop policy if exists "authenticated own provider delete" on public.providers;
create policy "authenticated own provider delete" on public.providers for delete to authenticated using (profile_id = auth.uid());

drop policy if exists "public read active providers" on public.providers;
create policy "public read active providers" on public.providers for select using (active = true);

drop policy if exists "authenticated insert own reports" on public.reports;
create policy "authenticated insert own reports" on public.reports for insert to authenticated with check (reporter_id = auth.uid());

drop policy if exists "authenticated read own reports" on public.reports;
create policy "authenticated read own reports" on public.reports for select to authenticated using (reporter_id = auth.uid());

-- handle_new_user is an internal Auth trigger target, not a public RPC.
revoke execute on function public.handle_new_user() from public, anon, authenticated;

-- Explicit Data API grants. RLS remains the row-level authorization boundary.
grant usage on schema public to anon, authenticated;
grant select on table public.districts, public.blocks, public.gram_panchayats, public.villages, public.services, public.providers to anon, authenticated;
grant select, insert, update on table public.profiles to authenticated;
grant select, insert, update, delete on table public.providers to authenticated;
grant select, insert on table public.reports to authenticated;

-- Prevent a normal client from assigning itself the admin role.
create or replace function public.prevent_role_escalation()
returns trigger
language plpgsql
set search_path = pg_catalog
as $
begin
    if old.role = 'admin' then
        if new.role <> 'admin' then
            raise exception 'Admin role cannot be changed from client';
        end if;
    elsif new.role = 'admin' then
        raise exception 'Admin role cannot be assigned by client';
    end if;
    return new;
end;
$$;

drop trigger if exists profiles_prevent_role_escalation on public.profiles;
create trigger profiles_prevent_role_escalation
before update on public.profiles
for each row execute function public.prevent_role_escalation();
