SELECT
    rt.role,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year,
    MAX(mi_idx.info) AS highest_rank_info
FROM title t
JOIN cast_info ci
    ON t.id = ci.movie_id
JOIN role_type rt
    ON ci.role_id = rt.id
JOIN movie_link ml
    ON t.id = ml.movie_id
JOIN link_type lt
    ON ml.link_type_id = lt.id
JOIN movie_keyword mk
    ON t.id = mk.movie_id
JOIN keyword k
    ON mk.keyword_id = k.id
JOIN complete_cast cc
    ON t.id = cc.movie_id
JOIN comp_cast_type cct
    ON cc.subject_id = cct.id
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN movie_info_idx mi_idx
    ON t.id = mi_idx.movie_id
JOIN kind_type kt
    ON t.kind_id = kt.id
WHERE rt.role = 'composer'
  AND lt.link = 'spin off from'
  AND kt.kind = 'tv mini series'
  AND t.id IN (
        SELECT at.movie_id
        FROM aka_title at
        JOIN movie_info mi
            ON at.movie_id = mi.movie_id
        JOIN info_type it
            ON mi.info_type_id = it.id
        WHERE it.info = 'quotes'
    )
  AND t.id IN (
        SELECT mc2.movie_id
        FROM movie_companies mc2
        JOIN company_type ct2
            ON mc2.company_type_id = ct2.id
        WHERE ct2.kind = 'special effects companies'
    )
GROUP BY rt.role
ORDER BY movie_count DESC;
