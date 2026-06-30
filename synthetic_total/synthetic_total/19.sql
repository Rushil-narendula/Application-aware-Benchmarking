SELECT
    cn.name AS company_name,
    COUNT(DISTINCT t.id) AS movie_count
FROM title t
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN cast_info ci
    ON t.id = ci.movie_id
JOIN role_type rt
    ON ci.role_id = rt.id
WHERE rt.role = 'producer'
  AND cn.country_code = '[fr]'
  AND t.production_year BETWEEN 1888 AND 1970
GROUP BY
    cn.name
ORDER BY
    movie_count DESC;
