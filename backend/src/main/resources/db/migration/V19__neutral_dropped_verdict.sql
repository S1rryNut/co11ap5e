UPDATE game_entries
SET verdict = '待补充个人评价。',
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'DROPPED'
  AND verdict = '游玩时长不足 20 小时，暂归入已弃坑。';
