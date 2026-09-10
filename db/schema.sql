

create table if not exists public.inventory (
    product_id text primary key,
    name       text    not null,
    stock      integer not null check (stock >= 0)
);

create table if not exists public.orders (
    order_id   uuid primary key default gen_random_uuid(),

    product_id text        not null,
    quantity   integer     not null,
    status     text        not null check (status in ('CONFIRMED', 'REJECTED')),
    reason     text,
    created_at timestamptz not null default now()
);

create index if not exists orders_created_at_idx on public.orders (created_at desc);


insert into public.inventory (product_id, name, stock)
values ('P100', 'Wireless Mouse', 25),
       ('P200', 'Mechanical Keyboard', 10),
       ('P300', 'USB-C Hub', 0)
on conflict (product_id) do update
    set name  = excluded.name,
        stock = excluded.stock;
