-- PulseVote Supabase schema and RLS policies

create table if not exists profiles (
  id uuid primary key references auth.users(id),
  branch text,
  year int,
  goal text,
  role text default 'user',
  created_at timestamp with time zone default now()
);

create table if not exists questions (
  id uuid primary key default gen_random_uuid(),
  text text not null,
  is_active boolean default false,
  created_at timestamp with time zone default now()
);

create table if not exists votes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references profiles(id),
  question_id uuid not null references questions(id),
  vote boolean not null,
  created_at timestamp with time zone default now(),
  unique(user_id, question_id)
);

alter table profiles enable row level security;
create policy "profiles self select" on profiles
  for select using (auth.uid() = id);
create policy "profiles self update" on profiles
  for update using (auth.uid() = id);
create policy "profiles self insert" on profiles
  for insert with check (auth.uid() = id);

alter table questions enable row level security;
create policy "questions public read" on questions
  for select using (true);

alter table votes enable row level security;
create policy "votes own select" on votes
  for select using (auth.uid() = user_id);
create policy "votes own insert" on votes
  for insert with check (auth.uid() = user_id);
