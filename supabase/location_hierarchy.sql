-- Village Companion — location hierarchy migration
-- Source design: Government of India LGD / Integrated Government Online Directory.
-- Hierarchy used by the app: State -> District -> Development Block -> Village.
-- This migration is additive and does not delete existing marketplace data.

create table if not exists public.districts (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    state text not null default 'Rajasthan',
    lgd_code bigint,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (name, state),
    unique (lgd_code)
);

create table if not exists public.blocks (
    id uuid primary key default gen_random_uuid(),
    district_id uuid not null references public.districts(id) on delete cascade,
    name text not null,
    lgd_code bigint,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (district_id, name),
    unique (lgd_code)
);

-- Keep the existing villages table so provider listings and location records
-- remain compatible with the original V1 schema.
alter table public.villages
    add column if not exists block_id uuid references public.blocks(id) on delete set null;

alter table public.villages
    add column if not exists lgd_code bigint;

create unique index if not exists uq_villages_lgd_code
    on public.villages(lgd_code)
    where lgd_code is not null;

create index if not exists idx_blocks_district
    on public.blocks(district_id)
    where is_active = true;

create index if not exists idx_villages_block
    on public.villages(block_id)
    where is_active = true;

-- Public marketplace location selectors only need active names/codes.
alter table public.districts enable row level security;
alter table public.blocks enable row level security;

drop policy if exists "public read active districts" on public.districts;
create policy "public read active districts"
on public.districts for select
to anon, authenticated
using (is_active = true);

drop policy if exists "public read active blocks" on public.blocks;
create policy "public read active blocks"
on public.blocks for select
to anon, authenticated
using (is_active = true);

-- Seed the currently published Barmer development blocks from the
-- Government of India's Integrated Government Online Directory (LGD source).
insert into public.districts (name, state)
values ('Barmer', 'Rajasthan')
on conflict (name, state) do nothing;

insert into public.blocks (district_id, name)
select d.id, x.name
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
) as x(name)
where d.name = 'Barmer' and d.state = 'Rajasthan'
on conflict (district_id, name) do nothing;

-- Optional compatibility backfill for the old V1 rows.
-- These rows are only matched when a village name is already present in the
-- imported LGD village catalogue; no guessed village->block mapping is made.
update public.villages v
set district = d.name
from public.districts d
where v.district is null
  and d.name = 'Barmer';

comment on table public.districts is
    'Administrative districts sourced from Government of India LGD/IGOD.';
comment on table public.blocks is
    'Development blocks sourced from Government of India LGD/IGOD.';
comment on column public.villages.block_id is
    'Development block for this village; populated from the authoritative LGD block-village mapping import.';
comment on column public.villages.lgd_code is
    'Unique LGD village code where available.';
