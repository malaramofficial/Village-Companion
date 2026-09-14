-- Village Companion — Barmer district location hierarchy
-- V1 scope: entire current Barmer district.
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

alter table public.villages add column if not exists block_id uuid references public.blocks(id) on delete set null;
alter table public.villages add column if not exists lgd_code bigint;

create unique index if not exists uq_villages_lgd_code on public.villages(lgd_code) where lgd_code is not null;
create index if not exists idx_blocks_district on public.blocks(district_id) where is_active = true;
create index if not exists idx_villages_block on public.villages(block_id) where is_active = true;

alter table public.districts enable row level security;
alter table public.blocks enable row level security;

drop policy if exists "public read active districts" on public.districts;
create policy "public read active districts" on public.districts for select to anon, authenticated using (is_active = true);

drop policy if exists "public read active blocks" on public.blocks;
create policy "public read active blocks" on public.blocks for select to anon, authenticated using (is_active = true);

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

comment on table public.districts is 'V1 location scope: entire current Barmer district, Rajasthan.';
comment on table public.blocks is 'V1 location scope: all 11 current Barmer development blocks according to the current LGD-backed government directory.';
comment on column public.villages.block_id is 'Authoritative LGD block mapping; no guessed mappings.';
comment on column public.villages.lgd_code is 'LGD village code where available.';
