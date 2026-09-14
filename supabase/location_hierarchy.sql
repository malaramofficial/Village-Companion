-- Village Companion — Barmer district location hierarchy
-- V1 scope: entire current Barmer district.
-- Aadel Panchayat Samiti GP list is seeded for the first location-selection release.
-- Village rows must be imported from authoritative LGD data; no guessed mappings.

create table if not exists public.districts (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    state text not null default 'Rajasthan',
    lgd_code bigint,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (name, state), unique (lgd_code)
);

create table if not exists public.blocks (
    id uuid primary key default gen_random_uuid(),
    district_id uuid not null references public.districts(id) on delete cascade,
    name text not null,
    lgd_code bigint,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (district_id, name), unique (lgd_code)
);

create table if not exists public.gram_panchayats (
    id uuid primary key default gen_random_uuid(),
    block_id uuid not null references public.blocks(id) on delete cascade,
    name text not null,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (block_id, name)
);

alter table public.villages add column if not exists block_id uuid references public.blocks(id) on delete set null;
alter table public.villages add column if not exists gram_panchayat_id uuid references public.gram_panchayats(id) on delete set null;
alter table public.villages add column if not exists lgd_code bigint;

create unique index if not exists uq_villages_lgd_code on public.villages(lgd_code) where lgd_code is not null;
create index if not exists idx_blocks_district on public.blocks(district_id) where is_active = true;
create index if not exists idx_gram_panchayats_block on public.gram_panchayats(block_id) where is_active = true;
create index if not exists idx_villages_block on public.villages(block_id) where is_active = true;
create index if not exists idx_villages_gram_panchayat on public.villages(gram_panchayat_id) where is_active = true;

alter table public.districts enable row level security;
alter table public.blocks enable row level security;
alter table public.gram_panchayats enable row level security;

drop policy if exists "public read active districts" on public.districts;
create policy "public read active districts" on public.districts for select to anon, authenticated using (is_active = true);

drop policy if exists "public read active blocks" on public.blocks;
create policy "public read active blocks" on public.blocks for select to anon, authenticated using (is_active = true);

drop policy if exists "public read active gram panchayats" on public.gram_panchayats;
create policy "public read active gram panchayats" on public.gram_panchayats for select to anon, authenticated using (is_active = true);

insert into public.districts (name, state)
values ('Barmer', 'Rajasthan')
on conflict (name, state) do nothing;

insert into public.blocks (district_id, name)
select d.id, b.name
from public.districts d
cross join (values
    ('Aadel'),
    ('Barmer'),
    ('Barmer Rural'),
    ('Baytoo'),
    ('Chohtan'),
    ('Dhanau'),
    ('Fagliya'),
    ('Gadra Road'),
    ('Ramsar'),
    ('Sedwa'),
    ('Sheo')
) as b(name)
where d.name = 'Barmer' and d.state = 'Rajasthan'
on conflict (district_id, name) do nothing;

-- Aadel Panchayat Samiti: 20 Gram Panchayats.
-- Names are based on the currently available Barmer/Aadel GP listing.
insert into public.gram_panchayats (block_id, name)
select b.id, gp.name
from public.blocks b
cross join (values
    ('AASUON KI DHANI'),
    ('ADARSH ADEL'),
    ('ARJUN KI DHANI'),
    ('BHAMBHU NAGAR'),
    ('DHOLPALIYA NADA'),
    ('KHARDI BERI'),
    ('KHARIYA KHURD'),
    ('MEETHI BERI'),
    ('SADECHA'),
    ('अणखिया'),
    ('आडेल'),
    ('गोलिया जैतमाल'),
    ('छोटु'),
    ('धोलानाडा'),
    ('निम्बलकोट'),
    ('नोखड़ा'),
    ('बाण्ड'),
    ('मालपुरा'),
    ('मंगले की बेरी'),
    ('राणासर खुर्द')
) as gp(name)
where b.name = 'Aadel'
on conflict (block_id, name) do nothing;

comment on table public.districts is 'V1 location scope: entire current Barmer district, Rajasthan.';
comment on table public.blocks is 'V1 scope: current Barmer development blocks.';
comment on table public.gram_panchayats is 'Aadel Gram Panchayat selection list seeded from the current available Aadel GP listing; village membership will be mapped only from authoritative LGD data.';
comment on column public.villages.block_id is 'Authoritative LGD block mapping; no guessed mappings.';
comment on column public.villages.gram_panchayat_id is 'Authoritative Gram Panchayat mapping where available.';
comment on column public.villages.lgd_code is 'LGD village code where available.';
