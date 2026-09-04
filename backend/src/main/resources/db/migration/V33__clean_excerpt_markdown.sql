UPDATE articles
SET excerpt = REPLACE(REPLACE(REPLACE(REPLACE(excerpt, '**', ''), '##', ''), '`', ''), '*', ''),
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'PUBLISHED';
