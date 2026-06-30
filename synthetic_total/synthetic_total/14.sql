SELECT
    kt.kind AS movie_type,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year
FROM title t
JOIN movie_info mi
    ON t.id = mi.movie_id
JOIN info_type it
    ON mi.info_type_id = it.id
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN kind_type kt
    ON t.kind_id = kt.id
WHERE it.info = 'rating'
  AND kt.kind = 'tv series'
  AND t.production_year BETWEEN 1980 AND 2019
GROUP BY
    kt.kind
ORDER BY
    movie_count DESC;
