-- Run this once in the Supabase SQL Editor (Dashboard -> SQL Editor -> New query).
-- Migration-first: the app runs with ddl-auto=validate, so this file is the source of truth.

create table if not exists public.inventory (
    product_id text primary key,
    name       text    not null,
    stock      integer not null check (stock >= 0)
);

create table if not exists public.orders (
    order_id   uuid primary key default gen_random_uuid(),
    -- deliberately no foreign key: rejected attempts for a mistyped product id
    -- still get recorded here, so the table is a complete audit trail
    product_id text        not null,
    quantity   integer     not null,
    status     text        not null check (status in ('CONFIRMED', 'REJECTED')),
    reason     text,
    created_at timestamptz not null default now()
);

create index if not exists orders_created_at_idx on public.orders (created_at desc);

-- Seed data required by the spec.
insert into public.inventory (product_id, name, stock)
values ('P100', 'Wireless Mouse', 25),
       ('P200', 'Mechanical Keyboard', 10),
       ('P300', 'USB-C Hub', 0)
on conflict (product_id) do update
    set name  = excluded.name,
        stock = excluded.stock;
