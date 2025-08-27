package com.Minecraft_server.Narkomanka.trash;

import com.Minecraft_server.Narkomanka.Narkomanka;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс для управления станциями переработки мусора
 */
public class TrashStation {
    private final Narkomanka plugin;
    private final NamespacedKey stationKey;
    private final Map<String, Location> stations = new HashMap<>();
    private final Map<UUID, TrashStationData> trashStations = new HashMap<>();

    public TrashStation(Narkomanka plugin) {
        this.plugin = plugin;
        this.stationKey = new NamespacedKey(plugin, "trash_station");

        // Загружаем существующие станции
        loadStations();
    }

    /**
     * Загружает сохраненные станции переработки мусора
     */
    private void loadStations() {
        stations.clear();

        if (!plugin.getConfig().isConfigurationSection("trash.stations")) {
            plugin.getLogger().info("Секция станций переработки мусора не найдена в конфигурации");
            return;
        }

        for (String key : plugin.getConfig().getConfigurationSection("trash.stations").getKeys(false)) {
            String worldName = plugin.getConfig().getString("trash.stations." + key + ".world");
            double x = plugin.getConfig().getDouble("trash.stations." + key + ".x");
            double y = plugin.getConfig().getDouble("trash.stations." + key + ".y");
            double z = plugin.getConfig().getDouble("trash.stations." + key + ".z");

            if (worldName != null && Bukkit.getWorld(worldName) != null) {
                Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                stations.put(key, location);
                plugin.getLogger().info("Загружена станция переработки мусора: " + key + " @ " + location);
            }
        }
    }

    /**
     * Сохраняет станции переработки мусора в конфигурацию
     */
    private void saveStations() {
        if (plugin.getConfig().isConfigurationSection("trash.stations")) {
            plugin.getConfig().set("trash.stations", null);
        }

        for (Map.Entry<String, Location> entry : stations.entrySet()) {
            String key = entry.getKey();
            Location loc = entry.getValue();

            plugin.getConfig().set("trash.stations." + key + ".world", loc.getWorld().getName());
            plugin.getConfig().set("trash.stations." + key + ".x", loc.getX());
            plugin.getConfig().set("trash.stations." + key + ".y", loc.getY());
            plugin.getConfig().set("trash.stations." + key + ".z", loc.getZ());
        }

        plugin.saveConfig();
    }

    /**
     * Создает новую станцию переработки мусора
     */
    public void createTrashStation(Location location, String name) {
        // Создаем станцию с уникальным ID
        UUID stationId = UUID.randomUUID();
        
        // Размещаем специальный блок с кастомной текстурой мусорки
        Block block = location.getBlock();
        block.setType(Material.CAULDRON); // Используем котел как основу для мусорки
        
        // Создаем голограмму с названием
        ArmorStand hologram = (ArmorStand) location.getWorld().spawnEntity(
                location.clone().add(0.5, 0.5, 0.5), EntityType.ARMOR_STAND);
        
        hologram.setCustomName(ChatColor.GREEN + "Мусорка: " + name);
        hologram.setCustomNameVisible(true);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setSmall(true);
        hologram.setMarker(true);
        
        // Добавляем метаданные к блоку
        block.setMetadata("trash_station", new FixedMetadataValue(plugin, stationId.toString()));
        block.setMetadata("trash_station_name", new FixedMetadataValue(plugin, name));
        
        // Записываем в конфиг
        storeTrashStationInConfig(stationId, location, name);
        
        // Добавляем в карту станций
        trashStations.put(stationId, new TrashStationData(stationId, location, name, hologram));
        
        plugin.getLogger().info("Создана станция переработки мусора: " + name + " на координатах: " + 
                location.getWorld().getName() + " " + location.getBlockX() + " " + 
                location.getBlockY() + " " + location.getBlockZ());
    }

