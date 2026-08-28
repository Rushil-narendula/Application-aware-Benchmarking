SELECT
    cn.name AS company_name,
    kt.kind AS movie_type,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year
FROM title t
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN cast_info ci
    ON t.id = ci.movie_id
JOIN role_type rt
    ON ci.role_id = rt.id
JOIN movie_info_idx mi_idx
    ON t.id = mi_idx.movie_id
JOIN info_type it
    ON mi_idx.info_type_id = it.id
JOIN movie_link ml
    ON t.id = ml.movie_id
JOIN kind_type kt
    ON t.kind_id = kt.id
WHERE rt.role = 'director'
  AND it.info = 'top 250 rank'
  AND kt.kind = 'movie'
  AND cn.country_code = '[gb]'
GROUP BY
    cn.name,
    kt.kind
ORDER BY
    movie_count DESC;
