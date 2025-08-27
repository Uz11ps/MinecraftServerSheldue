package com.Minecraft_server.Narkomanka.npc;

import com.Minecraft_server.Narkomanka.Narkomanka;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Класс для управления NPC наркоманами
 */
public class JunkieNPC {
    
    private final Narkomanka plugin;
    private final Villager entity;
    private final UUID npcId;
    private final String name;
    
    // Настройки поведения
    private Location wanderPoint;
    private JunkieState state = JunkieState.IDLE;
    private Player interactingPlayer;
    private long lastInteractionTime;
    private boolean isHighOnDrugs = false;
    
    // Предпочтения наркотиков
    private final Map<String, Double> drugPreferences = new HashMap<>();
    private String preferredDrug;
    
    // Цены на наркотики (за единицу)
    private final Map<String, Double> drugPrices = new HashMap<>();
    
    // Диалоги
    private final String[] greetingDialogs = {
        "Эй, друг... У тебя есть... ты знаешь что?",
        "Чувак, мне очень нужно... сам понимаешь...",
        "Привет... У тебя есть что-нибудь? Я в отчаянии...",
        "Братан, помоги... Мне нужна доза...",
        "Эй, ты. Я слышал, ты можешь помочь с моей... проблемой?"
    };
    
    private final String[] buyingDialogs = {
        "Отлично, вот деньги. Давай быстрее!",
        "Наконец-то! Вот, возьми деньги, просто дай мне это.",
        "Ты спаситель! Держи бабки, давай товар.",
        "Ох, как я ждал этого... Вот деньги, только быстрее!",
        "Спасибо, чувак! Вот твоя оплата, не подведи."
    };
    
    private final String[] rejectionDialogs = {
        "Ты шутишь? Мне нужен товар получше!",
        "Нет, это не то, что мне нужно...",
        "Ты что, издеваешься? Я ищу нормальный товар!",
        "Это не поможет мне... Найди что-нибудь получше.",
        "Нет-нет, я не буду это брать. Найди настоящий товар!"
    };
    
    private final String[] highDialogs = {
        "Вау... Всё так... красиво...",
        "Я вижу... вижу цвета...",
        "Ты когда-нибудь видел, как летают стены?",
        "Ха-ха-ха... мне так хорошо...",
        "Жизнь прекрааааасна..."
    };
    
    /**
     * Создает NPC наркомана
     */
    public JunkieNPC(Narkomanka plugin, Villager entity, UUID npcId, String name) {
        this.plugin = plugin;
        this.entity = entity;
        this.npcId = npcId;
        this.name = name;
        this.wanderPoint = entity.getLocation().clone();
        this.lastInteractionTime = System.currentTimeMillis();
        
        // Инициализируем предпочтения наркотиков
        initDrugPreferences();
        
        // Инициализируем цены на наркотики
        initDrugPrices();
    }
    
    /**
     * Инициализирует предпочтения наркотиков для наркомана
     */
    private void initDrugPreferences() {
        // Базовые шансы предпочтения для разных наркотиков
        drugPreferences.put("marijuana", ThreadLocalRandom.current().nextDouble(0.1, 0.9));
        drugPreferences.put("cocaine", ThreadLocalRandom.current().nextDouble(0.1, 0.9));
        drugPreferences.put("meth", ThreadLocalRandom.current().nextDouble(0.1, 0.9));
        drugPreferences.put("heroin", ThreadLocalRandom.current().nextDouble(0.1, 0.9));
        
        // Выбираем предпочитаемый наркотик
        double maxPreference = 0;
        for (Map.Entry<String, Double> entry : drugPreferences.entrySet()) {
            if (entry.getValue() > maxPreference) {
                maxPreference = entry.getValue();
                preferredDrug = entry.getKey();
            }
        }
        
        // Усиливаем предпочтение для основного наркотика
        drugPreferences.put(preferredDrug, 1.0);
    }
    
    /**
     * Инициализирует цены на наркотики
     */
    private void initDrugPrices() {
        // Базовые цены с вариацией
        drugPrices.put("marijuana", 15.0 + ThreadLocalRandom.current().nextDouble(-3, 3));
        drugPrices.put("cocaine", 50.0 + ThreadLocalRandom.current().nextDouble(-5, 5));
        drugPrices.put("meth", 40.0 + ThreadLocalRandom.current().nextDouble(-4, 4));
        drugPrices.put("heroin", 60.0 + ThreadLocalRandom.current().nextDouble(-6, 6));
        
        // Повышаем цену на предпочитаемый наркотик (готов платить больше)
        if (preferredDrug != null) {
            drugPrices.put(preferredDrug, drugPrices.get(preferredDrug) * 1.2);
        }
    }
    
    /**
     * Обновляет поведение NPC
     */
    public void updateBehavior() {
        if (entity == null || entity.isDead()) {
            return;
        }
        
        // Если наркоман под кайфом, показываем соответствующее поведение
        if (isHighOnDrugs) {
            updateHighBehavior();
            return;
        }
        
        switch (state) {
            case IDLE:
                updateIdleBehavior();
                break;
            case FOLLOWING:
                updateFollowingBehavior();
                break;
            case INTERACTING:
                updateInteractingBehavior();
                break;
        }
    }
    
