alter table public.orders drop constraint if exists orders_status_check;
alter table public.orders add constraint orders_status_check
    check (status in ('CONFIRMED', 'REJECTED', 'CANCELLED', 'BACKORDERED'));

create table if not exists public.channel_cursor (
    id       integer primary key,
    last_seq bigint  not null
);

create table if not exists public.channel_events (
    event_id     text primary key,
    seq          bigint      not null,
    type         text,
    order_id     text,
    processed_at timestamptz not null default now()
);

create table if not exists public.channel_orders (
    tiangge_order_id text primary key,
    shop_order_id    uuid,
    decision         text,
    reason           text,
    decision_sent    boolean     not null default false,
    resolution       text,
    resolution_sent  boolean     not null default false,
    cancel_received  boolean     not null default false,
    cancel_confirmed boolean     not null default false,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);

create unique index if not exists channel_orders_shop_order_idx on public.channel_orders (shop_order_id);

alter table public.channel_cursor enable row level security;
alter table public.channel_events enable row level security;
alter table public.channel_orders enable row level security;
