package com.Minecraft_server.Narkomanka.npc;

import com.Minecraft_server.Narkomanka.Narkomanka;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Класс для управления полицейскими NPC
 */
public class PoliceNPC {
    
    private final Narkomanka plugin;
    private final Villager entity;
    private final UUID npcId;
    private final String name;
    
    // Настройки поведения
    private Location patrolPoint;
    private Player targetPlayer;
    private PoliceState state = PoliceState.PATROL;
    private BukkitTask currentTask;
    private final Set<UUID> detectedPlayers = new HashSet<>();
    
    // Диалоги
    private final String[] suspiciousDialogs = {
        "Эй ты! Что ты там делаешь?",
        "Стоять! Полиция!",
        "Остановись для проверки!",
        "Эй, парень! Предъяви документы!",
        "Стой на месте! Не двигайся!"
    };
    
    private final String[] arrestDialogs = {
        "Ты арестован! Не сопротивляйся!",
        "На землю, руки за голову!",
        "Ты задержан! Имеешь право хранить молчание!",
        "Стой! Ты арестован именем закона!",
        "Не двигайся! Ты под арестом!"
    };
    
    /**
     * Создает полицейского NPC
     */
    public PoliceNPC(Narkomanka plugin, Villager entity, UUID npcId, String name) {
        this.plugin = plugin;
        this.entity = entity;
        this.npcId = npcId;
        this.name = name;
        this.patrolPoint = entity.getLocation().clone();
    }
    
    /**
     * Обновляет поведение NPC
     */
    public void updateBehavior() {
        if (entity == null || entity.isDead()) {
            return;
        }
        
        switch (state) {
            case PATROL:
                updatePatrolBehavior();
                break;
            case SUSPICIOUS:
                updateSuspiciousBehavior();
                break;
            case CHASE:
                updateChaseBehavior();
                break;
            case ARREST:
                updateArrestBehavior();
                break;
        }
    }
    
