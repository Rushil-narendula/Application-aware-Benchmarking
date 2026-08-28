SELECT
    cn.name AS company_name,
    k.keyword,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year,
    MIN(mi_idx.info) AS lowest_rank_info,
    MAX(mi_idx.info) AS highest_rank_info
FROM title t
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN movie_info_idx mi_idx
    ON t.id = mi_idx.movie_id
JOIN info_type it
    ON mi_idx.info_type_id = it.id
JOIN movie_keyword mk
    ON t.id = mk.movie_id
JOIN keyword k
    ON mk.keyword_id = k.id
JOIN movie_link ml
    ON t.id = ml.movie_id
JOIN link_type lt
    ON ml.link_type_id = lt.id
JOIN complete_cast cc
    ON t.id = cc.movie_id
JOIN comp_cast_type cct
    ON cc.subject_id = cct.id
JOIN kind_type kt
    ON t.kind_id = kt.id
WHERE k.keyword = 'monkey'
  AND it.info = 'keywords'
  AND lt.link = 'features'
  AND kt.kind = 'movie'
GROUP BY
    cn.name,
    k.keyword
ORDER BY
    movie_count DESC;
