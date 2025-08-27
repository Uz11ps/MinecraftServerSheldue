package com.Minecraft_server.Narkomanka.trash;

import com.Minecraft_server.Narkomanka.Narkomanka;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Менеджер для управления мусором - генерация, отслеживание и удаление
 */
public class TrashManager {
    private final Narkomanka plugin;
    private final NamespacedKey trashKey;
    private final NamespacedKey trashValueKey;

    // Конфигурационные параметры
    private int maxTrashItemsPerWorld = 100; // Максимальное количество мусора в мире
    private int trashSpawnRateMinutes = 5;   // Как часто появляется мусор (в минутах)
    private int trashDespawnTimeMinutes = 30; // Время жизни мусора (в минутах)
    private int minTrashValue = 1;           // Минимальная стоимость мусора
    private int maxTrashValue = 10;          // Максимальная стоимость мусора
    private int trashRadius = 50;            // Радиус вокруг игроков для появления мусора
    private int maximumTrashSpawnAttempts = 10; // Максимальное количество попыток генерации за один цикл

    // Настраиваемые материалы, которые можно использовать как мусор
    private final List<TrashType> trashTypes = new ArrayList<>();

    // Отслеживание созданного мусора
    private final Map<UUID, TrashItem> activeTrashItems = new HashMap<>();

    // Задачи Bukkit для генерации и управления мусором
    private BukkitTask trashGenerationTask;
    private BukkitTask trashCleanupTask;

    public TrashManager(Narkomanka plugin) {
        this.plugin = plugin;
        this.trashKey = new NamespacedKey(plugin, "is_trash");
        this.trashValueKey = new NamespacedKey(plugin, "trash_value");

        // Загрузка конфигурации мусора
        loadConfig();

        // Планирование задач
        startTasks();
    }

    /**
     * Загружает конфигурацию системы мусора из config.yml
     */
    public void loadConfig() {
        plugin.reloadConfig();
        ConfigurationSection trashConfig = plugin.getConfig().getConfigurationSection("trash");

        if (trashConfig == null) {
            plugin.getLogger().warning("Секция конфигурации мусора не найдена. Используются значения по умолчанию.");
            // Создадим стандартные типы мусора
            initializeDefaultTrashTypes();
            return;
        }

        // Загрузка основных параметров
        maxTrashItemsPerWorld = trashConfig.getInt("max-items-per-world", maxTrashItemsPerWorld);
        trashSpawnRateMinutes = trashConfig.getInt("spawn-rate-minutes", trashSpawnRateMinutes);
        trashDespawnTimeMinutes = trashConfig.getInt("despawn-time-minutes", trashDespawnTimeMinutes);
        minTrashValue = trashConfig.getInt("min-value", minTrashValue);
        maxTrashValue = trashConfig.getInt("max-value", maxTrashValue);
        trashRadius = trashConfig.getInt("spawn-radius", trashRadius);
        maximumTrashSpawnAttempts = trashConfig.getInt("max-spawn-attempts", maximumTrashSpawnAttempts);

        // Загрузка типов мусора
        trashTypes.clear();
        ConfigurationSection trashTypesSection = trashConfig.getConfigurationSection("types");

        if (trashTypesSection == null) {
            plugin.getLogger().warning("Секция типов мусора не найдена. Используются типы по умолчанию.");
            initializeDefaultTrashTypes();
        } else {
            for (String key : trashTypesSection.getKeys(false)) {
                ConfigurationSection typeSection = trashTypesSection.getConfigurationSection(key);
                if (typeSection == null) continue;

                String materialName = typeSection.getString("material");
                String displayName = typeSection.getString("display-name", key);
                int minValue = typeSection.getInt("min-value", minTrashValue);
                int maxValue = typeSection.getInt("max-value", maxTrashValue);
                int weight = typeSection.getInt("weight", 1);

                try {
                    Material material = Material.valueOf(materialName);
                    TrashType trashType = new TrashType(material, displayName, minValue, maxValue, weight);
                    trashTypes.add(trashType);
                    plugin.getLogger().info("Загружен тип мусора: " + displayName + " (" + materialName + ")");
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Неверный материал для мусора: " + materialName);
                }
            }
        }

        // Если после загрузки из конфига список типов пуст, используем стандартные типы
        if (trashTypes.isEmpty()) {
            initializeDefaultTrashTypes();
        }
    }

