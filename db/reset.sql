
delete from public.orders;

update public.inventory set stock = 25 where product_id = 'P100';
update public.inventory set stock = 10 where product_id = 'P200';
update public.inventory set stock = 0  where product_id = 'P300';
