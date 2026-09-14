-- Village Companion — initial Supabase/PostgreSQL schema
-- Safe to run on a new Supabase project.
-- V1 intentionally avoids payments, ratings and booking guarantees.

create extension if not exists pgcrypto;

-- -----------------------------
-- 1. Villages
-- -----------------------------
create table if not exists public.villages (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    district text,
    state text not null default 'Rajasthan',
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (name, district, state)
);

-- -----------------------------
-- 2. Service categories
-- -----------------------------
create table if not exists public.services (
    id uuid primary key default gen_random_uuid(),
    name text not null unique,
    subtitle text,
    emoji text,
    sort_order integer not null default 0,
    is_active boolean not null default true,
    created_at timestamptz not null default now()
);

-- -----------------------------
-- 3. User profiles
-- auth.users remains the source of authentication identity.
-- -----------------------------
create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    full_name text,
    village_id uuid references public.villages(id) on delete set null,
    role text not null default 'customer'
        check (role in ('customer', 'provider', 'admin')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Create a safe default profile automatically whenever a new auth user is created.
-- Role is always the database default (customer); client metadata cannot grant admin.
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

-- -----------------------------
-- 4. Provider listings
-- One profile can have one provider listing in V1.
-- -----------------------------
create table if not exists public.providers (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null unique references public.profiles(id) on delete cascade,
    service_id uuid not null references public.services(id) on delete restrict,
    village_id uuid not null references public.villages(id) on delete restrict,
    phone text not null check (phone ~ '^[0-9]{10}$'),
    availability text not null default 'available_now'
        check (availability in ('available_now', 'available_today', 'unavailable')),
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- -----------------------------
-- 5. Reports / abuse handling
-- Kept small for V1; admin can review later.
-- -----------------------------
create table if not exists public.reports (
    id uuid primary key default gen_random_uuid(),
    reporter_id uuid references public.profiles(id) on delete set null,
    provider_id uuid not null references public.providers(id) on delete cascade,
    reason text not null,
    details text,
    status text not null default 'open'
        check (status in ('open', 'reviewing', 'resolved', 'rejected')),
    created_at timestamptz not null default now()
);

-- -----------------------------
-- Helpful indexes
-- -----------------------------
create index if not exists idx_providers_service_village
    on public.providers(service_id, village_id)
    where is_active = true;

create index if not exists idx_providers_village
    on public.providers(village_id)
    where is_active = true;

create index if not exists idx_reports_provider_status
    on public.reports(provider_id, status);

-- -----------------------------
-- updated_at helper
-- -----------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
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

-- -----------------------------
-- Seed V1 service categories
-- -----------------------------
insert into public.services (name, subtitle, emoji, sort_order)
values
    ('कृषि मजदूर', 'खेत का काम', '🌾', 1),
    ('Tractor / मशीन', 'किराये पर मशीन', '🚜', 2),
    ('Electrician', 'बिजली का काम', '⚡', 3),
    ('Motor / Pump', 'मरम्मत सेवा', '⚙️', 4),
    ('Plumber', 'पानी की लाइन', '🚰', 5),
    ('Mason / Welding', 'निर्माण काम', '🧱', 6)
on conflict (name) do update set
    subtitle = excluded.subtitle,
    emoji = excluded.emoji,
    sort_order = excluded.sort_order;

-- -----------------------------
-- RLS
-- Public marketplace reads only active, non-sensitive listing fields.
-- Provider phone is intentionally public in V1 because the app's core action
-- is direct Call/WhatsApp. We can add verified-contact/privacy controls later.
-- -----------------------------
alter table public.villages enable row level security;
alter table public.services enable row level security;
alter table public.profiles enable row level security;
alter table public.providers enable row level security;
alter table public.reports enable row level security;

drop policy if exists "public read active villages" on public.villages;
create policy "public read active villages"
on public.villages for select
to anon, authenticated
using (is_active = true);

drop policy if exists "public read active services" on public.services;
create policy "public read active services"
on public.services for select
to anon, authenticated
using (is_active = true);

drop policy if exists "public read active providers" on public.providers;
create policy "public read active providers"
on public.providers for select
to anon, authenticated
using (is_active = true);

drop policy if exists "user read own profile" on public.profiles;
create policy "user read own profile"
on public.profiles for select
to authenticated
using (id = auth.uid());

drop policy if exists "user insert own profile" on public.profiles;
create policy "user insert own profile"
on public.profiles for insert
to authenticated
with check (id = auth.uid());

drop policy if exists "user update own profile" on public.profiles;
create policy "user update own profile"
on public.profiles for update
to authenticated
using (id = auth.uid())
with check (id = auth.uid());

drop policy if exists "user insert own provider" on public.providers;
create policy "user insert own provider"
on public.providers for insert
to authenticated
with check (profile_id = auth.uid());

drop policy if exists "user update own provider" on public.providers;
create policy "user update own provider"
on public.providers for update
to authenticated
using (profile_id = auth.uid())
with check (profile_id = auth.uid());

drop policy if exists "user delete own provider" on public.providers;
create policy "user delete own provider"
on public.providers for delete
to authenticated
using (profile_id = auth.uid());

drop policy if exists "authenticated report provider" on public.reports;
create policy "authenticated report provider"
on public.reports for insert
to authenticated
with check (reporter_id = auth.uid());

-- No public INSERT/UPDATE/DELETE policies are intentionally provided.
-- Admin moderation policies should be added only after an admin role design
-- is implemented; never trust a client-provided role alone.
