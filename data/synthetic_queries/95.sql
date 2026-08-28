SELECT
    rt.role AS role_type,
    cn.name AS company_name,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year,
    MAX(mi_idx.info) AS highest_rank_info
FROM title t
JOIN cast_info ci
    ON t.id = ci.movie_id
JOIN role_type rt
    ON ci.role_id = rt.id
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN movie_info_idx mi_idx
    ON t.id = mi_idx.movie_id
JOIN info_type it
    ON mi_idx.info_type_id = it.id
JOIN complete_cast cc
    ON t.id = cc.movie_id
JOIN comp_cast_type cct
    ON cc.subject_id = cct.id
JOIN movie_link ml
    ON t.id = ml.movie_id
JOIN link_type lt
    ON ml.link_type_id = lt.id
JOIN kind_type kt
    ON t.kind_id = kt.id
WHERE rt.role = 'actor'
  AND it.info = 'rating'
  AND lt.link = 'references'
  AND kt.kind = 'tv series'
GROUP BY
    rt.role,
    cn.name
ORDER BY
    movie_count DESC;
