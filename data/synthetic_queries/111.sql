SELECT
    cn.name AS company_name,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year,
    MIN(t.production_year) AS earliest_release_year,
    MAX(t.production_year) AS latest_release_year,
    SUM(ci.nr_order) AS total_cast_order,
    MAX(mi_idx.info) AS highest_rank_info
FROM title t
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN cast_info ci
    ON t.id = ci.movie_id
JOIN movie_info_idx mi_idx
    ON t.id = mi_idx.movie_id
JOIN info_type it
    ON mi_idx.info_type_id = it.id
JOIN movie_link ml
    ON t.id = ml.movie_id
JOIN link_type lt
    ON ml.link_type_id = lt.id
WHERE it.info = 'budget'
  AND lt.link = 'featured in'
  AND cn.country_code = '[us]'
GROUP BY cn.name
ORDER BY movie_count DESC;
