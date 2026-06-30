SELECT
    k.keyword,
    COUNT(DISTINCT t.id) AS movie_count,
    AVG(t.production_year) AS avg_release_year,
    MAX(mc.id) AS latest_company_record
FROM title t
JOIN movie_keyword mk
    ON t.id = mk.movie_id
JOIN keyword k
    ON mk.keyword_id = k.id
JOIN movie_companies mc
    ON t.id = mc.movie_id
JOIN company_name cn
    ON mc.company_id = cn.id
JOIN complete_cast cc
    ON t.id = cc.movie_id
JOIN comp_cast_type cct
    ON cc.subject_id = cct.id
JOIN kind_type kt
    ON t.kind_id = kt.id
WHERE k.keyword = 'laser-sword'
  AND cn.country_code = '[jp]'
  AND kt.kind = 'tv series'
GROUP BY
    k.keyword
ORDER BY
    movie_count DESC;
