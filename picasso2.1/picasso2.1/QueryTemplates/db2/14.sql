select
	l_extendedprice * (1 - l_discount)
from
	lineitem,
	part
where
	l_partkey = p_partkey
	and l_extendedprice :varies
	and p_retailprice :varies

