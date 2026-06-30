SELECT
    lt.link AS relationship_type,
    COUNT(DISTINCT t1.id) AS movie_count,
    AVG(t1.production_year) AS avg_release_year
FROM title t1
JOIN movie_link ml ON t1.id = ml.movie_id
JOIN link_type lt ON ml.link_type_id = lt.id
JOIN title t2 ON ml.linked_movie_id = t2.id
JOIN movie_info mi ON t1.id = mi.movie_id
JOIN info_type it ON mi.info_type_id = it.id
JOIN movie_keyword mk ON t1.id = mk.movie_id
JOIN keyword k ON mk.keyword_id = k.id
JOIN movie_companies mc ON t1.id = mc.movie_id
WHERE t1.production_year BETWEEN 1945 AND 1995
  AND lt.link = 'spin off from'
  AND k.keyword = 'cocaine-addict'
  AND it.info = 'locations'
GROUP BY lt.link
ORDER BY movie_count DESC;
