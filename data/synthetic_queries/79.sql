SELECT
    n.name,
    COUNT(DISTINCT t.id) AS movie_count,
    COUNT(DISTINCT k.id) AS keyword_count
FROM title t
JOIN cast_info ci ON t.id = ci.movie_id
JOIN name n ON ci.person_id = n.id
JOIN aka_name an ON n.id = an.person_id
JOIN movie_keyword mk ON t.id = mk.movie_id
JOIN keyword k ON mk.keyword_id = k.id
JOIN movie_companies mc ON t.id = mc.movie_id
JOIN company_name cn ON mc.company_id = cn.id
WHERE k.keyword = 'monkey'
  AND cn.country_code = '[us]'
  AND an.name LIKE '%Big%'
GROUP BY n.name;
