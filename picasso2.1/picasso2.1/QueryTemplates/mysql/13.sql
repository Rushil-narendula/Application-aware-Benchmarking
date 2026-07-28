select
			c_custkey,
			count(o_orderkey) 
		from
			customer left outer join orders on
				c_custkey = o_custkey
				and o_totalprice :varies
		group by
			c_custkey
	