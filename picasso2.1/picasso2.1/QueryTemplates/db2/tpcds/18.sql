select i_item_id,
        ca_country,
        ca_state, 
        ca_county,
        avg(cs_quantity) as agg1,
        avg(cs_list_price) as agg2,
        avg(cs_coupon_amt) as agg3,
        avg(cs_sales_price) as agg4,
        avg(cs_net_profit) as agg5,
        avg(c_birth_year) as agg6,
        avg(cd1.cd_dep_count) as agg7
 from catalog_sales, customer_demographics cd1, customer_demographics cd2, customer, customer_address, date_dim, item
 where cs_sold_date_sk = d_date_sk and
       cs_item_sk = i_item_sk and
       cs_bill_cdemo_sk = cd1.cd_demo_sk and
       cs_bill_customer_sk = c_customer_sk and
       cd1.cd_gender = 'F' and 
       cd1.cd_education_status = 'Unknown' and
       c_current_cdemo_sk = cd2.cd_demo_sk and
       c_current_addr_sk = ca_address_sk and
       c_birth_month in (3,11,9,5,8,10) and
       d_year = 2000 and
       ca_state in ('NC','AK','PA','AK','CA','MA','WV') and
       cs_list_price :varies and
       i_current_price :varies       
 group by i_item_id, ca_country, ca_state, ca_county
 order by ca_country, ca_state, ca_county;
