-- KEYS: Array of seat keys (e.g., show:1:seat:A1, show:1:seat:A2)
-- ARGV[1]: userId
-- ARGV[2]: TTL in seconds

local userId = ARGV[1]
local ttl = tonumber(ARGV[2])

-- 1. Check karo ki koi bhi seat pehle se locked toh nahi hai
for i, key in ipairs(KEYS) do
    if redis.call('EXISTS', key) == 1 then
        return 0 -- Ek bhi seat mili toh fail
    end
end

-- 2. Agar saari available hain, toh sabhi ko atomically lock karo with TTL
for i, key in ipairs(KEYS) do
    redis.call('SET', key, userId, 'EX', ttl)
end

return 1 -- All-or-nothing lock successful