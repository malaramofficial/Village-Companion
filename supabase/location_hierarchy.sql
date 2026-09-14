-- Village Companion location hierarchy migration/seed
-- Run this file in Supabase SQL Editor after schema.sql.

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

alter table public.villages add column if not exists block_id uuid references public.blocks(id) on delete set null;
alter table public.villages add column if not exists gram_panchayat_id uuid references public.gram_panchayats(id) on delete set null;
alter table public.villages add column if not exists lgd_code text;

create index if not exists idx_blocks_district_id on public.blocks(district_id);
create index if not exists idx_gram_panchayats_block_id on public.gram_panchayats(block_id);
create index if not exists idx_villages_gram_panchayat_id on public.villages(gram_panchayat_id);

-- The location catalogue is public read-only data.
-- Explicit grants are required for newer Supabase Data API projects.
grant usage on schema public to anon, authenticated;
grant select on table public.districts, public.blocks, public.gram_panchayats, public.villages to anon, authenticated;

alter table public.districts enable row level security;
alter table public.blocks enable row level security;
alter table public.gram_panchayats enable row level security;
alter table public.villages enable row level security;

drop policy if exists "public read active districts" on public.districts;
create policy "public read active districts" on public.districts for select to anon, authenticated using (active = true);
drop policy if exists "public read active blocks" on public.blocks;
create policy "public read active blocks" on public.blocks for select to anon, authenticated using (active = true);
drop policy if exists "public read active gram panchayats" on public.gram_panchayats;
create policy "public read active gram panchayats" on public.gram_panchayats for select to anon, authenticated using (active = true);
drop policy if exists "public read active villages" on public.villages;
create policy "public read active villages" on public.villages for select to anon, authenticated using (is_active = true);

-- Current Barmer district and its 11 current blocks.
insert into public.districts (name, active)
values ('Barmer', true)
on conflict (name) do update set active = true;

insert into public.blocks (district_id, name, active)
select d.id, v.name, true
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
) as v(name)
where d.name = 'Barmer'
on conflict (district_id, name) do update set active = true;

-- Aadel gram panchayats represented in the currently validated V1 village data.
insert into public.gram_panchayats (block_id, name, active)
select b.id, v.name, true
from public.blocks b
join public.districts d on d.id = b.district_id
cross join (values
    ('Aasuon Ki Dhani'),
    ('Adarsh Adel'),
    ('Arjun Ki Dhani'),
    ('Bhambhu Nagar'),
    ('Dholpaliya Nada'),
    ('Khardi Beri'),
    ('Khariya Khurd'),
    ('Meethi Beri'),
    ('Sadecha'),
    ('अणखिया'),
    ('आडेल'),
    ('छोटु'),
    ('धोलानाडा'),
    ('निम्बलकोट'),
    ('नोखड़ा'),
    ('बाण्ड'),
    ('मालपुरा'),
    ('मंगले की बेरी'),
    ('राणासर खुर्द')
) as v(name)
where d.name = 'Barmer' and b.name = 'Aadel'
on conflict (block_id, name) do update set active = true;

