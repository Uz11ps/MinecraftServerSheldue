package com.Minecraft_server.Narkomanka.services;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.logging.Level;

public class PlayerService {
    private final Narkomanka plugin;
    // Cache for player data when database is unavailable
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();

    public PlayerService(Narkomanka plugin) {
        this.plugin = plugin;
    }

    /**
     * Получает или создает данные игрока по Player
     */
    public PlayerData getOrCreatePlayerData(Player player) {
        return getOrCreatePlayerData(player.getUniqueId(), player.getName());
    }

    /**
     * Получает или создает данные игрока по UUID
     */
    public PlayerData getOrCreatePlayerData(UUID playerUuid) {
        // Проверяем кэш
        PlayerData playerData = playerDataCache.get(playerUuid);
        if (playerData != null) {
            return playerData;
        }
        
        // Ищем игрока онлайн
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            return getOrCreatePlayerData(player.getUniqueId(), player.getName());
        }
        
        // Загружаем из БД или создаем с дефолтным именем
        playerData = loadPlayerData(playerUuid);
        if (playerData == null) {
            playerData = createNewPlayerData(playerUuid, "Unknown_" + playerUuid.toString().substring(0, 8));
        }
        
        return playerData;
    }

    /**
     * Получает или создает данные игрока по UUID и имени
     */
    public PlayerData getOrCreatePlayerData(UUID playerUuid, String playerName) {
        // Проверяем кэш
        PlayerData playerData = playerDataCache.get(playerUuid);
        if (playerData != null) {
            return playerData;
        }
        
        // Пытаемся загрузить из БД
        playerData = loadPlayerData(playerUuid);
        
        // Если не нашли, создаем нового игрока
        if (playerData == null) {
            playerData = createNewPlayerData(playerUuid, playerName);
        }
        
        // Добавляем в кэш
        playerDataCache.put(playerUuid, playerData);
        return playerData;
    }

    /**
     * Получает данные игрока без создания новых
     */
    public PlayerData getPlayerData(UUID playerUuid) {
        // Check cache first
        if (playerDataCache.containsKey(playerUuid)) {
            return playerDataCache.get(playerUuid);
        }

        // Attempt to load from database
        PlayerData playerData = loadPlayerData(playerUuid);
        if (playerData != null) {
            playerDataCache.put(playerUuid, playerData);
        }

        return playerData;
    }

    /**
     * Сохраняет данные игрока
     */
    public void savePlayerData(PlayerData playerData) {
        // Получаем UUID игрока
        UUID playerUuid = playerData.getPlayerUuid();
        
        // Обновляем кэш
        playerDataCache.put(playerUuid, playerData);
        
        // Пропускаем сохранение в БД, если она недоступна
        if (!isDatabaseAccessible()) {
            plugin.getLogger().warning("Database not available, player data saved to cache only");
            return;
        }

        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            Transaction transaction = null;
            try {
                transaction = session.beginTransaction();
                session.merge(playerData);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                plugin.getLogger().log(Level.SEVERE, "Error saving player data: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Добавляет наличные игроку
     */
    public boolean addCash(UUID playerUuid, double amount) {
        PlayerData playerData = getOrCreatePlayerData(playerUuid);
        playerData.addCash(amount);
        savePlayerData(playerData);
        return true;
    }

    /**
     * Снимает наличные у игрока
     */
    public boolean removeCash(UUID playerUuid, double amount) {
        PlayerData playerData = getOrCreatePlayerData(playerUuid);
        boolean success = playerData.removeCash(amount);
        if (success) {
            savePlayerData(playerData);
        }
        return success;
    }

    /**
     * Добавляет деньги на карту игрока
     */
    public boolean addCardBalance(UUID playerUuid, double amount) {
        PlayerData playerData = getOrCreatePlayerData(playerUuid);
        playerData.addCardBalance(amount);
        savePlayerData(playerData);
        return true;
    }

    /**
     * Снимает деньги с карты игрока
     */
    public boolean removeCardBalance(UUID playerUuid, double amount) {
        PlayerData playerData = getOrCreatePlayerData(playerUuid);
        boolean success = playerData.removeCardBalance(amount);
        if (success) {
            savePlayerData(playerData);
        }
        return success;
    }

    /**
     * Переводит наличные на карту игрока
     */
    public boolean depositToCard(UUID playerUuid, double amount) {
        PlayerData playerData = getOrCreatePlayerData(playerUuid);
        if (playerData.getCashBalance() >= amount) {
            playerData.removeCash(amount);
            playerData.addCardBalance(amount);
            savePlayerData(playerData);
            return true;
        }
        return false;
    }

    /**
     * Снимает деньги с карты на наличные
     */
    public boolean withdrawFromCard(UUID playerUuid, double amount) {
        PlayerData playerData = getOrCreatePlayerData(playerUuid);
        if (playerData.getCardBalance() >= amount) {
            playerData.removeCardBalance(amount);
            playerData.addCash(amount);
            savePlayerData(playerData);
            return true;
        }
        return false;
    }

    /**
     * Проверяет доступность базы данных
     */
    private boolean isDatabaseAccessible() {
        return plugin.isDatabaseAvailable() && plugin.getDatabaseManager() != null &&
                plugin.getDatabaseManager().getSessionFactory() != null &&
                !plugin.getDatabaseManager().getSessionFactory().isClosed();
    }

    /**
     * Получает баланс наличных игрока
     */
    public double getCashBalance(UUID playerUuid) {
        PlayerData playerData = getOrCreatePlayerData(playerUuid);
        return playerData.getCashBalance();
    }

    /**
     * Получает баланс на карте игрока
     */
    public double getCardBalance(UUID playerUuid) {
        PlayerData playerData = getOrCreatePlayerData(playerUuid);
        return playerData.getCardBalance();
    }

    /**
     * Получает баланс наличных игрока (метод-алиас для совместимости)
     */
    public double getCash(UUID playerUuid) {
        return getCashBalance(playerUuid);
    }
    
    /**
     * Загружает данные игрока из БД
     */
    private PlayerData loadPlayerData(UUID playerUuid) {
        // Если БД недоступна, возвращаем null
        if (!isDatabaseAccessible()) {
            return null;
        }
        
        try (Session session = plugin.getDatabaseManager().getSessionFactory().openSession()) {
            PlayerData playerData = session.get(PlayerData.class, playerUuid.toString());
            if (playerData != null) {
                // Добавляем в кэш
                playerDataCache.put(playerUuid, playerData);
                return playerData;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при загрузке данных игрока: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Создает нового игрока
     */
    private PlayerData createNewPlayerData(UUID playerUuid, String playerName) {
        double startingCash = plugin.getConfig().getDouble("economy.starting-cash", 100.0);
        double startingCardBalance = plugin.getConfig().getDouble("economy.starting-card-balance", 0.0);
        
        PlayerData playerData = new PlayerData(playerUuid, playerName);
        playerData.setCashBalance(startingCash);
        playerData.setCardBalance(startingCardBalance);
        
        savePlayerData(playerData);
        plugin.getLogger().info("Created new player data for " + playerName);
        
        return playerData;
    }
}