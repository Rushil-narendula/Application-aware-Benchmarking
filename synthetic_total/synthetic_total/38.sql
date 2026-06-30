SELECT
    rt.role,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year,
    MIN(mc.id) AS earliest_company_record
FROM title t
JOIN cast_info ci
    ON t.id = ci.movie_id
JOIN role_type rt
    ON ci.role_id = rt.id
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN kind_type kt
    ON t.kind_id = kt.id
JOIN complete_cast cc
    ON t.id = cc.movie_id
JOIN comp_cast_type cct
    ON cc.subject_id = cct.id
WHERE rt.role = 'producer'
  AND kt.kind = 'tv series'
  AND cn.country_code = '[jp]'
GROUP BY
    rt.role
ORDER BY
    movie_count DESC;
