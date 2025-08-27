package com.Minecraft_server.Narkomanka.npc;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerData;
import com.Minecraft_server.Narkomanka.npc.PhoneMissionManager.Mission;
import com.Minecraft_server.Narkomanka.ui.PhoneBoothMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PhoneBoothNPC implements Listener {

    private final Narkomanka plugin;
    private final NamespacedKey phoneBoothKey;
    private final Map<Location, ArmorStand> phoneBooths = new HashMap<>();
    private final Map<UUID, BukkitTask> ringingPhones = new HashMap<>();
    private final Random random = new Random();
    
    // Система миссий
    private final PhoneMissionManager missionManager;
    
    // Активные миссии игроков
    private final Map<UUID, List<Mission>> playerActiveMissions = new HashMap<>();

    // Меню телефонной будки
    private PhoneBoothMenu phoneBoothMenu;

    public PhoneBoothNPC(Narkomanka plugin) {
        this.plugin = plugin;
        this.phoneBoothKey = new NamespacedKey(plugin, "phone_booth");
        
        // Инициализируем менеджер миссий
        this.missionManager = new PhoneMissionManager(plugin);

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Создаем меню
        this.phoneBoothMenu = new PhoneBoothMenu(plugin, this);

        // Запускаем периодические звонки
        startRandomCalls();
    }

    /**
     * Запускает систему случайных звонков для игроков
     */
    private void startRandomCalls() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Если сейчас ночь, не генерировать звонки
            if (plugin.getDayNightCycleManager() != null && plugin.getDayNightCycleManager().isNightLocked()) {
                return;
            }

            // Проверяем каждого онлайн игрока
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Проверяем, есть ли у игрока уже активные миссии
                if (!playerHasActiveMission(player.getUniqueId()) && random.nextDouble() < 0.2) { // 20% шанс звонка
                    // Генерируем новый звонок с миссией
                    generateCallForPlayer(player);
                }
            }
        }, 2400L, 2400L); // Каждые 2 минуты (2400 тиков)
    }

    /**
     * Генерирует звонок с миссией для игрока
     */
    private void generateCallForPlayer(Player player) {
        // Получаем случайную миссию из менеджера
        Mission mission = missionManager.generateMission(player);

        // Получаем диалоги клиента
        String greeting = missionManager.getRandomClientGreeting(mission.getClientName());
        String request = missionManager.getRandomClientRequest(mission.getClientName(), mission.getItemType(), mission.getQuantity());

        // Создаем сообщение о звонке
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "======== ВХОДЯЩИЙ ЗВОНОК ========");
        player.sendMessage(ChatColor.GOLD + greeting);
        player.sendMessage(ChatColor.WHITE + request);
        player.sendMessage(ChatColor.GRAY + "Найдите телефонную будку или используйте телефон, чтобы ответить.");
        player.sendMessage(ChatColor.YELLOW + "================================");
        player.sendMessage("");

        // Запоминаем миссию для игрока
        if (!playerActiveMissions.containsKey(player.getUniqueId())) {
            playerActiveMissions.put(player.getUniqueId(), new ArrayList<>());
        }
        playerActiveMissions.get(player.getUniqueId()).add(mission);

        // Запускаем звуковой эффект телефона
        startPhoneRinging(player.getUniqueId());
    }

    /**
     * Запускает звуковой эффект звонящего телефона для игрока
     */
    private void startPhoneRinging(UUID playerUuid) {
        // Отменяем предыдущий звуковой эффект, если он был
        BukkitTask existingTask = ringingPhones.get(playerUuid);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Запускаем новый звуковой эффект с более реалистичными звуками телефона
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                // Воспроизводим звук звонка (чередуем звуки для реалистичности)
                if (System.currentTimeMillis() % 2000 < 1000) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                } else {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.8f);
                }
            }
        }, 0L, 10L); // Каждые 0.5 секунды

        // Сохраняем задачу
        ringingPhones.put(playerUuid, task);

        // Автоматически останавливаем звонок через 60 секунд
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stopPhoneRinging(playerUuid);

            // Сообщаем игроку, что он пропустил звонок
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.RED + "Вы пропустили звонок от клиента.");

                // Удаляем миссию, если игрок не ответил на звонок
                if (playerActiveMissions.containsKey(playerUuid)) {
                    List<Mission> missions = playerActiveMissions.get(playerUuid);
                    if (!missions.isEmpty()) {
                        // Снижаем репутацию с клиентом
                        Mission missedMission = missions.get(missions.size() - 1);
                        missionManager.updateReputation(playerUuid, missedMission.getClientName(), -1);
                        
                        missions.remove(missions.size() - 1);
                    }
                }
            }
        }, 1200L); // 60 секунд (1200 тиков)
    }

    /**
     * Останавливает звуковой эффект телефона
     */
    public void stopPhoneRinging(UUID playerUuid) {
        BukkitTask task = ringingPhones.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Проверяет, есть ли у игрока активные миссии
     */
    public boolean playerHasActiveMission(UUID playerUuid) {
        return playerActiveMissions.containsKey(playerUuid) && !playerActiveMissions.get(playerUuid).isEmpty();
    }

    /**
     * Получает активные миссии игрока
     */
    public List<Mission> getPlayerMissions(UUID playerUuid) {
        if (playerActiveMissions.containsKey(playerUuid)) {
            return playerActiveMissions.get(playerUuid);
        }
        return new ArrayList<>();
    }

    /**
     * Создает телефонную будку в указанном месте
     */
    public void createPhoneBooth(Location location) {
        // Устанавливаем блок железной двери как основу телефонной будки
        Block baseBlock = location.getBlock();
        baseBlock.setType(Material.IRON_DOOR);

        // Создаем стойку для брони, которая будет представлять телефонную будку
        ArmorStand boothStand = (ArmorStand) location.getWorld().spawnEntity(
                location.clone().add(0.5, 0, 0.5),
                EntityType.ARMOR_STAND
        );

        // Настраиваем стойку
        boothStand.setCustomName(ChatColor.AQUA + "Телефонная Будка");
        boothStand.setCustomNameVisible(true);
        boothStand.setVisible(true);
        boothStand.setSmall(false);
        boothStand.setArms(true);
        boothStand.setBasePlate(true);
        boothStand.setCanPickupItems(false);
        boothStand.setInvulnerable(true);

        // Добавляем "телефон" в руку
        boothStand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_TRAPDOOR));

        // Добавляем метаданные для идентификации
        boothStand.getPersistentDataContainer().set(
                phoneBoothKey,
                PersistentDataType.STRING,
                "phone_booth"
        );

        // Сохраняем будку в карте
        phoneBooths.put(location, boothStand);

        plugin.getLogger().info("Телефонная будка создана на координатах: " +
                location.getWorld().getName() + " " + location.getX() + " " + location.getY() + " " + location.getZ());
    }

    /**
     * Удаляет телефонную будку
     */
    public void removePhoneBooth(Location location) {
        ArmorStand boothStand = phoneBooths.remove(location);
        if (boothStand != null && !boothStand.isDead()) {
            boothStand.remove();
            location.getBlock().setType(Material.AIR);
            plugin.getLogger().info("Телефонная будка удалена с координат: " +
                    location.getWorld().getName() + " " + location.getX() + " " + location.getY() + " " + location.getZ());
        }
    }

    /**
     * Обработчик взаимодействия игрока с телефонной будкой
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Проверяем, что игрок взаимодействует с блоком правой рукой
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Проверяем, что блок является железной дверью (частью телефонной будки)
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.IRON_DOOR) {
            return;
        }

        // Проверяем, является ли этот блок зарегистрированной телефонной будкой
        Location location = clickedBlock.getLocation();
        if (!phoneBooths.containsKey(location)) {
            return;
        }

        // Отменяем стандартное действие
        event.setCancelled(true);

        // Открываем меню телефонной будки
        Player player = event.getPlayer();
        phoneBoothMenu.openMenu(player);

        // Если у игрока был звонок, останавливаем его
        stopPhoneRinging(player.getUniqueId());
    }

    /**
     * Принимает миссию телефонной будки
     */
    public boolean acceptMission(Player player, int missionIndex) {
        UUID playerUuid = player.getUniqueId();
        List<Mission> missions = playerActiveMissions.get(playerUuid);
        
        if (missions == null || missions.isEmpty() || missionIndex >= missions.size()) {
            player.sendMessage(ChatColor.RED + "Нет доступных миссий с таким индексом.");
            return false;
        }
        
        Mission mission = missions.get(missionIndex);
        
        // Принимаем миссию
        boolean success = missionManager.acceptMission(player, mission);
        
        // Если успешно принята, удаляем из списка активных
        if (success) {
            missions.remove(missionIndex);
            
            // Отправляем сообщение об успешном принятии миссии
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "Вы приняли задание от " + mission.getClientName() + ".");
            player.sendMessage(ChatColor.GREEN + "Детали миссии добавлены в ваш телефон.");
            player.sendMessage("");
            
            // Воспроизводим звук подтверждения
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            // Отправляем сообщение об ошибке
            player.sendMessage(ChatColor.RED + "У вас недостаточно места в инвентаре для получения деталей миссии.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
        
        return success;
    }
    
    /**
     * Отклоняет миссию телефонной будки
     */
    public boolean declineMission(Player player, int missionIndex) {
        UUID playerUuid = player.getUniqueId();
        List<Mission> missions = playerActiveMissions.get(playerUuid);
        
        if (missions == null || missions.isEmpty() || missionIndex >= missions.size()) {
            player.sendMessage(ChatColor.RED + "Нет доступных миссий с таким индексом.");
            return false;
        }
        
        Mission mission = missions.get(missionIndex);
        String clientName = mission.getClientName();
        
        // Снижаем репутацию с клиентом при отказе от миссии
        missionManager.updateReputation(playerUuid, clientName, -1);
        
        // Получаем сообщение о неудаче от клиента
        String failureMessage = missionManager.getRandomFailureMessage(clientName);
        
        // Удаляем миссию из списка активных
        missions.remove(missionIndex);
        
        // Отправляем сообщение об отклонении
        player.sendMessage("");
        player.sendMessage(ChatColor.RED + "Вы отклонили задание от " + clientName + ".");
        player.sendMessage(ChatColor.YELLOW + clientName + ": " + ChatColor.WHITE + failureMessage);
        player.sendMessage(ChatColor.GRAY + "Ваша репутация с этим клиентом снизилась.");
        player.sendMessage("");
        
        // Воспроизводим звук отклонения
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        
        return true;
    }

    /**
     * Возвращает менеджер миссий
     */
    public PhoneMissionManager getMissionManager() {
        return missionManager;
    }
    
    /**
     * Симулирует звонок для тестирования
     */
    public void simulateCall(Player player) {
        generateCallForPlayer(player);
    }

    /**
     * Симулирует телефонный звонок для игрока
     * @param player Игрок, которому звонят
     */
    public void simulatePhoneCall(Player player) {
        // Создаем миссию
        Mission mission = missionManager.generateMission(player);
        
        // Получаем диалоги клиента
        String greeting = missionManager.getRandomClientGreeting(mission.getClientName());
        String request = missionManager.getRandomClientRequest(mission.getClientName(), mission.getItemType(), mission.getQuantity());
        
        // Добавляем миссию в список активных для игрока
        UUID playerUuid = player.getUniqueId();
        List<Mission> missions = playerActiveMissions.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        missions.add(mission);
        
        // Отправляем сообщение и звук
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "======== ВХОДЯЩИЙ ЗВОНОК ========");
        player.sendMessage(ChatColor.GOLD + greeting);
        player.sendMessage(ChatColor.WHITE + request);
        player.sendMessage(ChatColor.GRAY + "Используйте телефонную будку или телефон, чтобы принять или отклонить задание.");
        player.sendMessage(ChatColor.YELLOW + "================================");
        player.sendMessage("");
        
        // Воспроизводим звук телефона
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
        
        // Повторяем звук через секунду
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
        }, 20L);
    }
}