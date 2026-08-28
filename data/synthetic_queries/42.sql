SELECT
    kt.kind AS movie_type,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year
FROM title t
JOIN movie_info_idx mi_idx
    ON t.id = mi_idx.movie_id
JOIN info_type it
    ON mi_idx.info_type_id = it.id
JOIN movie_link ml
    ON t.id = ml.movie_id
JOIN link_type lt
    ON ml.link_type_id = lt.id
JOIN movie_keyword mk
    ON t.id = mk.movie_id
JOIN keyword k
    ON mk.keyword_id = k.id
JOIN kind_type kt
    ON t.kind_id = kt.id
WHERE it.info = 'top 250 rank'
  AND lt.link = 'remake of'
  AND k.keyword = 'breakfast'
GROUP BY
    kt.kind
ORDER BY
    movie_count DESC;
