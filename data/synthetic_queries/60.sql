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
JOIN complete_cast cc
    ON t.id = cc.movie_id
JOIN comp_cast_type cct
    ON cc.subject_id = cct.id
JOIN kind_type kt
    ON t.kind_id = kt.id
JOIN movie_link ml
    ON t.id = ml.movie_id
WHERE kt.kind = 'movie'
  AND cn.country_code = '[gb]'
  AND t.id IN (
        SELECT mi.movie_id
        FROM movie_info mi
        JOIN info_type it
            ON mi.info_type_id = it.id
        WHERE it.info = 'top 250 rank'
    )
GROUP BY k.keyword
ORDER BY movie_count DESC;
