SELECT
    cn.name AS company_name,
    rt.role AS role_type,
    k.keyword,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year,
    MAX(mi_idx.info) AS highest_rank_info
FROM title t
JOIN cast_info ci ON t.id = ci.movie_id
JOIN role_type rt ON ci.role_id = rt.id
JOIN movie_companies mc ON t.id = mc.movie_id
JOIN company_name cn ON mc.company_id = cn.id
JOIN company_type ct ON mc.company_type_id = ct.id
JOIN movie_info_idx mi_idx ON t.id = mi_idx.movie_id
JOIN movie_keyword mk ON t.id = mk.movie_id
JOIN keyword k ON mk.keyword_id = k.id
JOIN movie_link ml ON t.id = ml.movie_id
JOIN link_type lt ON ml.link_type_id = lt.id
JOIN complete_cast cc ON t.id = cc.movie_id
JOIN comp_cast_type cct ON cc.subject_id = cct.id
JOIN kind_type kt ON t.kind_id = kt.id
WHERE rt.role = 'writer'
  AND k.keyword = 'uruguay'
  AND lt.link = 'featured in'
  AND kt.kind = 'movie'
  AND ct.kind = 'production companies'
  AND cct.kind = 'complete+verified'
  AND t.id IN (
        SELECT mi.movie_id
        FROM movie_info mi
        WHERE mi.info LIKE '%Poland%'
    )
GROUP BY cn.name, rt.role, k.keyword
ORDER BY movie_count DESC;
