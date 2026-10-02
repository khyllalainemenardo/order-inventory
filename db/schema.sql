drop table if exists public.channel_orders;
drop table if exists public.channel_events;
drop table if exists public.channel_cursor;
drop table if exists public.supplier_orders;
drop table if exists public.notifications;
drop table if exists public.order_items;
drop table if exists public.orders;
drop table if exists public.inventory;

create table public.inventory (
    product_id text primary key,
    name       text    not null,
    stock      integer not null check (stock >= 0)
);

create table public.orders (
    order_id   uuid primary key default gen_random_uuid(),
    status     text        not null check (status in ('CONFIRMED', 'REJECTED', 'CANCELLED', 'BACKORDERED')),
    reason     text,
    created_at timestamptz not null default now()
);

create index orders_created_at_idx on public.orders (created_at desc);

create table public.order_items (
    id         bigint generated always as identity primary key,
    order_id   uuid    not null references public.orders (order_id) on delete cascade,
    product_id text    not null references public.inventory (product_id),
    quantity   integer not null check (quantity > 0)
);

create index order_items_order_id_idx on public.order_items (order_id);

create table public.notifications (
    notification_id bigint generated always as identity primary key,
    type            text        not null check (type in ('ORDER_CONFIRMED', 'ORDER_REJECTED',
                                                         'ORDER_CANCELLED', 'LOW_STOCK',
                                                         'REORDER_PLACED', 'REORDER_DELIVERED',
                                                         'REORDER_STOPPED')),
    message         text        not null,
    created_at      timestamptz not null default now()
);

create index notifications_created_at_idx on public.notifications (created_at desc);

create table public.supplier_orders (
    id         bigint generated always as identity primary key,
    product_id text        not null references public.inventory (product_id),
    buyer_ref  text unique,
    request_id text        not null unique,
    po_number  text,
    cases      integer     not null check (cases between 1 and 99),
    units      integer     not null check (units > 0),
    status     text        not null check (status in ('PENDING', 'PLACED', 'PICKING', 'SHIPPED',
                                                     'DELIVERED', 'FAILED', 'NEEDS_REVIEW')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index supplier_orders_status_idx on public.supplier_orders (status);

alter table public.inventory     enable row level security;
alter table public.orders        enable row level security;
alter table public.order_items   enable row level security;
alter table public.notifications enable row level security;
alter table public.supplier_orders enable row level security;

insert into public.inventory (product_id, name, stock)
values ('P100', 'Wireless Mouse', 25),
       ('P200', 'Mechanical Keyboard', 10),
       ('P300', 'USB-C Hub', 0),
       ('P400', 'USB-C Cable 1m', 12),
       ('P500', 'Headset with Mic', 8);

