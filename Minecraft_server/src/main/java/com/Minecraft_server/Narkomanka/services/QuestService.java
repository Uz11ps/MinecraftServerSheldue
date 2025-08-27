package com.Minecraft_server.Narkomanka.services;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerQuestProgress;
import com.Minecraft_server.Narkomanka.database.QuestData;
import com.Minecraft_server.Narkomanka.database.QuestObjective;
import com.Minecraft_server.Narkomanka.database.QuestProgress;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class QuestService {
    private final Narkomanka plugin;

    // Cache for quests and progress
    private final List<QuestData> questCache = new ArrayList<>();
    private final Map<UUID, Map<Long, PlayerQuestProgress>> progressCache = new ConcurrentHashMap<>();

    public QuestService(Narkomanka plugin) {
        this.plugin = plugin;
    }

    /**
     * Получает все квесты из базы данных
     * @return Список всех квестов
     */
    public List<QuestData> getAllQuests() {
        // Если кэш уже содержит квесты и база недоступна, вернем кэшированные квесты
        if (!questCache.isEmpty() && !isDatabaseAccessible()) {
            plugin.getLogger().info("База данных недоступна, используем кэш. Квестов в кэше: " + questCache.size());
            return new ArrayList<>(questCache);
        }

        if (!isDatabaseAccessible()) {
            plugin.getLogger().warning("База данных недоступна, и кэш пуст!");
            return new ArrayList<>();
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                // Сначала пробуем через Hibernate API
                List<QuestData> quests = session.createQuery("FROM QuestData q ORDER BY q.id", QuestData.class).list();

                // Если результатов нет, попробуем SQL запрос напрямую
                if (quests.isEmpty()) {
                    plugin.getLogger().info("HQL запрос не вернул результатов, пробуем SQL запрос...");

                    try {
                        String sql = "SELECT * FROM quests ORDER BY id";
                        NativeQuery<QuestData> nativeQuery = session.createNativeQuery(sql, QuestData.class);
                        quests = nativeQuery.list();
                        plugin.getLogger().info("SQL запрос вернул " + quests.size() + " квестов");
                    } catch (Exception e) {
                        plugin.getLogger().severe("Ошибка выполнения SQL запроса: " + e.getMessage());
                    }
                }

                tx.commit();

                // Обновляем кэш
                questCache.clear();
                questCache.addAll(quests);

                plugin.getLogger().info("Загружено квестов из базы данных: " + quests.size());
                for (QuestData quest : quests) {
                    plugin.getLogger().info("  - Квест #" + quest.getId() + ": " + quest.getTitle());
                }

                return quests;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception rollbackEx) {
                        plugin.getLogger().severe("Ошибка при откате транзакции: " + rollbackEx.getMessage());
                    }
                }
                plugin.getLogger().severe("Ошибка при загрузке квестов: " + e.getMessage());
                e.printStackTrace();
                // Return cached quests if database fails
                return new ArrayList<>(questCache);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при загрузке квестов: " + e.getMessage());
            e.printStackTrace();
            // Return cached quests if database fails
            return new ArrayList<>(questCache);
        }
    }

    /**
     * Получает квест по ID
     * @param questId ID квеста
     * @return Объект квеста или null
     */
    public QuestData getQuest(Long questId) {
        // Сначала ищем в кэше
        for (QuestData quest : questCache) {
            if (quest.getId() != null && quest.getId().equals(questId)) {
                return quest;
            }
        }

        if (!isDatabaseAccessible()) {
            return null;
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                QuestData quest = session.get(QuestData.class, questId);

                // Если не нашли через Hibernate, попробуем SQL
                if (quest == null) {
                    try {
                        String sql = "SELECT * FROM quests WHERE id = :id";
                        NativeQuery<QuestData> nativeQuery = session.createNativeQuery(sql, QuestData.class);
                        nativeQuery.setParameter("id", questId);
                        quest = nativeQuery.uniqueResult();
                    } catch (Exception e) {
                        plugin.getLogger().severe("Ошибка выполнения SQL запроса для получения квеста: " + e.getMessage());
                    }
                }

                tx.commit();

                // Добавляем в кэш если нашли
                if (quest != null) {
                    boolean found = false;
                    for (int i = 0; i < questCache.size(); i++) {
                        if (questCache.get(i).getId() != null && questCache.get(i).getId().equals(questId)) {
                            questCache.set(i, quest);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        questCache.add(quest);
                    }
                }

                return quest;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception rollbackEx) {
                        plugin.getLogger().severe("Ошибка при откате транзакции: " + rollbackEx.getMessage());
                    }
                }
                plugin.getLogger().severe("Ошибка при получении квеста: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при получении квеста: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Сохраняет квест в базу данных
     * @param quest Объект квеста
     */
    public void saveQuest(QuestData quest) {
        plugin.getLogger().info("Сохранение квеста: " + quest.getTitle());

        // Сначала пробуем через Hibernate
        boolean hibernateSaveSuccess = saveQuestWithHibernate(quest);

        // Если не удалось сохранить через Hibernate, пробуем через SQL
        if (!hibernateSaveSuccess && isDatabaseAccessible() && plugin.getDatabaseManager().isUsingPostgreSQL()) {
            saveQuestWithSQL(quest);
        }

        // В любом случае обновляем кэш
        boolean updated = false;
        for (int i = 0; i < questCache.size(); i++) {
            if (questCache.get(i).getId() != null && questCache.get(i).getId().equals(quest.getId())) {
                questCache.set(i, quest);
                updated = true;
                break;
            }
        }
        if (!updated) {
            questCache.add(quest);
        }
    }

    /**
     * Сохраняет квест через Hibernate
     * @param quest Объект квеста
     * @return true если сохранение успешно
     */
    private boolean saveQuestWithHibernate(QuestData quest) {
        if (!isDatabaseAccessible()) {
            return false;
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction transaction = null;

            try {
                transaction = session.beginTransaction();

                // Используем persist для новых и merge для существующих объектов
                if (quest.getId() == null) {
                    plugin.getLogger().info("Использую persist для нового квеста");
                    session.persist(quest);
                } else {
                    plugin.getLogger().info("Использую merge для существующего квеста с ID: " + quest.getId());
                    session.merge(quest);
                }

                session.flush(); // Принудительная отправка изменений в базу
                transaction.commit();

                // Получим ID квеста после сохранения
                plugin.getLogger().info("Квест сохранен с ID: " + quest.getId());

                return true;
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    try {
                        transaction.rollback();
                    } catch (Exception rollbackEx) {
                        plugin.getLogger().severe("Ошибка при откате транзакции: " + rollbackEx.getMessage());
                    }
                }
                plugin.getLogger().severe("Ошибка при сохранении квеста через Hibernate: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при сохранении квеста через Hibernate: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Сохраняет квест через прямой SQL запрос
     * @param quest Объект квеста
     * @return true если сохранение успешно
     */
    private boolean saveQuestWithSQL(QuestData quest) {
        plugin.getLogger().info("Попытка сохранения квеста через SQL: " + quest.getTitle());

        if (!isDatabaseAccessible() || !plugin.getDatabaseManager().isUsingPostgreSQL()) {
            return false;
        }

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;

        try {
            connection = plugin.getDatabaseManager().getDataSource().getConnection();
            connection.setAutoCommit(false);

            String sql;
            if (quest.getId() == null) {
                // INSERT для нового квеста
                sql = "INSERT INTO quests (title, description, reward_cash, reward_card_money, is_repeatable, quest_type, target_amount, target_item) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

                stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, quest.getTitle());
                stmt.setString(2, quest.getDescription());
                stmt.setDouble(3, quest.getRewardCash());
                stmt.setDouble(4, quest.getRewardCardMoney());
                stmt.setBoolean(5, quest.isRepeatable());
                stmt.setString(6, quest.getQuestType());
                stmt.setInt(7, quest.getTargetAmount());
                stmt.setString(8, quest.getTargetItem());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    generatedKeys = stmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        Long generatedId = generatedKeys.getLong(1);
                        quest.setId(generatedId);
                        plugin.getLogger().info("Квест создан через SQL с ID: " + generatedId);
                    }
                }
            } else {
                // UPDATE для существующего квеста
                sql = "UPDATE quests SET title = ?, description = ?, reward_cash = ?, reward_card_money = ?, " +
                        "is_repeatable = ?, quest_type = ?, target_amount = ?, target_item = ? WHERE id = ?";

                stmt = connection.prepareStatement(sql);
                stmt.setString(1, quest.getTitle());
                stmt.setString(2, quest.getDescription());
                stmt.setDouble(3, quest.getRewardCash());
                stmt.setDouble(4, quest.getRewardCardMoney());
                stmt.setBoolean(5, quest.isRepeatable());
                stmt.setString(6, quest.getQuestType());
                stmt.setInt(7, quest.getTargetAmount());
                stmt.setString(8, quest.getTargetItem());
                stmt.setLong(9, quest.getId());

                int affectedRows = stmt.executeUpdate();
                plugin.getLogger().info("Обновлено строк через SQL: " + affectedRows);
            }

            connection.commit();
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при сохранении квеста через SQL: " + e.getMessage());
            e.printStackTrace();

            if (connection != null) {
                try {
                    connection.rollback();
                } catch (Exception rollbackEx) {
                    plugin.getLogger().severe("Ошибка при откате SQL транзакции: " + rollbackEx.getMessage());
                }
            }
            return false;
        } finally {
            try {
                if (generatedKeys != null) generatedKeys.close();
                if (stmt != null) stmt.close();
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка при закрытии SQL ресурсов: " + e.getMessage());
            }
        }
    }

    /**
     * Удаляет квест из базы данных
     * @param questId ID квеста
     */
    public void deleteQuest(Long questId) {
        // Удаляем из кэша
        questCache.removeIf(q -> q.getId() != null && q.getId().equals(questId));

        if (!isDatabaseAccessible()) {
            return;
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction transaction = null;

            try {
                transaction = session.beginTransaction();
                QuestData quest = session.get(QuestData.class, questId);
                if (quest != null) {
                    session.remove(quest);
                    transaction.commit();
                    plugin.getLogger().info("Квест с ID " + questId + " успешно удален");
                } else {
                    plugin.getLogger().warning("Квест с ID " + questId + " не найден для удаления");
                    if (transaction != null) {
                        transaction.rollback();
                    }
                }
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    try {
                        transaction.rollback();
                    } catch (Exception rollbackEx) {
                        plugin.getLogger().severe("Ошибка при откате транзакции: " + rollbackEx.getMessage());
                    }
                }
                plugin.getLogger().severe("Ошибка при удалении квеста: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при удалении квеста: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Метод для создания тестовых квестов напрямую через SQL, минуя Hibernate
     */
    public void createDefaultQuestsWithSQL() {
        if (!isDatabaseAccessible() || !plugin.getDatabaseManager().isUsingPostgreSQL()) {
            plugin.getLogger().warning("SQL-метод создания квестов требует PostgreSQL");
            return;
        }

        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            connection = plugin.getDatabaseManager().getDataSource().getConnection();
            connection.setAutoCommit(false);
            stmt = connection.createStatement();

            // Проверяем наличие квестов
            rs = stmt.executeQuery("SELECT COUNT(*) FROM quests");
            int questCount = 0;
            if (rs.next()) {
                questCount = rs.getInt(1);
            }
            plugin.getLogger().info("Текущее количество квестов в базе: " + questCount);

            // Если квестов нет, создаем через SQL
            if (questCount == 0) {
                plugin.getLogger().info("Создание квестов через SQL...");

                // Сбрасываем последовательность ID, если есть
                // stmt.execute("ALTER SEQUENCE quests_id_seq RESTART WITH 1");

                // SQL для создания квестов напрямую
                String[] insertSqls = {
                        "INSERT INTO quests (title, description, reward_cash, reward_card_money, is_repeatable, quest_type, target_amount, target_item) " +
                                "VALUES ('Камнетёс', 'Добудьте 10 камней', 50.0, 0.0, true, 'BREAK_BLOCK', 10, 'STONE')",

                        "INSERT INTO quests (title, description, reward_cash, reward_card_money, is_repeatable, quest_type, target_amount, target_item) " +
                                "VALUES ('Лесоруб', 'Соберите 20 древесины', 75.0, 0.0, true, 'BREAK_BLOCK', 20, 'OAK_LOG')",

                        "INSERT INTO quests (title, description, reward_cash, reward_card_money, is_repeatable, quest_type, target_amount, target_item) " +
                                "VALUES ('Охотник на зомби', 'Убейте 5 зомби', 100.0, 50.0, true, 'KILL_ENTITY', 5, 'ZOMBIE')"
                };

                for (String sql : insertSqls) {
                    stmt.addBatch(sql);
                }
                int[] results = stmt.executeBatch();

                plugin.getLogger().info("Вставлено записей: " + results.length);

                connection.commit();
                plugin.getLogger().info("Квесты успешно созданы через SQL");

                // Обновляем кэш
                questCache.clear();
                rs = stmt.executeQuery("SELECT * FROM quests ORDER BY id");

                while (rs.next()) {
                    QuestData quest = new QuestData();
                    quest.setId(rs.getLong("id"));
                    quest.setTitle(rs.getString("title"));
                    quest.setDescription(rs.getString("description"));
                    quest.setRewardCash(rs.getDouble("reward_cash"));
                    quest.setRewardCardMoney(rs.getDouble("reward_card_money"));
                    quest.setRepeatable(rs.getBoolean("is_repeatable"));
                    quest.setQuestType(rs.getString("quest_type"));
                    quest.setTargetAmount(rs.getInt("target_amount"));
                    quest.setTargetItem(rs.getString("target_item"));

                    questCache.add(quest);
                    plugin.getLogger().info("Добавлен в кэш квест: " + quest.getTitle() + " (ID: " + quest.getId() + ")");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при SQL создании квестов: " + e.getMessage());
            e.printStackTrace();

            if (connection != null) {
                try {
                    connection.rollback();
                } catch (Exception rollbackEx) {
                    plugin.getLogger().severe("Ошибка при откате SQL транзакции: " + rollbackEx.getMessage());
                }
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка при закрытии SQL ресурсов: " + e.getMessage());
            }
        }
    }

    /**
     * Получает активные квесты игрока
     * @param playerUuid UUID игрока
     * @return Список активных квестов
     */
    public List<PlayerQuestProgress> getPlayerActiveQuests(UUID playerUuid) {
        // Try cache first
        List<PlayerQuestProgress> cachedActiveQuests = new ArrayList<>();
        if (progressCache.containsKey(playerUuid)) {
            for (PlayerQuestProgress progress : progressCache.get(playerUuid).values()) {
                if (!progress.isCompleted()) {
                    cachedActiveQuests.add(progress);
                }
            }
        }

        if (!isDatabaseAccessible()) {
            return cachedActiveQuests;
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                Query<PlayerQuestProgress> query = session.createQuery(
                        "FROM PlayerQuestProgress p WHERE p.playerUuid = :uuid AND p.completed = false",
                        PlayerQuestProgress.class
                );
                query.setParameter("uuid", playerUuid.toString());
                List<PlayerQuestProgress> activeQuests = query.list();

                tx.commit();

                // Update cache
                if (!progressCache.containsKey(playerUuid)) {
                    progressCache.put(playerUuid, new HashMap<>());
                }

                for (PlayerQuestProgress progress : activeQuests) {
                    progressCache.get(playerUuid).put(progress.getQuest().getId(), progress);
                }

                return activeQuests;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception rollbackEx) {
                        plugin.getLogger().severe("Ошибка при откате транзакции: " + rollbackEx.getMessage());
                    }
                }
                plugin.getLogger().severe("Ошибка при получении активных квестов игрока: " + e.getMessage());
                e.printStackTrace();
                return cachedActiveQuests;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при получении активных квестов игрока: " + e.getMessage());
            e.printStackTrace();
            return cachedActiveQuests;
        }
    }

    /**
     * Получает завершенные квесты игрока
     * @param playerUuid UUID игрока
     * @return Список завершенных квестов
     */
    public List<PlayerQuestProgress> getPlayerCompletedQuests(UUID playerUuid) {
        // Try cache first
        List<PlayerQuestProgress> cachedCompletedQuests = new ArrayList<>();
        if (progressCache.containsKey(playerUuid)) {
            for (PlayerQuestProgress progress : progressCache.get(playerUuid).values()) {
                if (progress.isCompleted()) {
                    cachedCompletedQuests.add(progress);
                }
            }
        }

        if (!isDatabaseAccessible()) {
            return cachedCompletedQuests;
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                Query<PlayerQuestProgress> query = session.createQuery(
                        "FROM PlayerQuestProgress p WHERE p.playerUuid = :uuid AND p.completed = true",
                        PlayerQuestProgress.class
                );
                query.setParameter("uuid", playerUuid.toString());
                List<PlayerQuestProgress> completedQuests = query.list();

                tx.commit();

                // Update cache
                if (!progressCache.containsKey(playerUuid)) {
                    progressCache.put(playerUuid, new HashMap<>());
                }

                for (PlayerQuestProgress progress : completedQuests) {
                    progressCache.get(playerUuid).put(progress.getQuest().getId(), progress);
                }

                return completedQuests;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception rollbackEx) {
                        plugin.getLogger().severe("Ошибка при откате транзакции: " + rollbackEx.getMessage());
                    }
                }
                plugin.getLogger().severe("Ошибка при получении завершенных квестов игрока: " + e.getMessage());
                e.printStackTrace();
                return cachedCompletedQuests;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при получении завершенных квестов игрока: " + e.getMessage());
            e.printStackTrace();
            return cachedCompletedQuests;
        }
    }

    /**
     * Получает прогресс игрока по конкретному квесту
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return Объект прогресса или null
     */
    public PlayerQuestProgress getPlayerQuestProgress(UUID playerUuid, Long questId) {
        // Try cache first
        if (progressCache.containsKey(playerUuid) && progressCache.get(playerUuid).containsKey(questId)) {
            return progressCache.get(playerUuid).get(questId);
        }

        if (!isDatabaseAccessible()) {
            return null;
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                Query<PlayerQuestProgress> query = session.createQuery(
                        "FROM PlayerQuestProgress p JOIN FETCH p.quest q WHERE p.playerUuid = :uuid AND q.id = :questId",
                        PlayerQuestProgress.class
                );
                query.setParameter("uuid", playerUuid.toString());
                query.setParameter("questId", questId);
                PlayerQuestProgress progress = query.uniqueResult();

                tx.commit();

                // Update cache
                if (progress != null) {
                    if (!progressCache.containsKey(playerUuid)) {
                        progressCache.put(playerUuid, new HashMap<>());
                    }
                    progressCache.get(playerUuid).put(questId, progress);
                }

                return progress;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception rollbackEx) {
                        plugin.getLogger().severe("Ошибка при откате транзакции: " + rollbackEx.getMessage());
                    }
                }
                plugin.getLogger().severe("Ошибка при получении прогресса квеста: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка при получении прогресса квеста: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Начинает квест для игрока
     * @param player Игрок
     * @param questId ID квеста
     * @return true, если квест успешно начат
     */
    public boolean startQuest(Player player, int questId) {
        return startQuest(player.getUniqueId(), Long.valueOf(questId));
    }

    /**
     * Начинает квест для игрока
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return true, если квест успешно начат
     */
    public boolean startQuest(UUID playerUuid, Long questId) {
        // Получаем данные квеста
        QuestData quest = getQuest(questId);
        if (quest == null) {
            plugin.getLogger().warning("Не удалось найти квест с ID: " + questId);
            return false;
        }

        // Проверяем, не активен ли уже этот квест
        if (isQuestActive(playerUuid, questId)) {
            return false;
        }
        
        // Проверяем, можно ли начать этот квест снова
        if (!quest.isRepeatable() && isQuestCompleted(playerUuid, questId)) {
            return false;
        }
        
        // Создаем новый прогресс
        QuestProgress progress = new QuestProgress(playerUuid, questId);

        if (!isDatabaseAccessible()) {
            return true; // Считаем, что успешно, даже если не сохранили в БД
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                session.persist(progress);
                tx.commit();
                return true;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                plugin.getLogger().severe("Ошибка при начале квеста: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Получает квест по ID (int версия)
     * @param questId ID квеста
     * @return Объект квеста или null
     */
    public QuestData getQuestById(int questId) {
        return getQuest(Long.valueOf(questId));
    }

    /**
     * Проверяет доступность базы данных
     * @return true если база данных доступна
     */
    private boolean isDatabaseAccessible() {
        return plugin.getDatabaseManager() != null &&
                plugin.getDatabaseManager().getSessionFactory() != null &&
                !plugin.getDatabaseManager().getSessionFactory().isClosed() &&
                plugin.getDatabaseManager().getDataSource() != null &&
                !plugin.getDatabaseManager().getDataSource().isClosed();
    }

    /**
     * Обрабатывает событие сбора растения для обновления прогресса соответствующих квестов
     */
    public void notifyPlantHarvested(UUID playerUuid, String plantType, int plantQuality) {
        if (!plugin.isDatabaseAvailable()) {
            return;
        }
        
        try {
            // Простые квесты на выращивание
            updateQuestProgressByType(playerUuid, "GROW_PLANT", plantType, 1);
            
            // Квесты на выращивание растений высокого качества
            if (plantQuality >= 3) {
                updateQuestProgressByType(playerUuid, "GROW_PLANT_QUALITY", plantType, 1);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обновлении прогресса квестов выращивания: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает событие продажи наркотиков для обновления прогресса соответствующих квестов
     */
    public void notifyDrugSale(UUID playerUuid, String drugType, int quantity) {
        if (!plugin.isDatabaseAvailable()) {
            return;
        }
        
        try {
            // Обновляем прогресс квестов на продажу наркотиков
            updateQuestProgressByType(playerUuid, "SELL_DRUG", drugType, quantity);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обновлении прогресса квестов продажи: " + e.getMessage());
        }
    }

    /**
     * Обновляет прогресс квестов определенного типа
     * 
     * @param playerUuid UUID игрока
     * @param questType Тип квеста (например, GROW_PLANT, SELL_DRUG)
     * @param targetItem Целевой предмет
     * @param increment Значение для увеличения прогресса
     */
    public void updateQuestProgressByType(UUID playerUuid, String questType, String targetItem, int increment) {
        try {
            Session session = plugin.getDatabaseManager().getSessionFactory().openSession();
            Transaction tx = session.beginTransaction();
            
            // Получаем активные квесты игрока указанного типа
            List<PlayerQuestProgress> activeQuests = session.createQuery(
                    "FROM PlayerQuestProgress p WHERE p.playerUuid = :uuid AND p.quest.questType = :type AND " +
                    "(p.quest.targetItem = :item OR p.quest.targetItem = 'ANY') AND p.completed = false",
                    PlayerQuestProgress.class)
                    .setParameter("uuid", playerUuid.toString())
                    .setParameter("type", questType)
                    .setParameter("item", targetItem)
                    .getResultList();
            
            // Обновляем прогресс и проверяем выполнение
            for (PlayerQuestProgress pqd : activeQuests) {
                int newProgress = pqd.getCurrentProgress() + increment;
                pqd.setCurrentProgress(newProgress);
                
                // Проверяем, выполнен ли квест
                if (newProgress >= pqd.getQuest().getTargetAmount()) {
                    // Если квест повторяемый, сбрасываем прогресс
                    if (pqd.getQuest().isRepeatable()) {
                        pqd.setCurrentProgress(0);
                        
                        // Выдаем награду
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player != null && player.isOnline()) {
                            // Сообщаем о выполнении
                            player.sendMessage(ChatColor.GOLD + "Квест '" + pqd.getQuest().getTitle() + "' выполнен!");
                            
                            // Выдаем награду
                            if (pqd.getQuest().getRewardCash() > 0) {
                                plugin.getPlayerService().addCash(playerUuid, pqd.getQuest().getRewardCash());
                            }
                            
                            if (pqd.getQuest().getRewardCardMoney() > 0) {
                                plugin.getPlayerService().addCardBalance(playerUuid, pqd.getQuest().getRewardCardMoney());
                            }
                            
                            // Сообщаем о награде
                            player.sendMessage(ChatColor.YELLOW + "Получено: $" + 
                                    String.format("%.2f", pqd.getQuest().getRewardCash()) + " наличными, $" + 
                                    String.format("%.2f", pqd.getQuest().getRewardCardMoney()) + " на карту");
                        }
                    } else {
                        // Если квест не повторяемый, отмечаем как выполненный
                        pqd.setCompleted(true);
                        
                        // Выдаем награду
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player != null && player.isOnline()) {
                            // Сообщаем о выполнении
                            player.sendMessage(ChatColor.GOLD + "Квест '" + pqd.getQuest().getTitle() + "' выполнен!");
                            
                            // Выдаем награду
                            if (pqd.getQuest().getRewardCash() > 0) {
                                plugin.getPlayerService().addCash(playerUuid, pqd.getQuest().getRewardCash());
                            }
                            
                            if (pqd.getQuest().getRewardCardMoney() > 0) {
                                plugin.getPlayerService().addCardBalance(playerUuid, pqd.getQuest().getRewardCardMoney());
                            }
                            
                            // Сообщаем о награде
                            player.sendMessage(ChatColor.YELLOW + "Получено: $" + 
                                    String.format("%.2f", pqd.getQuest().getRewardCash()) + " наличными, $" + 
                                    String.format("%.2f", pqd.getQuest().getRewardCardMoney()) + " на карту");
                        }
                    }
                } else {
                    // Сообщаем о прогрессе
                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(ChatColor.GREEN + "Квест '" + pqd.getQuest().getTitle() + "': " + 
                                newProgress + "/" + pqd.getQuest().getTargetAmount());
                    }
                }
                
                // Сохраняем изменения
                session.merge(pqd);
            }
            
            tx.commit();
            session.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обновлении прогресса квестов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Получает активные квесты игрока
     * @param playerUuid UUID игрока
     * @return Список активных квестов
     */
    public List<QuestProgress> getActiveQuests(UUID playerUuid) {
        List<QuestProgress> result = new ArrayList<>();

        if (!isDatabaseAccessible()) {
            return result;
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                
                Query<QuestProgress> query = session.createQuery(
                    "FROM QuestProgress p WHERE p.playerUuid = :uuid AND p.completed = false",
                    QuestProgress.class
                );
                query.setParameter("uuid", playerUuid.toString());
                result = query.list();
                
                tx.commit();
                return result;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                plugin.getLogger().severe("Ошибка при получении активных квестов: " + e.getMessage());
                return result;
            }
        }
    }

    /**
     * Получает завершенные квесты игрока
     * @param playerUuid UUID игрока
     * @return Список завершенных квестов
     */
    public List<QuestProgress> getCompletedQuests(UUID playerUuid) {
        List<QuestProgress> result = new ArrayList<>();
        
        if (!isDatabaseAccessible()) {
            return result;
        }
        
        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                
                Query<QuestProgress> query = session.createQuery(
                    "FROM QuestProgress p WHERE p.playerUuid = :uuid AND p.completed = true",
                    QuestProgress.class
                );
                query.setParameter("uuid", playerUuid.toString());
                result = query.list();
                
                tx.commit();
                return result;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                plugin.getLogger().severe("Ошибка при получении завершенных квестов: " + e.getMessage());
                return result;
            }
        }
    }

    /**
     * Получает прогресс квеста для игрока
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return Объект прогресса или null, если не найден
     */
    public QuestProgress getQuestProgress(UUID playerUuid, Long questId) {
        if (!isDatabaseAccessible()) {
            return null;
        }
        
        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                
                Query<QuestProgress> query = session.createQuery(
                    "FROM QuestProgress p WHERE p.playerUuid = :uuid AND p.questId = :questId",
                    QuestProgress.class
                );
                query.setParameter("uuid", playerUuid.toString());
                query.setParameter("questId", questId);
                
                QuestProgress result = query.uniqueResult();
                tx.commit();
                return result;
        } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                plugin.getLogger().severe("Ошибка при получении прогресса квеста: " + e.getMessage());
                return null;
            }
        }
    }

    /**
     * Проверяет, активен ли квест для игрока
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return true, если квест активен
     */
    public boolean isQuestActive(UUID playerUuid, Long questId) {
        QuestProgress progress = getQuestProgress(playerUuid, questId);
        return progress != null && !progress.isCompleted();
    }

    /**
     * Проверяет, активен ли квест для игрока (для int параметра)
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return true, если квест активен
     */
    public boolean isQuestActive(UUID playerUuid, int questId) {
        return isQuestActive(playerUuid, Long.valueOf(questId));
    }

    /**
     * Проверяет, завершен ли квест игроком
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return true, если квест завершен
     */
    public boolean isQuestCompleted(UUID playerUuid, Long questId) {
        QuestProgress progress = getQuestProgress(playerUuid, questId);
        return progress != null && progress.isCompleted();
    }

    /**
     * Проверяет, завершен ли квест игроком (версия для int)
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return true, если квест завершен
     */
    public boolean isQuestCompleted(UUID playerUuid, int questId) {
        return isQuestCompleted(playerUuid, Long.valueOf(questId));
    }

    /**
     * Начинает квест для игрока
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return true, если квест успешно начат
     */
    public boolean startQuest(UUID playerUuid, int questId) {
        return startQuest(playerUuid, Long.valueOf(questId));
    }

    /**
     * Завершает квест
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return true если квест успешно завершен
     */
    public boolean completeQuest(UUID playerUuid, Long questId) {
        QuestProgress progress = getQuestProgress(playerUuid, questId);

        if (progress == null || progress.isCompleted()) {
            return false;
        }

        // Отмечаем как завершенный
        progress.setCompleted(true);
        
        // Сохраняем в БД, если доступна
        if (!isDatabaseAccessible()) {
            return true;
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                session.merge(progress);
                tx.commit();
                return true;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                plugin.getLogger().severe("Ошибка при завершении квеста: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Отменяет квест для игрока
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return true если квест успешно отменен
     */
    public boolean abandonQuest(UUID playerUuid, Long questId) {
        // Проверяем, активен ли квест
        if (!isQuestActive(playerUuid, questId)) {
            return false;
        }

        // Получаем прогресс квеста
        QuestProgress progress = getQuestProgress(playerUuid, questId);
        if (progress == null) {
            return false;
        }

        // Обновляем кэш - удаляем прогресс из кэша, если он там был
        if (progressCache.containsKey(playerUuid) && progressCache.get(playerUuid).containsKey(questId)) {
            progressCache.get(playerUuid).remove(questId);
        }

        // Если база недоступна, считаем что удалили успешно
        if (!isDatabaseAccessible()) {
            return true;
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                
                // Удаляем запись из базы данных
                session.remove(progress);
                
                tx.commit();
                plugin.getLogger().info("Игрок " + playerUuid + " отменил квест #" + questId);
                return true;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception rollbackEx) {
                        plugin.getLogger().severe("Ошибка при откате транзакции: " + rollbackEx.getMessage());
                    }
                }
                plugin.getLogger().severe("Ошибка при отмене квеста: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Отменяет квест для игрока (версия для int)
     * @param playerUuid UUID игрока
     * @param questId ID квеста
     * @return true если квест успешно отменен
     */
    public boolean abandonQuest(UUID playerUuid, int questId) {
        return abandonQuest(playerUuid, Long.valueOf(questId));
    }

    /**
     * Отменяет квест для игрока
     * @param player Игрок
     * @param questId ID квеста
     * @return true если квест успешно отменен
     */
    public boolean abandonQuest(Player player, int questId) {
        return abandonQuest(player.getUniqueId(), Long.valueOf(questId));
    }
}