    /**
     * Инициализирует стандартные типы мусора
     */
    private void initializeDefaultTrashTypes() {
        trashTypes.add(new TrashType(Material.GLASS_BOTTLE, "Стеклянная бутылка", 1, 3, 10));
        trashTypes.add(new TrashType(Material.PAPER, "Мусорная бумага", 1, 2, 15));
        trashTypes.add(new TrashType(Material.BONE, "Кость", 2, 4, 8));
        trashTypes.add(new TrashType(Material.ROTTEN_FLESH, "Гнилая плоть", 1, 2, 12));
        trashTypes.add(new TrashType(Material.STRING, "Верёвка", 1, 3, 10));
        trashTypes.add(new TrashType(Material.STICK, "Палка", 1, 2, 20));
        trashTypes.add(new TrashType(Material.LEATHER, "Старая кожа", 2, 4, 8));
        trashTypes.add(new TrashType(Material.FEATHER, "Перо", 1, 3, 10));

        plugin.getLogger().info("Инициализированы стандартные типы мусора: " + trashTypes.size());
    }

    /**
     * Запускает задачи генерации и очистки мусора
     */
    public void startTasks() {
        // Отменяем существующие задачи, если они есть
        stopTasks();

        // Задача генерации мусора
        long spawnTickRate = trashSpawnRateMinutes * 60 * 20L; // переводим минуты в тики (20 тиков = 1 секунда)
        trashGenerationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::generateTrash, 100L, spawnTickRate);

