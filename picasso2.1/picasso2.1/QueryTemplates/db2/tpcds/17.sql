select
     w_warehouse_name
    ,i_item_id
    ,sum(case when d_date < '1999-02-16'
	      then inv_quantity_on_hand 
              else 0 
	      end) as inv_before
    ,sum(case when d_date  >= '1999-02-16' 
              then inv_quantity_on_hand 
              else 0 
              end) as inv_after
   from
     inventory
    ,warehouse
    ,item
    ,date_dim
   where
    i_current_price :varies
    and i_item_sk          = inv_item_sk
    and inv_warehouse_sk   = w_warehouse_sk
    and inv_date_sk    = d_date_sk
    and d_date between '1999-01-16' 
                   and '1999-03-16' 
    and inv_quantity_on_hand :varies
   group by
     w_warehouse_name, i_item_id;
