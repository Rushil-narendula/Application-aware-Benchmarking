SELECT
    cn.name AS company_name,
    rt.role AS role_type,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year
FROM title t
JOIN cast_info ci
    ON t.id = ci.movie_id
JOIN role_type rt
    ON ci.role_id = rt.id
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN movie_link ml
    ON t.id = ml.movie_id
JOIN link_type lt
    ON ml.link_type_id = lt.id
JOIN movie_keyword mk
    ON t.id = mk.movie_id
JOIN keyword k
    ON mk.keyword_id = k.id
JOIN complete_cast cc
    ON t.id = cc.movie_id
JOIN comp_cast_type cct
    ON cc.subject_id = cct.id
WHERE rt.role = 'producer'
  AND lt.link = 'similar to'
  AND k.keyword = 'outbreak'
  AND cn.country_code = '[fr]'
GROUP BY
    cn.name,
    rt.role
ORDER BY
    movie_count DESC;
