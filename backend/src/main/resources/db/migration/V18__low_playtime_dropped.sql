UPDATE game_entries
SET status = 'DROPPED',
    verdict = CASE
        WHEN verdict = '来自 Steam 游戏库，后续补充个人评价。' THEN '游玩时长不足 20 小时，暂归入已弃坑。'
        ELSE verdict
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE slug LIKE 'steam-%'
  AND hours_played < 20
  AND status = 'LIBRARY';
