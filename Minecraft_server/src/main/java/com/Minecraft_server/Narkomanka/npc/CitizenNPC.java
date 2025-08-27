package com.Minecraft_server.Narkomanka.npc;

import com.Minecraft_server.Narkomanka.Narkomanka;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Класс для управления мирными жителями NPC
 */
public class CitizenNPC {
    
    private final Narkomanka plugin;
    private final Villager entity;
    private final UUID npcId;
    private final String name;
    
    // Настройки поведения
    private Location homeLocation;
    private Location workLocation;
    private CitizenState state = CitizenState.IDLE;
    private Player suspiciousPlayer;
    private BukkitTask currentTask;
    private boolean isFleeing = false;
    
    // Личностные характеристики (0.0 - 1.0)
    private final double bravery;        // Смелость (влияет на вероятность вызова полиции)
    private final double curiosity;      // Любопытство (влияет на желание наблюдать за подозрительной активностью)
    private final double friendliness;   // Дружелюбие (влияет на диалоги)
    
    // Диалоги
    private final String[] greetingDialogs = {
        "Добрый день!",
        "Здравствуйте!",
        "Привет!",
        "Приветствую!",
        "Хорошая сегодня погода, не правда ли?"
    };
    
    private final String[] suspiciousDialogs = {
        "Эй, что ты там делаешь?",
        "Это выглядит подозрительно...",
        "Что происходит?",
        "Я за тобой наблюдаю!",
        "Прекрати это немедленно!"
    };
    
    private final String[] fearDialogs = {
        "Помогите! Полиция!",
        "Боже мой! Вызовите полицию!",
        "Я вызываю полицию!",
        "Это преступление! Помогите!",
        "Стража! Здесь преступник!"
    };
    
    private final String[] fleeingDialogs = {
        "Ааа! Спасайся кто может!",
        "Бегите! Опасно!",
        "О нет! Я ухожу отсюда!",
        "Помогите! Спасите меня!",
        "Я не хочу умирать!"
    };
    
    /**
     * Создает мирного жителя NPC
     */
    public CitizenNPC(Narkomanka plugin, Villager entity, UUID npcId, String name) {
        this.plugin = plugin;
        this.entity = entity;
        this.npcId = npcId;
        this.name = name;
        this.homeLocation = entity.getLocation().clone();
        this.workLocation = entity.getLocation().clone();
        
        // Генерируем случайные личностные характеристики
        this.bravery = ThreadLocalRandom.current().nextDouble();
        this.curiosity = ThreadLocalRandom.current().nextDouble();
        this.friendliness = ThreadLocalRandom.current().nextDouble();
    }
    
    /**
     * Обновляет поведение NPC
     */
    public void updateBehavior() {
        if (entity == null || entity.isDead()) {
            return;
        }
        
        // Если NPC убегает, это приоритет
        if (isFleeing) {
            updateFleeingBehavior();
            return;
        }
        
        switch (state) {
            case IDLE:
                updateIdleBehavior();
                break;
            case WALKING:
                updateWalkingBehavior();
                break;
            case WORKING:
                updateWorkingBehavior();
                break;
            case SUSPICIOUS:
                updateSuspiciousBehavior();
                break;
        }
        
        // Проверяем окружающих игроков
        checkNearbyPlayers();
    }
    
    /**
     * Обновляет поведение в состоянии покоя
     */
    private void updateIdleBehavior() {
        // С некоторым шансом начинаем ходить
        if (ThreadLocalRandom.current().nextDouble() < 0.05) { // 5% шанс начать ходить
            state = CitizenState.WALKING;
            chooseRandomDestination();
        }
        
        // В некоторых случаях идем на работу
        long time = entity.getWorld().getTime();
        if (time > 1000 && time < 8000 && ThreadLocalRandom.current().nextDouble() < 0.1) { // День
            state = CitizenState.WALKING;
            entity.getPathfinder().moveTo(workLocation);
        }
        
        // Вечером идем домой
        if (time > 12000 && ThreadLocalRandom.current().nextDouble() < 0.2) { // Вечер
            state = CitizenState.WALKING;
            entity.getPathfinder().moveTo(homeLocation);
        }
    }
    
    /**
     * Обновляет поведение при ходьбе
     */
    private void updateWalkingBehavior() {
        // Если NPC пришел в пункт назначения или путь заблокирован
        if (!entity.getPathfinder().hasPath()) {
            if (ThreadLocalRandom.current().nextDouble() < 0.7) { // 70% шанс перейти в состояние покоя
                state = CitizenState.IDLE;
            } else {
                // Выбираем новую случайную точку
                chooseRandomDestination();
            }
        }
    }
    
