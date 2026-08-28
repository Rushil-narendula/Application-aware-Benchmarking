SELECT
    cn.name AS production_company,
    COUNT(DISTINCT t.id) AS movie_count
FROM title t
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN company_type ct
    ON mc.company_type_id = ct.id
JOIN kind_type kt
    ON t.kind_id = kt.id
WHERE ct.kind = 'distributors'
  AND kt.kind = 'video movie'
  AND cn.country_code = '[br]'
  AND t.production_year BETWEEN 1888 AND 2012
GROUP BY
    cn.name
ORDER BY
    movie_count DESC;