    /**
     * Обновляет поведение в состоянии простоя
     */
    private void updateIdleBehavior() {
        // Случайные перемещения
        if (ThreadLocalRandom.current().nextDouble() < 0.1) { // 10% шанс сменить позицию
            double offsetX = ThreadLocalRandom.current().nextDouble(-5, 5);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-5, 5);
            
            Location newLocation = wanderPoint.clone().add(offsetX, 0, offsetZ);
            entity.getPathfinder().moveTo(newLocation);
        }
        
        // Проверяем ближайших игроков
        checkNearbyPlayers();
    }
    
    /**
     * Обновляет поведение при следовании за игроком
     */
    private void updateFollowingBehavior() {
        if (interactingPlayer == null || !interactingPlayer.isOnline() || interactingPlayer.isDead()) {
            state = JunkieState.IDLE;
            interactingPlayer = null;
            return;
        }
        
        // Проверяем расстояние до игрока
        double distance = entity.getLocation().distance(interactingPlayer.getLocation());
        
        if (distance > 15) {
            // Игрок слишком далеко, возвращаемся к блужданию
            state = JunkieState.IDLE;
            interactingPlayer = null;
        } else if (distance < 3) {
            // Игрок рядом, начинаем взаимодействие
            state = JunkieState.INTERACTING;
            offerToBuyDrugs();
        } else {
            // Движемся к игроку
            entity.getPathfinder().moveTo(interactingPlayer.getLocation());
            
            // Периодически отправляем сообщения
            if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                interactingPlayer.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "]: " + getRandomDialog(greetingDialogs));
                interactingPlayer.playSound(interactingPlayer.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 0.8f);
            }
        }
    }
    
    /**
     * Обновляет поведение при взаимодействии с игроком
     */
    private void updateInteractingBehavior() {
        if (interactingPlayer == null || !interactingPlayer.isOnline() || interactingPlayer.isDead()) {
            state = JunkieState.IDLE;
            interactingPlayer = null;
            return;
        }
        
        // Проверяем расстояние до игрока
        double distance = entity.getLocation().distance(interactingPlayer.getLocation());
        
        if (distance > 5) {
            // Игрок отошел, следуем за ним
            state = JunkieState.FOLLOWING;
        } else {
            // Периодически предлагаем купить наркотики
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInteractionTime > 10000) { // Каждые 10 секунд
                offerToBuyDrugs();
                lastInteractionTime = currentTime;
            }
        }
    }
    
    /**
     * Обновляет поведение наркомана под кайфом
     */
    private void updateHighBehavior() {
        // Случайные движения
        if (ThreadLocalRandom.current().nextDouble() < 0.2) {
            double offsetX = ThreadLocalRandom.current().nextDouble(-2, 2);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-2, 2);
            
            Location newLocation = entity.getLocation().clone().add(offsetX, 0, offsetZ);
            entity.getPathfinder().moveTo(newLocation);
        }
        
        // Случайные реплики о кайфе
        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            for (Entity nearbyEntity : entity.getNearbyEntities(5, 5, 5)) {
                if (nearbyEntity instanceof Player) {
                    Player player = (Player) nearbyEntity;
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "[" + name + "]: " + getRandomDialog(highDialogs));
                }
            }
        }
        
        // Со временем эффект проходит
        if (ThreadLocalRandom.current().nextDouble() < 0.005) { // 0.5% шанс каждое обновление
            isHighOnDrugs = false;
            for (Entity nearbyEntity : entity.getNearbyEntities(5, 5, 5)) {
                if (nearbyEntity instanceof Player) {
                    Player player = (Player) nearbyEntity;
                    player.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "]: Эх... Кайф закончился... Нужно еще...");
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
            
            // Проверяем, держит ли игрок наркотики в руке
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            
            if (isDrugItem(heldItem)) {
                // Игрок держит наркотики, подходим к нему
                interactingPlayer = player;
                state = JunkieState.FOLLOWING;
                
                // Отправляем сообщение
                player.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "]: " + getRandomDialog(greetingDialogs));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 0.8f);
                return;
            }
        }
    }
    
    /**
     * Предлагает игроку продать наркотики
     */
    private void offerToBuyDrugs() {
        if (interactingPlayer == null) return;
        
        // Проверяем наркотики в руке игрока
        ItemStack heldItem = interactingPlayer.getInventory().getItemInMainHand();
        
        if (isDrugItem(heldItem)) {
            String drugType = getDrugType(heldItem);
            
            // Предлагаем цену в зависимости от типа наркотика и количества
            if (drugPreferences.containsKey(drugType)) {
                double pricePerUnit = drugPrices.getOrDefault(drugType, 20.0);
                int quantity = heldItem.getAmount();
                double totalPrice = pricePerUnit * quantity;
                
                // Если это предпочитаемый наркотик, сообщаем об этом
                if (drugType.equals(preferredDrug)) {
                    interactingPlayer.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "]: " + "О, " + getDrugDisplayName(drugType) + "! Как раз то, что я искал!");
                }
                
                // Предлагаем купить
                interactingPlayer.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "]: " + "Я дам тебе $" + String.format("%.2f", totalPrice) + " за " + quantity + " " + getDrugDisplayName(drugType) + ". По рукам?");
                interactingPlayer.sendMessage(ChatColor.GRAY + "Нажмите ПКМ по NPC с наркотиком в руке, чтобы продать.");
                
                // Звуковой эффект
                interactingPlayer.playSound(interactingPlayer.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
            } else {
                // Не покупаем этот тип наркотиков
                interactingPlayer.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "]: " + getRandomDialog(rejectionDialogs));
                interactingPlayer.playSound(interactingPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        } else {
            // Напоминаем, что хотим купить наркотики
            interactingPlayer.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "]: " + "У тебя есть " + getDrugDisplayName(preferredDrug) + "? Мне очень нужно...");
        }
    }
    
    /**
     * Обрабатывает продажу наркотиков
     */
    public void handleDrugSale(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        
        if (!isDrugItem(heldItem)) {
            player.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "]: " + "Ты что пытаешься мне подсунуть? Мне нужны настоящие вещества!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        String drugType = getDrugType(heldItem);
        
        if (!drugPreferences.containsKey(drugType)) {
            player.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "]: " + "Нет, это не то, что мне нужно.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Рассчитываем цену
        double pricePerUnit = drugPrices.getOrDefault(drugType, 20.0);
        int quantity = heldItem.getAmount();
        double totalPrice = pricePerUnit * quantity;
        
        // Проверяем на полицию поблизости
        if (isPoliceNearby(player)) {
            // Есть полиция рядом, увеличиваем уровень розыска
            player.sendMessage(ChatColor.RED + "Полицейский заметил вашу сделку с наркотиками!");
            plugin.getNPCManager().increaseWantedLevel(player, 2, "Продажа наркотиков");
            return;
        }
        
        // Выплачиваем деньги
        plugin.getPlayerService().addCash(player.getUniqueId(), totalPrice);
        
        // Удаляем наркотики из инвентаря
        player.getInventory().setItemInMainHand(null);
        
        // Отправляем сообщение
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "=== ПРОДАЖА НАРКОТИКОВ ===");
        player.sendMessage(ChatColor.GREEN + "Вы продали " + quantity + " " + getDrugDisplayName(drugType) + " за $" + String.format("%.2f", totalPrice));
        player.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "]: " + getRandomDialog(buyingDialogs));
        player.sendMessage(ChatColor.GREEN + "=======================");
        player.sendMessage("");
        
        // Отправляем звуковой эффект
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
        // Обновляем прогресс квестов на продажу наркотиков
        if (plugin.isDatabaseAvailable()) {
            plugin.getQuestService().notifyDrugSale(player.getUniqueId(), drugType, quantity);
        }
        
        // Наркоман получает наркотики и становится под кайфом
        isHighOnDrugs = true;
        state = JunkieState.IDLE;
        interactingPlayer = null;
    }
    
    /**
     * Проверяет, есть ли полиция поблизости
     */
    private boolean isPoliceNearby(Player player) {
        double checkRadius = 20.0;
        
        for (Entity nearbyEntity : player.getNearbyEntities(checkRadius, checkRadius, checkRadius)) {
            if (nearbyEntity.getPersistentDataContainer().has(
                    new NamespacedKey(plugin, "npc_type"), PersistentDataType.STRING)) {
                
                String npcType = nearbyEntity.getPersistentDataContainer().get(
                        new NamespacedKey(plugin, "npc_type"), PersistentDataType.STRING);
                
                if ("police".equals(npcType)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Проверяет, является ли предмет наркотиком
     */
    private boolean isDrugItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        return item.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey(plugin, "drug_type"), 
                PersistentDataType.STRING);
    }
    
    /**
     * Получает тип наркотика из предмета
     */
    private String getDrugType(ItemStack item) {
        if (!isDrugItem(item)) return null;
        
        return item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(plugin, "drug_type"), 
                PersistentDataType.STRING);
    }
    
    /**
     * Возвращает отображаемое имя наркотика
     */
    private String getDrugDisplayName(String drugType) {
        switch (drugType.toLowerCase()) {
            case "marijuana":
                return "травка";
            case "cocaine":
                return "кокаин";
            case "meth":
                return "мет";
            case "heroin":
                return "герыч";
            default:
                return drugType;
        }
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
    public JunkieState getState() {
        return state;
    }
    
    /**
     * Устанавливает точку блуждания
     */
    public void setWanderPoint(Location wanderPoint) {
        this.wanderPoint = wanderPoint;
    }
    
    /**
     * Перечисление состояний наркомана NPC
     */
    public enum JunkieState {
        IDLE,         // Бездействие/блуждание
        FOLLOWING,    // Следование за игроком
        INTERACTING   // Взаимодействие с игроком
    }
} 