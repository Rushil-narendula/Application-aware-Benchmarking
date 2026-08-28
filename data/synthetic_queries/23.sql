SELECT
    cn.country_code,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_production_year
FROM title t
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN cast_info ci
    ON t.id = ci.movie_id
JOIN role_type rt
    ON ci.role_id = rt.id
JOIN complete_cast cc
    ON t.id = cc.movie_id
JOIN comp_cast_type cct
    ON cc.subject_id = cct.id
JOIN kind_type kt
    ON t.kind_id = kt.id
WHERE rt.role = 'actor'
  AND kt.kind = 'movie'
  AND cn.country_code = '[gb]'
GROUP BY
    cn.country_code
ORDER BY
    movie_count DESC;