-- User-validated V1 villages. Names are kept as supplied for reliable display.
with data(gp_name, village_name) as (
    values
    ('Aasuon Ki Dhani','Aasuon Ki Dhani'),('Aasuon Ki Dhani','Bhabhuon Ki Beri'),('Aasuon Ki Dhani','Dhelani Nadi'),('Aasuon Ki Dhani','Godaron Ki Beri'),
    ('Arjun Ki Dhani','Adarsh Chhotu'),('Arjun Ki Dhani','Arjun Ki Dhani'),('Arjun Ki Dhani','Baldev Nagar'),('Arjun Ki Dhani','Bhomani Kadwasaron Ki Dhani'),('Arjun Ki Dhani','Ed Chhotu'),('Arjun Ki Dhani','Khaniya'),('Arjun Ki Dhani','Navlasar'),('Arjun Ki Dhani','Peerani Godaron Ki Dhani'),('Arjun Ki Dhani','Udai Nagar'),('Arjun Ki Dhani','Vijai Nagar'),
    ('Dholpaliya Nada','Bhilon Ka Gol'),('Dholpaliya Nada','Dholpaliya Nada'),('Dholpaliya Nada','Laxmannagar'),('Dholpaliya Nada','Siddhoniyo Khotho Ki Dhani'),
    ('Khardi Beri','Aadhtara'),('Khardi Beri','Adel Panji'),('Khardi Beri','Adel Shivnagar @ Peme Ki Beri'),('Khardi Beri','Bateron Ki Beri'),('Khardi Beri','Jethasar'),('Khardi Beri','Kanani Dhakon Ki Dhani'),('Khardi Beri','Khardi Beri'),('Khardi Beri','Sadul Nagar'),
    ('Khariya Khurd','Bananiyon Ki Beri'),('Khariya Khurd','Bhurasar'),('Khariya Khurd','Choraliya Nada'),('Khariya Khurd','Dhannani Kumharo Ki Dhani'),('Khariya Khurd','Gangapura'),('Khariya Khurd','Khariya Khurd'),('Khariya Khurd','Khubad Mata Mandir'),('Khariya Khurd','Kishanpura'),('Khariya Khurd','Meghwalon Ki Dhani'),('Khariya Khurd','Tarad Nagar'),
    ('Meethi Beri','Bakani Sarnon Ki Dhani'),('Meethi Beri','Daukiyon Ki Dhani'),('Meethi Beri','Kookano Ki Dhaniyan'),('Meethi Beri','Lohamroad And Nanon Ki Dhani'),('Meethi Beri','Meethi Beri'),('Meethi Beri','Rajnagar'),('Meethi Beri','Sarnon And Sewaron Ki Dhani'),('Meethi Beri','Thoriyon Ki Dhani'),('Meethi Beri','Visvakarma Nagar'),
    ('Sadecha','Budhrani Hudon Ki Dhani'),('Sadecha','Karminagar'),('Sadecha','Panani Dhatarwalon Ki Dhani'),('Sadecha','Rupnagar'),('Sadecha','Sadecha'),('Sadecha','Shivnagar Sadecha'),('Sadecha','Utam Nagar'),
    ('छोटु','Behanoni Saranon Ki Dhani'),('छोटु','Chhotu'),('छोटु','Gogaji Ka Mandir Chhotu'),('छोटु','Jhardasar'),('छोटु','Jhurdo Ki Dhani'),('छोटु','Mahadeo Mandir'),('छोटु','Pokrasar'),('छोटु','Sanwalsar'),
    ('मालपुरा','Heerpura'),('मालपुरा','Hukmani Khoton Ki Dhani'),('मालपुरा','Malpura'),('मालपुरा','Thoriyon Ka Tala'),
    ('मंगले की बेरी','Ambedkar Nagar'),('मंगले की बेरी','Dhanne Bhil Ki Dhani'),('मंगले की बेरी','Gadher Magwalo Ki Dhani'),('मंगले की बेरी','Khumoni Beniwalon Ki Dhani'),('मंगले की बेरी','Mangle Ki Beri'),('मंगले की बेरी','Naya Kua'),('मंगले की बेरी','Radon And Kumharo Ki Dhani'),('मंगले की बेरी','Ramnagar'),('मंगले की बेरी','Tejasar'),('मंगले की बेरी','Vagoni Dhatarwalon Ki Dhani'),('मंगले की बेरी','Wankalsar'),
    ('निम्बलकोट','Bheraram Moondh Nagar'),('निम्बलकोट','Doloni Siyago Ka Tala'),('निम्बलकोट','Lakhoni Godaron Ki Dhani'),('निम्बलकोट','Lakhoni Megwalon Ki Dhani'),('निम्बलकोट','Neembal Kot'),('निम्बलकोट','Neembal Nadi'),('निम्बलकोट','Siyagon Ki Dhani Chak No1'),
    ('नोखड़ा','Adarsh Nokhra'),('नोखड़ा','Bhomani Meghwalon Ki Dhani'),('नोखड़ा','Guruon Ka Tala'),('नोखड़ा','Hira Nagar'),('नोखड़ा','Jagram Ki Dhani'),('नोखड़ा','N.T. Nagar'),('नोखड़ा','Nehron Ka Tala'),('नोखड़ा','Nokhra'),('नोखड़ा','Salgasar')
)
insert into public.villages (name, district, state, block_id, gram_panchayat_id, is_active)
select dta.village_name, 'Barmer', 'Rajasthan', b.id, gp.id, true
from data dta
join public.gram_panchayats gp on gp.name = dta.gp_name
join public.blocks b on b.id = gp.block_id
join public.districts d on d.id = b.district_id and d.name = 'Barmer'
on conflict (name, district, state) do update set block_id = excluded.block_id, gram_panchayat_id = excluded.gram_panchayat_id, is_active = true;

-- Refresh PostgREST schema metadata after the migration.
notify pgrst, 'reload schema';

select 'location hierarchy ready' as status,
       (select count(*) from public.blocks b join public.districts d on d.id=b.district_id where d.name='Barmer') as blocks,
       (select count(*) from public.gram_panchayats gp join public.blocks b on b.id=gp.block_id join public.districts d on d.id=b.district_id where d.name='Barmer' and b.name='Aadel') as gram_panchayats,
       (select count(*) from public.villages where district='Barmer' and gram_panchayat_id is not null and is_active = true) as villages;
