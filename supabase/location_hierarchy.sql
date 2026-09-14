-- Village Companion — Barmer district location hierarchy
-- V1 scope: entire current Barmer district.
-- Aadel Panchayat Samiti GP list is seeded for the first location-selection release.
-- Village rows below are only the village mappings that have been manually verified for this release.

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

drop policy if exists "public read active villages" on public.villages;
create policy "public read active villages" on public.villages for select to anon, authenticated using (is_active = true);

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

-- ---------------------------------------------------------
-- Verified village mappings supplied for the first data batch.
-- No LGD code is invented here; codes can be added after LGD import.
-- ---------------------------------------------------------
with village_seed(gp_name, village_name) as (
    values
    -- Aasuon Ki Dhani
    ('AASUON KI DHANI', 'आसुओं की ढाणी'),
    ('AASUON KI DHANI', 'भाम्भुओं की बेरी'),
    ('AASUON KI DHANI', 'ढेलाणी नाडी'),
    ('AASUON KI DHANI', 'गोदारों की बेरी'),

    -- Arjun Ki Dhani
    ('ARJUN KI DHANI', 'आदर्श छोटू'),
    ('ARJUN KI DHANI', 'अर्जुन की ढाणी'),
    ('ARJUN KI DHANI', 'बलदेव नगर'),
    ('ARJUN KI DHANI', 'भोमाणी कड़वासरों की ढाणी'),
    ('ARJUN KI DHANI', 'ईड छोटू'),
    ('ARJUN KI DHANI', 'खाणिया'),
    ('ARJUN KI DHANI', 'नवला सर'),
    ('ARJUN KI DHANI', 'पीराणी गोदारों की ढाणी'),
    ('ARJUN KI DHANI', 'उदय नगर'),
    ('ARJUN KI DHANI', 'विजय नगर'),

    -- Dholpaliya Nada
    ('DHOLPALIYA NADA', 'भीलों का गोल'),
    ('DHOLPALIYA NADA', 'धोलपालिया नाडा'),
    ('DHOLPALIYA NADA', 'लक्ष्मणनगर'),
    ('DHOLPALIYA NADA', 'सिद्धोनियों खोतों की ढाणी'),

    -- Khardi Beri
    ('KHARDI BERI', 'आधतरा'),
    ('KHARDI BERI', 'आडेल पांजी'),
    ('KHARDI BERI', 'आडेल शिवनगर @ पेमा की बेरी'),
    ('KHARDI BERI', 'बाटेरों की बेरी'),
    ('KHARDI BERI', 'जेठासर'),
    ('KHARDI BERI', 'कनाणी धाकों की ढाणी'),
    ('KHARDI BERI', 'खड़री बेरी'),
    ('KHARDI BERI', 'सादुल नगर'),

    -- Khariya Khurd
    ('KHARIYA KHURD', 'बनानियों की बेरी'),
    ('KHARIYA KHURD', 'भूरासर'),
    ('KHARIYA KHURD', 'चोरलिया नाडा'),
    ('KHARIYA KHURD', 'धन्नाणी कुम्हारों की ढाणी'),
    ('KHARIYA KHURD', 'गंगापुरा'),
    ('KHARIYA KHURD', 'खारिया खुर्द'),
    ('KHARIYA KHURD', 'खूबड़ माता मंदिर'),
    ('KHARIYA KHURD', 'किशनपुरा'),
    ('KHARIYA KHURD', 'मेघवालों की ढाणी'),
    ('KHARIYA KHURD', 'तारड़ नगर'),

    -- Meethi Beri
    ('MEETHI BERI', 'बाकाणी सारणों की ढाणी'),
    ('MEETHI BERI', 'डाऊकियों की ढाणी'),
    ('MEETHI BERI', 'कूकाणों की ढाणियां'),
    ('MEETHI BERI', 'लोहमरोड़ एंड नानोन की ढाणी'),
    ('MEETHI BERI', 'मीठी बेरी'),
    ('MEETHI BERI', 'राजनगर'),
    ('MEETHI BERI', 'सारणों एंड सेवारों की ढाणी'),
    ('MEETHI BERI', 'थोरियों की ढाणी'),
    ('MEETHI BERI', 'विश्वकर्मा नगर'),

    -- Sadecha
    ('SADECHA', 'बुधराणी हूडों की ढाणी'),
    ('SADECHA', 'करमी नगर'),
    ('SADECHA', 'पनाणी धातरवालों की ढाणी'),
    ('SADECHA', 'रूपनगर'),
    ('SADECHA', 'सडेचा'),
    ('SADECHA', 'शिवनगर सडेचा'),
    ('SADECHA', 'उत्तम नगर'),

    -- Chhotu
    ('छोटु', 'बेहणोनी सारणों की ढाणी'),
    ('छोटु', 'छोटु'),
    ('छोटु', 'गोगाजी का मंदिर छोटु'),
    ('छोटु', 'झरड़ासर'),
    ('छोटु', 'झूरड़ों की ढाणी'),
    ('छोटु', 'महादेव मंदिर'),
    ('छोटु', 'पोखरासर'),
    ('छोटु', 'सांवलसर'),

    -- Malpura
    ('मालपुरा', 'हीरपुरा'),
    ('मालपुरा', 'हुकमाणी खोतों की ढाणी'),
    ('मालपुरा', 'मालपुरा'),
    ('मालपुरा', 'थोरियों का तला'),

    -- Mangle Ki Beri
    ('मंगले की बेरी', 'अम्बेडकर नगर'),
    ('मंगले की बेरी', 'धन्ने भील की ढाणी'),
    ('मंगले की बेरी', 'गाढ़ेर मघवालों की ढाणी'),
    ('मंगले की बेरी', 'खुमोणी बेनीवालों की ढाणी'),
    ('मंगले की बेरी', 'मंगले की बेरी'),
    ('मंगले की बेरी', 'नया कुआ'),
    ('मंगले की बेरी', 'राडों एंड कुम्हारों की ढाणी'),
    ('मंगले की बेरी', 'रामनगर'),
    ('मंगले की बेरी', 'तेजासर'),
    ('मंगले की बेरी', 'वागोणी धातरवालों की ढाणी'),
    ('मंगले की बेरी', 'वांकलसर'),

    -- Nimbalkot
    ('निम्बलकोट', 'भेराराम मूंढ नगर'),
    ('निम्बलकोट', 'डोलोणी सियागों का तला'),
    ('निम्बलकोट', 'लाखोणी गोदारों की ढाणी'),
    ('निम्बलकोट', 'लाखोणी मेघवालों की ढाणी'),
    ('निम्बलकोट', 'नीम्बल कोट'),
    ('निम्बलकोट', 'नीम्बल नाडी'),
    ('निम्बलकोट', 'सियागों की ढाणी चक नं. 1'),

    -- Nokhra
    ('नोखड़ा', 'आदर्श नोखड़ा'),
    ('नोखड़ा', 'भोमाणी मेघवालों की ढाणी'),
    ('नोखड़ा', 'गुरुओं का तला'),
    ('नोखड़ा', 'हीरा नगर'),
    ('नोखड़ा', 'जगराम की ढाणी'),
    ('नोखड़ा', 'एन.टी. नगर'),
    ('नोखड़ा', 'नेहरों का तला'),
    ('नोखड़ा', 'नोखड़ा'),
    ('नोखड़ा', 'सालगासर')
)
insert into public.villages (name, district, state, block_id, gram_panchayat_id)
select
    vs.village_name,
    'Barmer',
    'Rajasthan',
    b.id,
    gp.id
from village_seed vs
join public.gram_panchayats gp on gp.name = vs.gp_name
join public.blocks b on b.id = gp.block_id
where b.name = 'Aadel'
on conflict (name, district, state) do update set
    block_id = excluded.block_id,
    gram_panchayat_id = excluded.gram_panchayat_id,
    is_active = true;

comment on table public.districts is 'V1 location scope: entire current Barmer district, Rajasthan.';
comment on table public.blocks is 'V1 scope: current Barmer development blocks.';
comment on table public.gram_panchayats is 'Aadel Gram Panchayat selection list seeded from the current available Aadel GP listing.';
comment on column public.villages.block_id is 'Block mapping for the location hierarchy.';
comment on column public.villages.gram_panchayat_id is 'Gram Panchayat mapping for verified village seed rows.';
comment on column public.villages.lgd_code is 'LGD village code where available; not guessed.';