    /**
     * Восстанавливает станции переработки мусора из конфига
     */
    private void loadTrashStationsFromConfig() {
        ConfigurationSection stationsSection = plugin.getConfig().getConfigurationSection("trash.stations");
        if (stationsSection == null) {
            plugin.getLogger().info("Нет сохраненных станций переработки мусора");
            return;
        }
        
        for (String key : stationsSection.getKeys(false)) {
            try {
                UUID stationId = UUID.fromString(key);
                ConfigurationSection stationSection = stationsSection.getConfigurationSection(key);
                
                if (stationSection != null) {
                    String worldName = stationSection.getString("world");
                    double x = stationSection.getDouble("x");
                    double y = stationSection.getDouble("y");
                    double z = stationSection.getDouble("z");
                    String name = stationSection.getString("name", "Станция");
                    
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        Location location = new Location(world, x, y, z);
                        
                        // Проверяем, существует ли блок станции
                        Block block = location.getBlock();
                        if (block.getType() != Material.CAULDRON) {
                            // Восстанавливаем блок станции
                            block.setType(Material.CAULDRON);
                            block.setMetadata("trash_station", new FixedMetadataValue(plugin, stationId.toString()));
                            block.setMetadata("trash_station_name", new FixedMetadataValue(plugin, name));
                        }
                        
                        // Создаем или восстанавливаем голограмму
                        ArmorStand hologram = null;
                        for (Entity entity : location.getWorld().getNearbyEntities(location.clone().add(0.5, 0.5, 0.5), 1, 1, 1)) {
                            if (entity instanceof ArmorStand && entity.getCustomName() != null && 
                                    entity.getCustomName().contains(name)) {
                                hologram = (ArmorStand) entity;
                                break;
                            }
                        }
                        
                        if (hologram == null) {
                            hologram = (ArmorStand) location.getWorld().spawnEntity(
                                    location.clone().add(0.5, 0.5, 0.5), EntityType.ARMOR_STAND);
                            hologram.setCustomName(ChatColor.GREEN + "Мусорка: " + name);
                            hologram.setCustomNameVisible(true);
                            hologram.setVisible(false);
                            hologram.setGravity(false);
                            hologram.setSmall(true);
                            hologram.setMarker(true);
                        }
                        
                        // Добавляем в карту станций
                        trashStations.put(stationId, new TrashStationData(stationId, location, name, hologram));
                        
                        plugin.getLogger().info("Загружена станция переработки мусора: " + name + " на координатах: " + 
                                worldName + " " + x + " " + y + " " + z);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при загрузке станции переработки мусора: " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Загружено станций переработки мусора: " + trashStations.size());
    }

    /**
     * Проверяет, является ли блок станцией переработки мусора
     */
    public boolean isTrashStation(Block block) {
        if (block.getType() != Material.CHEST) return false;

        BlockState state = block.getState();
        if (!(state instanceof Chest chest)) return false;

        PersistentDataContainer container = chest.getPersistentDataContainer();
        return container.has(stationKey, PersistentDataType.STRING);
    }

    /**
     * Получает название станции по блоку
     */
    public String getStationName(Block block) {
        if (!isTrashStation(block)) return null;

        BlockState state = block.getState();
        if (!(state instanceof Chest chest)) return null;

        PersistentDataContainer container = chest.getPersistentDataContainer();
        return container.get(stationKey, PersistentDataType.STRING);
    }

    /**
     * Удаляет станцию переработки мусора
     */
    public boolean removeStation(String name) {
        Location location = stations.get(name);
        if (location == null) return false;

        // Удаляем сундук и табличку
        Block block = location.getBlock();
        if (block.getType() == Material.CHEST) {
            block.setType(Material.AIR);
        }

        Block signBlock = block.getRelative(0, 1, 0);
        if (signBlock.getType() == Material.OAK_SIGN) {
            signBlock.setType(Material.AIR);
        }

        // Удаляем из списка и сохраняем
        stations.remove(name);
        saveStations();

        return true;
    }

    /**
     * Обрабатывает сдачу мусора игроком
     */
    public void processTrashTurn(Player player, Block stationBlock) {
        int totalTrashValue = 0;
        int trashCount = 0;
        
        // Проверяем инвентарь игрока на наличие мусора
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getTrashManager().isTrash(item)) {
                int itemValue = plugin.getTrashManager().getTrashValue(item) * item.getAmount();
                totalTrashValue += itemValue;
                trashCount += item.getAmount();
                player.getInventory().remove(item);
            }
        }
        
        // Проверяем, держит ли игрок контейнер для мусора
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (plugin.getTrashContainer().isContainer(mainHand)) {
            UUID containerUuid = plugin.getTrashContainer().getContainerUuid(mainHand);
            if (containerUuid != null) {
                int containerValue = plugin.getTrashContainer().emptyContainerAndGetValue(containerUuid);
                if (containerValue > 0) {
                    totalTrashValue += containerValue;
                    trashCount++;
                    player.sendMessage("§aМусор из контейнера сдан на переработку!");
                }
            }
        }
        
        // Если нашелся мусор, выдаем награду
        if (totalTrashValue > 0) {
            // Добавляем деньги игроку
            plugin.getPlayerService().addCash(player.getUniqueId(), totalTrashValue);
            
            // Отправляем сообщение о сдаче мусора
            player.sendMessage("§aВы сдали " + trashCount + " единиц мусора и получили $" + totalTrashValue);
            
            // Обновляем статистику игрока и прогресс квестов
            if (plugin.isDatabaseAvailable()) {
                // Уведомляем слушателя квестов о сдаче мусора
                plugin.getTrashQuestListener().onTrashTurnIn(player, trashCount);
            }
        } else {
            player.sendMessage("§cУ вас нет мусора для сдачи.");
        }
    }

    /**
     * Получает список всех станций
     */
    public Map<String, Location> getStations() {
        return new HashMap<>(stations);
    }

    /**
     * Сохраняет данные станции в конфиг
     */
    private void storeTrashStationInConfig(UUID stationId, Location location, String name) {
        String path = "trash.stations." + stationId;
        plugin.getConfig().set(path + ".world", location.getWorld().getName());
        plugin.getConfig().set(path + ".x", location.getX());
        plugin.getConfig().set(path + ".y", location.getY());
        plugin.getConfig().set(path + ".z", location.getZ());
        plugin.getConfig().set(path + ".name", name);
        plugin.saveConfig();
    }

    /**
     * Создает новую станцию переработки мусора (метод совместимости)
     * @param location Местоположение станции
     * @param name Название станции
     * @return true если станция создана успешно
     */
    public boolean createStation(Location location, String name) {
        createTrashStation(location, name);
        return true;
    }

    /**
     * Класс для хранения данных о станции переработки мусора
     */
    public static class TrashStationData {
        private final UUID id;
        private final Location location;
        private final String name;
        private final ArmorStand hologram;
        
        public TrashStationData(UUID id, Location location, String name, ArmorStand hologram) {
            this.id = id;
            this.location = location;
            this.name = name;
            this.hologram = hologram;
        }
        
        public UUID getId() {
            return id;
        }
        
        public Location getLocation() {
            return location;
        }
        
        public String getName() {
            return name;
        }
        
        public ArmorStand getHologram() {
            return hologram;
        }
    }
}