    /**
     * Обновляет поведение патрулирования
     */
    private void updatePatrolBehavior() {
        // Патрулирование - проверяем окружающих игроков
        if (ThreadLocalRandom.current().nextDouble() < 0.1) { // 10% шанс проверить окрестности
            checkNearbyPlayers();
        }
        
        // Случайное перемещение вокруг патрульной точки
        if (ThreadLocalRandom.current().nextDouble() < 0.2) { // 20% шанс сменить позицию
            double offsetX = ThreadLocalRandom.current().nextDouble(-5, 5);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-5, 5);
            
            Location newLocation = patrolPoint.clone().add(offsetX, 0, offsetZ);
            entity.getPathfinder().moveTo(newLocation);
        }
    }
    
    /**
     * Обновляет поведение при подозрительном игроке
     */
    private void updateSuspiciousBehavior() {
        if (targetPlayer == null || !targetPlayer.isOnline() || targetPlayer.isDead()) {
            state = PoliceState.PATROL;
            targetPlayer = null;
            return;
        }
        
        // Проверяем расстояние до игрока
        double distance = entity.getLocation().distance(targetPlayer.getLocation());
        
        if (distance > 30) {
            // Игрок слишком далеко, возвращаемся к патрулированию
            state = PoliceState.PATROL;
            targetPlayer = null;
        } else if (distance < 10) {
            // Если игрок близко, проверяем уровень розыска
            NPCManager npcManager = plugin.getNPCManager();
            int wantedLevel = npcManager.getWantedLevel(targetPlayer.getUniqueId()).getLevel();
            
            if (wantedLevel > 0) {
                // Если игрок в розыске, начинаем преследование
                state = PoliceState.CHASE;
                
                // Отправляем сообщение игроку
                targetPlayer.sendMessage(ChatColor.RED + "[Полиция " + name + "]: " + getRandomDialog(arrestDialogs));
                
                // Звук сирены
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.0f);
            } else {
                // Проверяем, держит ли игрок наркотики в руке
                if (isDrugItem(targetPlayer.getInventory().getItemInMainHand())) {
                    // Увеличиваем уровень розыска
                    npcManager.increaseWantedLevel(targetPlayer, 1, "Хранение наркотиков");
                    state = PoliceState.CHASE;
                } else {
                    // Возвращаемся к патрулированию
                    if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                        targetPlayer.sendMessage(ChatColor.BLUE + "[Полиция " + name + "]: Проходи, не задерживайся.");
                    }
                    state = PoliceState.PATROL;
                    targetPlayer = null;
                }
            }
        } else {
            // Движемся к игроку
            entity.getPathfinder().moveTo(targetPlayer.getLocation());
            
            // Отправляем сообщение
            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                targetPlayer.sendMessage(ChatColor.BLUE + "[Полиция " + name + "]: " + getRandomDialog(suspiciousDialogs));
                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
            }
        }
    }
    
    /**
     * Обновляет поведение при преследовании
     */
    private void updateChaseBehavior() {
        if (targetPlayer == null || !targetPlayer.isOnline() || targetPlayer.isDead()) {
            state = PoliceState.PATROL;
            targetPlayer = null;
            return;
        }
        
        // Проверяем расстояние до игрока
        double distance = entity.getLocation().distance(targetPlayer.getLocation());
        
        if (distance > 40) {
            // Игрок слишком далеко, возвращаемся к патрулированию
            state = PoliceState.PATROL;
            targetPlayer = null;
        } else if (distance < 3) {
            // Если игрок очень близко, арестовываем
            state = PoliceState.ARREST;
            
            // Отправляем сообщение игроку
            targetPlayer.sendMessage(ChatColor.RED + "[Полиция " + name + "]: " + getRandomDialog(arrestDialogs));
            
            // Звук сирены
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
            
            // Начинаем арест
            startArrest();
        } else {
            // Движемся к игроку
            entity.getPathfinder().moveTo(targetPlayer.getLocation());
            
            // Периодически отправляем сообщения
            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                targetPlayer.sendMessage(ChatColor.RED + "[Полиция " + name + "]: Стой, ты арестован!");
                
                // Звук погони
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.0f);
                }
            }
        }
    }
    
    /**
     * Обновляет поведение при аресте
     */
    private void updateArrestBehavior() {
        // Арест уже в процессе, ждем завершения
    }
    
    /**
     * Начинает процесс ареста
     */
    private void startArrest() {
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            state = PoliceState.PATROL;
            return;
        }
        
        // Отправляем сообщение о задержании
        targetPlayer.sendMessage(ChatColor.RED + "Офицер " + name + " задержал вас! Вы будете доставлены в участок.");
        
        // Звуковые эффекты
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.0f);
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);
        
        // Запускаем таймер ареста (3 секунды)
        if (currentTask != null) {
            currentTask.cancel();
        }
        
        currentTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (targetPlayer != null && targetPlayer.isOnline() && !targetPlayer.isDead()) {
                // Арестовываем игрока
                plugin.getNPCManager().arrestPlayer(targetPlayer, this);
            }
            
            // Возвращаемся к патрулированию
            state = PoliceState.PATROL;
            targetPlayer = null;
            currentTask = null;
        }, 60L); // 3 секунды (60 тиков)
    }
    
    /**
     * Проверяет игроков поблизости на подозрительную активность
     */
    private void checkNearbyPlayers() {
        double checkRadius = 15.0;
        
        for (Entity nearbyEntity : entity.getNearbyEntities(checkRadius, checkRadius, checkRadius)) {
            if (!(nearbyEntity instanceof Player)) continue;
            
            Player player = (Player) nearbyEntity;
            UUID playerUuid = player.getUniqueId();
            
            // Проверяем уровень розыска
            NPCManager npcManager = plugin.getNPCManager();
            int wantedLevel = npcManager.getWantedLevel(playerUuid).getLevel();
            
            if (wantedLevel > 0) {
                // Игрок в розыске
                double detectionChance = 0.3 * wantedLevel; // Шанс обнаружения зависит от уровня розыска
                
                if (ThreadLocalRandom.current().nextDouble() < detectionChance) {
                    targetPlayer = player;
                    state = PoliceState.CHASE;
                    
                    // Отправляем сообщение игроку
                    targetPlayer.sendMessage(ChatColor.RED + "[Полиция " + name + "]: Стой! Ты в розыске!");
                    targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.0f);
                    return;
                }
            } else if (isDrugItem(player.getInventory().getItemInMainHand())) {
                // Игрок держит наркотики в руке
                targetPlayer = player;
                state = PoliceState.SUSPICIOUS;
                
                // Отправляем сообщение игроку
                targetPlayer.sendMessage(ChatColor.BLUE + "[Полиция " + name + "]: " + getRandomDialog(suspiciousDialogs));
                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
                return;
            } else if (!detectedPlayers.contains(playerUuid) && ThreadLocalRandom.current().nextDouble() < 0.05) {
                // С небольшим шансом проверяем обычных игроков
                targetPlayer = player;
                state = PoliceState.SUSPICIOUS;
                detectedPlayers.add(playerUuid);
                
                // Отправляем сообщение игроку
                targetPlayer.sendMessage(ChatColor.BLUE + "[Полиция " + name + "]: " + getRandomDialog(suspiciousDialogs));
                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
                return;
            }
        }
    }
    
    /**
     * Начинает преследование игрока
     */
    public void pursuePlayer(Player player) {
        this.targetPlayer = player;
        this.state = PoliceState.CHASE;
        
        // Движемся к игроку
        entity.getPathfinder().moveTo(player.getLocation());
        
        // Отправляем сообщение
        player.sendMessage(ChatColor.RED + "[Полиция " + name + "]: Стой! Ты арестован!");
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.0f);
    }
    
    /**
     * Проверяет, является ли предмет наркотиком
     */
    private boolean isDrugItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        return item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "drug_type"), 
                org.bukkit.persistence.PersistentDataType.STRING);
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
    public PoliceState getState() {
        return state;
    }
    
    /**
     * Устанавливает точку патрулирования
     */
    public void setPatrolPoint(Location patrolPoint) {
        this.patrolPoint = patrolPoint;
    }
    
    /**
     * Перечисление состояний полицейского NPC
     */
    public enum PoliceState {
        PATROL,      // Патрулирование
        SUSPICIOUS,  // Подозрительный игрок
        CHASE,       // Преследование
        ARREST       // Арест
    }
} 