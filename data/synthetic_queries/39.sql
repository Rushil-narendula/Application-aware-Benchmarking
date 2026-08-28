SELECT
    cn.name AS company_name,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year,
    MAX(mi_idx.info) AS highest_rank_info
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
JOIN keyword k
    ON k.id IN (
        SELECT mk.keyword_id
        FROM movie_keyword mk
        WHERE mk.movie_id = t.id
    )
WHERE rt.role = 'director'
  AND it.info = 'top 250 rank'
  AND cn.country_code = '[gb]'
GROUP BY
    cn.name
ORDER BY
    movie_count DESC;
