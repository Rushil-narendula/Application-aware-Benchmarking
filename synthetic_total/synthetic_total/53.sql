SELECT
    ct.kind,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(CAST(mi_idx.info AS FLOAT)) AS avg_rating,
    COUNT(mi.info) AS info_entries
FROM title t
JOIN movie_companies mc ON t.id = mc.movie_id
JOIN company_type ct ON mc.company_type_id = ct.id
JOIN movie_info mi ON t.id = mi.movie_id
JOIN movie_info_idx mi_idx ON t.id = mi_idx.movie_id
JOIN info_type it ON mi.info_type_id = it.id
WHERE ct.kind = 'special effects companies'
  AND it.info = 'trivia'
  AND mi.info LIKE '%brain tumor%'
GROUP BY ct.kind;
