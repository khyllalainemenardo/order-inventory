update public.orders o set status = 'CONFIRMED', reason = null
from public.channel_orders c
where c.shop_order_id = o.order_id
  and c.resolution = 'ACCEPTED' and not c.cancel_received
  and o.status = 'CANCELLED';

update public.orders o set status = 'CANCELLED'
from public.channel_orders c
where c.shop_order_id = o.order_id
  and c.cancel_received
  and o.status = 'CONFIRMED';

update public.inventory set stock = stock + 98 where product_id = 'P100';
update public.inventory set stock = stock + 55 where product_id = 'P200';
update public.inventory set stock = stock + 80 where product_id = 'P300';
update public.inventory set stock = stock + 40 where product_id = 'P400';

select product_id, stock from public.inventory order by product_id;
