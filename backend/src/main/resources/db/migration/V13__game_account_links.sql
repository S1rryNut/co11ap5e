INSERT INTO game_accounts(platform, handle, url, description, sort_order)
SELECT 'Steam', 'Co11ap5e', 'https://steamcommunity.com/id/Co11ap5e/', 'Steam 个人主页与游戏库。', 0
WHERE NOT EXISTS (
    SELECT 1 FROM game_accounts WHERE url = 'https://steamcommunity.com/id/Co11ap5e/'
);

INSERT INTO game_accounts(platform, handle, url, description, sort_order)
SELECT '哔哩哔哩', 'Co11ap5e', 'https://space.bilibili.com/8704946', 'B 站个人主页与动态。', 1
WHERE NOT EXISTS (
    SELECT 1 FROM game_accounts WHERE url = 'https://space.bilibili.com/8704946'
);