    /**
     * Обновляет поведение при работе
     */
    private void updateWorkingBehavior() {
        // Работаем какое-то время, затем переходим к другим действиям
        if (ThreadLocalRandom.current().nextDouble() < 0.03) { // 3% шанс перестать работать
            state = CitizenState.IDLE;
        }
    }
    
    /**
     * Обновляет поведение при подозрительном игроке
     */
    private void updateSuspiciousBehavior() {
        if (suspiciousPlayer == null || !suspiciousPlayer.isOnline() || suspiciousPlayer.isDead()) {
            state = CitizenState.IDLE;
            suspiciousPlayer = null;
            return;
        }
        
        // Проверяем расстояние до игрока
        double distance = entity.getLocation().distance(suspiciousPlayer.getLocation());
        
        if (distance > 20) {
            // Игрок слишком далеко, возвращаемся к обычной деятельности
            state = CitizenState.IDLE;
            suspiciousPlayer = null;
        } else if (distance < 5) {
            // Игрок слишком близко, проверяем что он делает
            if (isHoldingDrugs(suspiciousPlayer)) {
                // С шансом, зависящим от храбрости, вызываем полицию или убегаем
                if (ThreadLocalRandom.current().nextDouble() < bravery) {
                    // Вызываем полицию
                    callPolice(suspiciousPlayer, "Подозрительное поведение");
                    
                    // Сообщаем об этом с некоторым шансом
                    if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                        suspiciousPlayer.sendMessage(ChatColor.RED + "[" + name + "]: " + getRandomDialog(fearDialogs));
                        suspiciousPlayer.playSound(suspiciousPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                } else {
                    // Убегаем от игрока
                    startFleeing(suspiciousPlayer);
                }
            }
        }
        
        // С определенным шансом высказываем подозрение
        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            // Высказываем подозрение
            suspiciousPlayer.sendMessage(ChatColor.YELLOW + "[" + name + "]: " + getRandomDialog(suspiciousDialogs));
            suspiciousPlayer.playSound(suspiciousPlayer.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
        }
    }
    
    /**
     * Обновляет поведение при бегстве
     */
    private void updateFleeingBehavior() {
        if (suspiciousPlayer == null || !suspiciousPlayer.isOnline() || suspiciousPlayer.isDead()) {
            isFleeing = false;
            state = CitizenState.IDLE;
            suspiciousPlayer = null;
            return;
        }
        
        // Проверяем расстояние до игрока
        double distance = entity.getLocation().distance(suspiciousPlayer.getLocation());
        
        if (distance > 30) {
            // Достаточно далеко убежали
            isFleeing = false;
            state = CitizenState.IDLE;
            suspiciousPlayer = null;
        } else {
            // Продолжаем убегать
            // Определяем направление от игрока
            Location playerLoc = suspiciousPlayer.getLocation();
            Location npcLoc = entity.getLocation();
            
            // Вектор от игрока к NPC
            double dx = npcLoc.getX() - playerLoc.getX();
            double dz = npcLoc.getZ() - playerLoc.getZ();
            
            // Нормализуем вектор
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 0) {
                dx = dx / length * 10; // Умножаем на 10, чтобы убегать подальше
                dz = dz / length * 10;
            }
            
            // Новая точка назначения - подальше от игрока
            Location target = npcLoc.clone().add(dx, 0, dz);
            entity.getPathfinder().moveTo(target);
            
            // Периодически высказываем страх
            if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                for (Entity nearbyEntity : entity.getNearbyEntities(10, 10, 10)) {
                    if (nearbyEntity instanceof Player) {
                        Player player = (Player) nearbyEntity;
                        player.sendMessage(ChatColor.RED + "[" + name + "]: " + getRandomDialog(fleeingDialogs));
                    }
                }
            }
        }
    }
    
    /**
     * Проверяет ближайших игроков
     */
    private void checkNearbyPlayers() {
        double checkRadius = 10.0;
        
        for (Entity nearbyEntity : entity.getNearbyEntities(checkRadius, checkRadius, checkRadius)) {
            if (!(nearbyEntity instanceof Player)) continue;
            
            Player player = (Player) nearbyEntity;
            
            // Проверяем уровень розыска
            if (plugin.getNPCManager() != null) {
                int wantedLevel = plugin.getNPCManager().getWantedLevel(player.getUniqueId()).getLevel();
                
                if (wantedLevel > 0) {
                    // Игрок разыскивается, реагируем в зависимости от храбрости
                    if (ThreadLocalRandom.current().nextDouble() < bravery) {
                        // Сообщаем о преступнике и вызываем полицию
                        player.sendMessage(ChatColor.RED + "[" + name + "]: " + getRandomDialog(fearDialogs));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        callPolice(player, "Разыскиваемый преступник");
                    } else {
                        // Убегаем от игрока
                        startFleeing(player);
                    }
                    return;
                }
            }
            
            // Проверяем, держит ли игрок наркотики в руке
            if (isHoldingDrugs(player)) {
                // С небольшим шансом замечаем наркотики
                if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                    suspiciousPlayer = player;
                    state = CitizenState.SUSPICIOUS;
                    
                    // С небольшим шансом говорим что-то
                    if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                        player.sendMessage(ChatColor.YELLOW + "[" + name + "]: " + getRandomDialog(suspiciousDialogs));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
                    }
                    return;
                }
            } else if (ThreadLocalRandom.current().nextDouble() < 0.05 * friendliness) {
                // С небольшим шансом, зависящим от дружелюбия, здороваемся
                player.sendMessage(ChatColor.GREEN + "[" + name + "]: " + getRandomDialog(greetingDialogs));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
            }
        }
    }
    
    /**
     * Вызывает полицию на игрока
     */
    private void callPolice(Player player, String reason) {
        if (plugin.getNPCManager() == null) return;
        
        // Увеличиваем уровень розыска
        plugin.getNPCManager().increaseWantedLevel(player, 1, reason);
        
        // Уведомляем игрока
        player.sendMessage(ChatColor.RED + "Гражданин " + name + " вызвал полицию!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        
        // Звук сирены вдалеке
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.0f);
    }
    
    /**
     * Начинает убегать от игрока
     */
    private void startFleeing(Player player) {
        suspiciousPlayer = player;
        isFleeing = true;
        
        // Отправляем сообщение о страхе
        player.sendMessage(ChatColor.RED + "[" + name + "]: " + getRandomDialog(fleeingDialogs));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1.0f, 1.0f);
        
        // Определяем направление бегства (от игрока)
        Location playerLoc = player.getLocation();
        Location npcLoc = entity.getLocation();
        
        // Вектор от игрока к NPC
        double dx = npcLoc.getX() - playerLoc.getX();
        double dz = npcLoc.getZ() - playerLoc.getZ();
        
        // Нормализуем вектор
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > 0) {
            dx = dx / length * 10; // Умножаем на 10, чтобы убегать подальше
            dz = dz / length * 10;
        }
        
        // Новая точка назначения - подальше от игрока
        Location target = npcLoc.clone().add(dx, 0, dz);
        entity.getPathfinder().moveTo(target);
    }
    
    /**
     * Выбирает случайную точку назначения
     */
    private void chooseRandomDestination() {
        double offsetX = ThreadLocalRandom.current().nextDouble(-10, 10);
        double offsetZ = ThreadLocalRandom.current().nextDouble(-10, 10);
        
        Location baseLocation;
        if (ThreadLocalRandom.current().nextBoolean()) {
            baseLocation = homeLocation;
        } else {
            baseLocation = workLocation;
        }
        
        Location newLocation = baseLocation.clone().add(offsetX, 0, offsetZ);
        entity.getPathfinder().moveTo(newLocation);
    }
    
    /**
     * Проверяет, держит ли игрок наркотики в руке
     */
    private boolean isHoldingDrugs(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        
        if (heldItem == null || !heldItem.hasItemMeta()) return false;
        
        return heldItem.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "drug_type"), 
                PersistentDataType.STRING);
    }
    
    /**
     * Возвращает случайный диалог из массива
     */
    private String getRandomDialog(String[] dialogs) {
        return dialogs[ThreadLocalRandom.current().nextInt(dialogs.length)];
    }
    
    /**
     * Получает Villager сущность
     */
    public Villager getEntity() {
        return entity;
    }
    
    /**
     * Получает имя NPC
     */
    public String getName() {
        return name;
    }
    
    /**
     * Получает уникальный идентификатор NPC
     */
    public UUID getNpcId() {
        return npcId;
    }
    
    /**
     * Получает текущее состояние NPC
     */
    public CitizenState getState() {
        return state;
    }
    
    /**
     * Устанавливает домашнюю локацию
     */
    public void setHomeLocation(Location homeLocation) {
        this.homeLocation = homeLocation;
    }
    
    /**
     * Устанавливает рабочую локацию
     */
    public void setWorkLocation(Location workLocation) {
        this.workLocation = workLocation;
    }
    
    /**
     * Перечисление состояний мирного жителя
     */
    public enum CitizenState {
        IDLE,        // Бездействие
        WALKING,     // Ходьба
        WORKING,     // Работа
        SUSPICIOUS   // Подозрительное поведение
    }
} 