        // Задача для периодической очистки устаревшего мусора
        long cleanupTickRate = 5 * 60 * 20L; // Проверка каждые 5 минут
        trashCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupOldTrash, 200L, cleanupTickRate);

        plugin.getLogger().info("Запущены задачи генерации и очистки мусора");
    }

    /**
     * Останавливает задачи генерации и очистки мусора
     */
    public void stopTasks() {
        if (trashGenerationTask != null && !trashGenerationTask.isCancelled()) {
            trashGenerationTask.cancel();
            trashGenerationTask = null;
        }

        if (trashCleanupTask != null && !trashCleanupTask.isCancelled()) {
            trashCleanupTask.cancel();
            trashCleanupTask = null;
        }
    }

    /**
     * Генерирует новый мусор в мире
     */
    public void generateTrash() {
        for (World world : Bukkit.getWorlds()) {
            // Пропускаем миры Нижнего мира и Края
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }

            // Считаем, сколько мусора уже есть в этом мире
            long trashInWorld = activeTrashItems.values().stream()
                    .filter(trash -> trash.getWorld().equals(world.getName()))
                    .count();

            // Если достигнут лимит, пропускаем
            if (trashInWorld >= maxTrashItemsPerWorld) {
                plugin.getLogger().fine("Достигнут лимит мусора в мире " + world.getName() + ": " + trashInWorld);
                continue;
            }

            // Определяем, сколько мусора нужно сгенерировать
            int toGenerate = Math.min(
                    maximumTrashSpawnAttempts,
                    maxTrashItemsPerWorld - (int) trashInWorld
            );

            // Если в мире нет игроков, не генерируем мусор
            if (world.getPlayers().isEmpty()) {
                continue;
            }

            for (int i = 0; i < toGenerate; i++) {
                trySpawnTrashNearRandomPlayer(world);
            }
        }
    }

    /**
     * Пытается создать мусор рядом со случайным игроком
     */
    private void trySpawnTrashNearRandomPlayer(World world) {
        List<org.bukkit.entity.Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        // Выбираем случайного игрока
        org.bukkit.entity.Player player = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        Location playerLoc = player.getLocation();

        // Генерируем случайное смещение в пределах радиуса
        int dx = ThreadLocalRandom.current().nextInt(-trashRadius, trashRadius + 1);
        int dz = ThreadLocalRandom.current().nextInt(-trashRadius, trashRadius + 1);

        Location spawnLoc = playerLoc.clone().add(dx, 0, dz);

        // Находим верхний блок в этой координате
        Block highestBlock = world.getHighestBlockAt(spawnLoc);
        if (highestBlock.getType() == Material.AIR || !highestBlock.getType().isSolid()) {
            return; // Не подходящий блок для размещения
        }

        // Устанавливаем локацию на 1 блок выше самого высокого блока
        spawnLoc.setY(highestBlock.getY() + 1);

        // Выбираем случайный тип мусора с учетом весов
        TrashType trashType = selectRandomTrashType();
        if (trashType == null) return;

        // Генерируем случайную стоимость в пределах этого типа
        int value = ThreadLocalRandom.current().nextInt(
                trashType.getMinValue(),
                trashType.getMaxValue() + 1
        );

        // Создаем предмет мусора
        spawnTrashAt(spawnLoc, trashType, value);
    }

    /**
     * Выбирает случайный тип мусора с учетом весов
     */
    private TrashType selectRandomTrashType() {
        if (trashTypes.isEmpty()) return null;

        // Рассчитываем общий вес всех типов
        int totalWeight = trashTypes.stream().mapToInt(TrashType::getWeight).sum();

        // Генерируем случайное число в диапазоне общего веса
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);

        // Выбираем тип на основе веса
        int currentWeight = 0;
        for (TrashType type : trashTypes) {
            currentWeight += type.getWeight();
            if (randomWeight < currentWeight) {
                return type;
            }
        }

        // Если по какой-то причине не выбрали, берем первый
        return trashTypes.get(0);
    }

    /**
     * Создает предмет мусора в указанной локации
     */
    public Item spawnTrashAt(Location location, TrashType trashType, int value) {
        try {
            ItemStack itemStack = new ItemStack(trashType.getMaterial());
            ItemMeta meta = itemStack.getItemMeta();

            if (meta != null) {
                // Устанавливаем название
                meta.displayName(net.kyori.adventure.text.Component.text(trashType.getDisplayName())
                        .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));

                // Добавляем метаданные
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(trashKey, PersistentDataType.BYTE, (byte) 1);
                container.set(trashValueKey, PersistentDataType.INTEGER, value);

                itemStack.setItemMeta(meta);
            }

            // Создаем энтити предмета в мире
            Item itemEntity = location.getWorld().dropItem(location, itemStack);
            itemEntity.setGlowing(true); // Делаем мусор светящимся для лучшей видимости

            // Отслеживаем мусор
            TrashItem trashItem = new TrashItem(
                    itemEntity.getUniqueId(),
                    location.getWorld().getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    trashType.getDisplayName(),
                    value,
                    System.currentTimeMillis()
            );

            activeTrashItems.put(itemEntity.getUniqueId(), trashItem);

            plugin.getLogger().fine("Создан мусор '" + trashType.getDisplayName() +
                    "' стоимостью " + value + " в " + location.getWorld().getName());

            return itemEntity;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка при создании мусора: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Очищает устаревший мусор
     */
    public void cleanupOldTrash() {
        long currentTime = System.currentTimeMillis();
        long maxAgeMillis = trashDespawnTimeMinutes * 60 * 1000L;

        Iterator<Map.Entry<UUID, TrashItem>> iterator = activeTrashItems.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, TrashItem> entry = iterator.next();
            TrashItem trashItem = entry.getValue();

            // Проверяем, не устарел ли мусор
            if (currentTime - trashItem.getCreationTime() > maxAgeMillis) {
                // Ищем энтити и удаляем его
                UUID entityId = entry.getKey();
                for (World world : Bukkit.getWorlds()) {
                    for (org.bukkit.entity.Entity entity : world.getEntities()) {
                        if (entity.getUniqueId().equals(entityId) && entity instanceof Item) {
                            entity.remove();
                            break;
                        }
                    }
                }

                iterator.remove();
                plugin.getLogger().fine("Удален устаревший мусор: " + trashItem.getName());
            }
        }
    }

    /**
     * Проверяет, является ли предмет мусором
     */
    public boolean isTrash(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.has(trashKey, PersistentDataType.BYTE);
    }

    /**
     * Получает стоимость мусора
     */
    public int getTrashValue(ItemStack item) {
        if (!isTrash(item)) return 0;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.getOrDefault(trashValueKey, PersistentDataType.INTEGER, 0);
    }

    /**
     * Удаляет предмет мусора из отслеживания
     */
    public void removeTrashItem(UUID itemId) {
        activeTrashItems.remove(itemId);
    }

    /**
     * Получает количество активного мусора
     */
    public int getActiveTrashCount() {
        return activeTrashItems.size();
    }

    /**
     * Удаляет весь мусор из мира
     */
    public int removeAllTrash() {
        int count = 0;

        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    Item item = (Item) entity;
                    if (isTrash(item.getItemStack())) {
                        entity.remove();
                        count++;
                    }
                }
            }
        }

        activeTrashItems.clear();
        return count;
    }

    /**
     * Класс для хранения типа мусора
     */
    public static class TrashType {
        private final Material material;
        private final String displayName;
        private final int minValue;
        private final int maxValue;
        private final int weight;

        public TrashType(Material material, String displayName, int minValue, int maxValue, int weight) {
            this.material = material;
            this.displayName = displayName;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.weight = weight;
        }

        public Material getMaterial() {
            return material;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getMinValue() {
            return minValue;
        }

        public int getMaxValue() {
            return maxValue;
        }

        public int getWeight() {
            return weight;
        }
    }

    /**
     * Класс для хранения информации о мусоре в мире
     */
    public static class TrashItem {
        private final UUID entityId;
        private final String world;
        private final double x, y, z;
        private final String name;
        private final int value;
        private final long creationTime;

        public TrashItem(UUID entityId, String world, double x, double y, double z,
                         String name, int value, long creationTime) {
            this.entityId = entityId;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.name = name;
            this.value = value;
            this.creationTime = creationTime;
        }

        public UUID getEntityId() {
            return entityId;
        }

        public String getWorld() {
            return world;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        public long getCreationTime() {
            return creationTime;
        }
    }
}