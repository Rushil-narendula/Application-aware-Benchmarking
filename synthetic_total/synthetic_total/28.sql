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
WHERE cn.country_code = '[de]'
  AND k.keyword = 'public-computer'
  AND t.id IN (
        SELECT mi.movie_id
        FROM movie_info mi
        JOIN info_type it
            ON mi.info_type_id = it.id
        WHERE it.info = 'soundtrack'
    )
GROUP BY k.keyword
ORDER BY movie_count DESC;
