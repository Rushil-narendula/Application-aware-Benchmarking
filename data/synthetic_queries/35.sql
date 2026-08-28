SELECT
    cct.kind AS cast_category,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year
FROM title t
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN complete_cast cc
    ON t.id = cc.movie_id
JOIN comp_cast_type cct
    ON cc.subject_id = cct.id
JOIN keyword k
    ON k.id IN (
        SELECT mk.keyword_id
        FROM movie_keyword mk
        WHERE mk.movie_id = t.id
    )
WHERE cn.country_code = '[de]'
  AND k.keyword = 'girl-scout'
GROUP BY
    cct.kind
ORDER BY
    movie_count DESC;
