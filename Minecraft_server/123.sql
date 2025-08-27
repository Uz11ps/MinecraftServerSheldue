-- Проверка и создание таблицы quests если она не существует
CREATE TABLE IF NOT EXISTS quests (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    reward_cash DOUBLE PRECISION NOT NULL DEFAULT 0,
    reward_card_money DOUBLE PRECISION NOT NULL DEFAULT 0,
    is_repeatable BOOLEAN NOT NULL DEFAULT false,
    quest_type VARCHAR(50) NOT NULL,
    target_amount INTEGER NOT NULL DEFAULT 1,
    target_item VARCHAR(100) NOT NULL
);

-- Проверка и создание таблицы quest_objectives если она не существует
CREATE TABLE IF NOT EXISTS quest_objectives (
    id BIGSERIAL PRIMARY KEY,
    quest_id BIGINT REFERENCES quests(id),
    description VARCHAR(1000) NOT NULL,
    target_amount INTEGER NOT NULL DEFAULT 1,
    objective_type VARCHAR(50) NOT NULL,
    target_item VARCHAR(100) NOT NULL
);

-- Очистка существующих данных (опционально)
-- DELETE FROM quest_objectives;
-- DELETE FROM quests;

-- Добавление базовых квестов
INSERT INTO quests (title, description, reward_cash, reward_card_money, is_repeatable, quest_type, target_amount, target_item)
VALUES
    ('Камнетёс', 'Добудьте 10 камней', 50.0, 0.0, true, 'BREAK_BLOCK', 10, 'STONE'),
    ('Лесоруб', 'Соберите 20 древесины для строительства', 75.0, 0.0, true, 'BREAK_BLOCK', 20, 'OAK_LOG'),
    ('Охотник на зомби', 'Убейте 5 зомби', 100.0, 50.0, true, 'KILL_ENTITY', 5, 'ZOMBIE');