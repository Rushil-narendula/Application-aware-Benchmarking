SELECT
    kt.kind AS movie_type,
    COUNT(DISTINCT t.id) AS total_movies,
    AVG(t.production_year) AS avg_release_year,
    MAX(mi_idx.info) AS max_rank_info
FROM title t
JOIN movie_info_idx mi_idx
    ON t.id = mi_idx.movie_id
JOIN info_type it
    ON mi_idx.info_type_id = it.id
JOIN kind_type kt
    ON t.kind_id = kt.id
JOIN movie_keyword mk
    ON t.id = mk.movie_id
WHERE it.info = 'top 250 rank'
  AND kt.kind = 'movie'
  AND t.production_year BETWEEN 1950 AND 2019
GROUP BY
    kt.kind
ORDER BY
    total_movies DESC;
