SELECT
    k.keyword,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year
FROM title t
JOIN movie_keyword mk
    ON t.id = mk.movie_id
JOIN keyword k
    ON mk.keyword_id = k.id
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN movie_info_idx mi_idx
    ON t.id = mi_idx.movie_id
JOIN info_type it
    ON mi_idx.info_type_id = it.id
JOIN kind_type kt
    ON t.kind_id = kt.id
WHERE k.keyword = 'monkey'
  AND it.info = 'rating'
  AND kt.kind = 'tv series'
  AND cn.country_code = '[us]'
GROUP BY
    k.keyword
ORDER BY
    movie_count DESC;
