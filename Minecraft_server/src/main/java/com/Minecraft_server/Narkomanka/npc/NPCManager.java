package com.Minecraft_server.Narkomanka.npc;

import com.Minecraft_server.Narkomanka.Narkomanka;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class NPCManager {
    
    private final Narkomanka plugin;
    
    // Ключи для метаданных
    private final NamespacedKey npcTypeKey;
    private final NamespacedKey npcIdKey;
    
    // Списки NPC по типам
    private final Map<UUID, PoliceNPC> policeNPCs = new HashMap<>();
    private final Map<UUID, JunkieNPC> junkieNPCs = new HashMap<>();
    private final Map<UUID, CitizenNPC> citizenNPCs = new HashMap<>();
    
    // Розыск игроков
    private final Map<UUID, WantedLevel> playerWantedLevels = new HashMap<>();
    private final Map<UUID, Long> lastWantedTimeUpdate = new HashMap<>();
    
    // Районы города и их контроль
    private final Map<String, DistrictControl> districtControl = new HashMap<>();
    
    /**
     * Создает менеджер NPC
     */
    public NPCManager(Narkomanka plugin) {
        this.plugin = plugin;
        
        // Создаем ключи для метаданных
        this.npcTypeKey = new NamespacedKey(plugin, "npc_type");
        this.npcIdKey = new NamespacedKey(plugin, "npc_id");
        
        // Инициализация районов города
        initializeDistricts();
        
        // Запуск обработки поведения NPC
        startNPCBehavior();
        
        // Запуск обработки розыска
        startWantedLevelProcessor();
    }
    
    /**
     * Инициализирует районы города и их контроль
     */
    private void initializeDistricts() {
        String[] districts = {
            "Центральный район", "Южный район", "Северный район", "Западный район", "Восточный район",
            "Промзона", "Старый город", "Новостройки", "Речной район", "Университетский городок"
        };
        
        for (String district : districts) {
            // Случайное начальное распределение контроля
            double policeControl = ThreadLocalRandom.current().nextDouble(0.3, 0.8);
            double gangControl = 1.0 - policeControl;
            
            districtControl.put(district, new DistrictControl(district, policeControl, gangControl));
        }
        
        // Особые настройки для некоторых районов
        districtControl.get("Промзона").setPoliceControl(0.2);
        districtControl.get("Промзона").setGangControl(0.8);
        
        districtControl.get("Центральный район").setPoliceControl(0.8);
        districtControl.get("Центральный район").setGangControl(0.2);
    }
    
    /**
     * Запускает обработку поведения NPC
     */
    private void startNPCBehavior() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Обновляем поведение полицейских каждые 5 секунд
            for (PoliceNPC police : policeNPCs.values()) {
                police.updateBehavior();
            }
            
            // Обновляем поведение наркоманов каждые 5 секунд
            for (JunkieNPC junkie : junkieNPCs.values()) {
                junkie.updateBehavior();
            }
            
            // Обновляем поведение граждан каждые 5 секунд
            for (CitizenNPC citizen : citizenNPCs.values()) {
                citizen.updateBehavior();
            }
        }, 100L, 100L); // Каждые 5 секунд (100 тиков)
    }
    
    /**
     * Запускает обработку розыска игроков
     */
    private void startWantedLevelProcessor() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            
            // Для каждого игрока с уровнем розыска
            for (Iterator<Map.Entry<UUID, WantedLevel>> it = playerWantedLevels.entrySet().iterator(); it.hasNext();) {
                Map.Entry<UUID, WantedLevel> entry = it.next();
                UUID playerUuid = entry.getKey();
                WantedLevel wantedLevel = entry.getValue();
                
                // Если прошло достаточно времени, снижаем уровень розыска
                if (wantedLevel.getLevel() > 0 && currentTime - lastWantedTimeUpdate.getOrDefault(playerUuid, 0L) > getWantedDecayTime(wantedLevel)) {
                    wantedLevel.decreaseLevel();
                    lastWantedTimeUpdate.put(playerUuid, currentTime);
                    
                    // Оповещаем игрока, если он онлайн
                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player != null && player.isOnline()) {
                        if (wantedLevel.getLevel() > 0) {
                            player.sendMessage(ChatColor.YELLOW + "Ваш уровень розыска снизился до " + wantedLevel.getLevel() + " звезд.");
                        } else {
                            player.sendMessage(ChatColor.GREEN + "Полиция больше вас не разыскивает.");
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            it.remove();
                        }
                    } else if (wantedLevel.getLevel() == 0) {
                        it.remove();
                    }
                }
            }
        }, 1200L, 1200L); // Каждую минуту (1200 тиков)
    }
    
    /**
     * Возвращает время снижения уровня розыска в миллисекундах
     */
    private long getWantedDecayTime(WantedLevel wantedLevel) {
        switch (wantedLevel.getLevel()) {
            case 1: return 3 * 60 * 1000; // 3 минуты для 1 звезды
            case 2: return 5 * 60 * 1000; // 5 минут для 2 звезд
            case 3: return 8 * 60 * 1000; // 8 минут для 3 звезд
            case 4: return 12 * 60 * 1000; // 12 минут для 4 звезд
            case 5: return 20 * 60 * 1000; // 20 минут для 5 звезд
            default: return 3 * 60 * 1000;
        }
    }
    
    /**
     * Создает полицейского NPC
     */
    public PoliceNPC createPoliceNPC(Location location, String name) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        
        // Настройка внешнего вида
        villager.setProfession(Profession.NITWIT);
        villager.setCustomName(ChatColor.BLUE + "[Полиция] " + name);
        villager.setCustomNameVisible(true);
        villager.setAdult();
        villager.setInvulnerable(true);
        
        // Добавляем метаданные
        UUID npcId = UUID.randomUUID();
        villager.getPersistentDataContainer().set(npcTypeKey, PersistentDataType.STRING, "police");
        villager.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, npcId.toString());
        
        // Создаем и сохраняем объект NPC
        PoliceNPC policeNPC = new PoliceNPC(plugin, villager, npcId, name);
        policeNPCs.put(npcId, policeNPC);
        
        return policeNPC;
    }
    
    /**
     * Создает NPC наркомана
     */
    public JunkieNPC createJunkieNPC(Location location, String name) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        
        // Настройка внешнего вида
        villager.setProfession(Villager.Profession.NONE);
        villager.setCustomName(ChatColor.DARK_PURPLE + "[Наркоман] " + name);
        villager.setCustomNameVisible(true);
        villager.setAdult();
        
        // Добавляем эффект медлительности для имитации "под кайфом"
        villager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 1, false, false));
        
        // Добавляем метаданные
        UUID npcId = UUID.randomUUID();
        villager.getPersistentDataContainer().set(npcTypeKey, PersistentDataType.STRING, "junkie");
        villager.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, npcId.toString());
        
        // Создаем и сохраняем объект NPC
        JunkieNPC junkieNPC = new JunkieNPC(plugin, villager, npcId, name);
        junkieNPCs.put(npcId, junkieNPC);
        
        return junkieNPC;
    }
    
    /**
     * Создает NPC гражданина
     */
    public CitizenNPC createCitizenNPC(Location location, String name) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        
        // Выбираем случайную профессию
        Profession[] professions = {
            Profession.FARMER, Profession.LIBRARIAN, Profession.CLERIC, 
            Profession.ARMORER, Profession.BUTCHER, Profession.CARTOGRAPHER,
            Profession.LEATHERWORKER, Profession.MASON, Profession.SHEPHERD,
            Profession.TOOLSMITH, Profession.WEAPONSMITH
        };
        Profession profession = professions[ThreadLocalRandom.current().nextInt(professions.length)];
        
        // Настройка внешнего вида
        villager.setProfession(profession);
        villager.setCustomName(ChatColor.GREEN + "[Гражданин] " + name);
        villager.setCustomNameVisible(true);
        villager.setAdult();
        
        // Добавляем метаданные
        UUID npcId = UUID.randomUUID();
        villager.getPersistentDataContainer().set(npcTypeKey, PersistentDataType.STRING, "citizen");
        villager.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, npcId.toString());
        
        // Создаем и сохраняем объект NPC
        CitizenNPC citizenNPC = new CitizenNPC(plugin, villager, npcId, name);
        citizenNPCs.put(npcId, citizenNPC);
        
        return citizenNPC;
    }
    
    /**
     * Увеличивает уровень розыска игрока
     */
    public WantedLevel increaseWantedLevel(Player player, int amount, String reason) {
        UUID playerUuid = player.getUniqueId();
        WantedLevel wantedLevel = playerWantedLevels.getOrDefault(playerUuid, new WantedLevel());
        
        // Увеличиваем уровень розыска
        wantedLevel.increaseLevel(amount);
        
        // Обновляем время последнего обновления
        lastWantedTimeUpdate.put(playerUuid, System.currentTimeMillis());
        
        // Сохраняем уровень розыска
        playerWantedLevels.put(playerUuid, wantedLevel);
        
        // Уведомляем игрока
        notifyWantedLevelChange(player, wantedLevel, reason);
        
        // Если уровень розыска высокий, оповещаем полицейских NPC
        if (wantedLevel.getLevel() >= 3) {
            alertPoliceAboutPlayer(player);
        }
        
        return wantedLevel;
    }
    
    /**
     * Оповещает полицейских NPC о игроке в розыске
     */
    private void alertPoliceAboutPlayer(Player player) {
        Location playerLocation = player.getLocation();
        
        for (PoliceNPC policeNPC : policeNPCs.values()) {
            if (policeNPC.getEntity().getWorld().equals(playerLocation.getWorld()) && 
                policeNPC.getEntity().getLocation().distance(playerLocation) < 50) {
                policeNPC.pursuePlayer(player);
            }
        }
    }
    
    /**
     * Уведомляет игрока об изменении уровня розыска
     */
    private void notifyWantedLevelChange(Player player, WantedLevel wantedLevel, String reason) {
        int level = wantedLevel.getLevel();
        
        player.sendMessage("");
        
        if (level > 0) {
            player.sendMessage(ChatColor.RED + "=== ВНИМАНИЕ: РОЗЫСК ===");
            player.sendMessage(ChatColor.RED + "Уровень розыска: " + getWantedStars(level));
            
            if (reason != null && !reason.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Причина: " + reason);
            }
            
            player.sendMessage(ChatColor.YELLOW + "Будьте осторожны! Полиция будет искать вас!");
            player.sendMessage(ChatColor.RED + "=======================");
            
            // Звук сирены
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
        } else {
            player.sendMessage(ChatColor.GREEN + "Полиция больше вас не разыскивает.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
        player.sendMessage("");
    }
    
    /**
     * Возвращает строку со звездами уровня розыска
     */
    private String getWantedStars(int level) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < level; i++) {
            stars.append("★");
        }
        for (int i = level; i < 5; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }
    
    /**
     * Получает уровень розыска игрока
     */
    public WantedLevel getWantedLevel(UUID playerUuid) {
        return playerWantedLevels.getOrDefault(playerUuid, new WantedLevel());
    }
    
    /**
     * Получает контроль района
     */
    public DistrictControl getDistrictControl(String districtName) {
        return districtControl.getOrDefault(districtName, new DistrictControl(districtName, 0.5, 0.5));
    }
    
    /**
     * Обновляет контроль района
     */
    public void updateDistrictControl(String districtName, double policeControl, double gangControl) {
        DistrictControl district = districtControl.get(districtName);
        if (district != null) {
            district.setPoliceControl(policeControl);
            district.setGangControl(gangControl);
        }
    }
    
    /**
     * Находит ближайшего NPC наркомана к игроку
     */
    public JunkieNPC getNearestJunkie(Player player, double maxDistance) {
        JunkieNPC nearest = null;
        double nearestDistance = maxDistance;
        
        for (JunkieNPC junkie : junkieNPCs.values()) {
            if (!junkie.getEntity().getWorld().equals(player.getWorld())) continue;
            
            double distance = junkie.getEntity().getLocation().distance(player.getLocation());
            if (distance < nearestDistance) {
                nearest = junkie;
                nearestDistance = distance;
            }
        }
        
        return nearest;
    }
    
    /**
     * Находит ближайшего полицейского NPC к игроку
     */
    public PoliceNPC getNearestPolice(Player player, double maxDistance) {
        PoliceNPC nearest = null;
        double nearestDistance = maxDistance;
        
        for (PoliceNPC police : policeNPCs.values()) {
            if (!police.getEntity().getWorld().equals(player.getWorld())) continue;
            
            double distance = police.getEntity().getLocation().distance(player.getLocation());
            if (distance < nearestDistance) {
                nearest = police;
                nearestDistance = distance;
            }
        }
        
        return nearest;
    }
    
    /**
     * Арестовывает игрока
     */
    public void arrestPlayer(Player player, PoliceNPC police) {
        UUID playerUuid = player.getUniqueId();
        WantedLevel wantedLevel = getWantedLevel(playerUuid);
        
        // Отправляем сообщение об аресте
        player.sendMessage("");
        player.sendMessage(ChatColor.RED + "=== ВЫ АРЕСТОВАНЫ ===");
        player.sendMessage(ChatColor.RED + "Офицер " + police.getName() + " арестовал вас!");
        
        // Штраф зависит от уровня розыска
        double fine = calculateFine(wantedLevel.getLevel());
        
        // Снимаем штраф с игрока
        double cash = plugin.getPlayerService().getCashBalance(playerUuid);
        double cardBalance = plugin.getPlayerService().getCardBalance(playerUuid);
        
        if (cash >= fine) {
            plugin.getPlayerService().removeCash(playerUuid, fine);
            player.sendMessage(ChatColor.RED + "Штраф: $" + String.format("%.2f", fine) + " оплачен наличными.");
        } else if (cardBalance >= fine) {
            plugin.getPlayerService().removeCardBalance(playerUuid, fine);
            player.sendMessage(ChatColor.RED + "Штраф: $" + String.format("%.2f", fine) + " оплачен с банковской карты.");
        } else {
            // Если нет денег, конфискуем наркотики
            player.sendMessage(ChatColor.RED + "У вас нет денег для оплаты штрафа!");
            player.sendMessage(ChatColor.RED + "Полиция конфисковала все ваши наркотики!");
            
            // Удаляем все наркотики из инвентаря
            confiscateDrugs(player);
        }
        
        // Сбрасываем уровень розыска
        playerWantedLevels.remove(playerUuid);
        
        // Телепортируем в полицейский участок или на спавн
        // В Schedule I игрока обычно выпускают возле полицейского участка
        // Location spawnLocation = Bukkit.getWorld("world").getSpawnLocation();
        // player.teleport(spawnLocation);
        
        player.sendMessage(ChatColor.YELLOW + "Вас отпустили после уплаты штрафа.");
        player.sendMessage(ChatColor.RED + "===================");
        player.sendMessage("");
        
        // Звуковой эффект ареста
        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);
    }
    
    /**
     * Конфискует наркотики у игрока
     */
    private void confiscateDrugs(Player player) {
        // Типы наркотиков для конфискации
        Map<String, Material> drugTypes = new HashMap<>();
        drugTypes.put("marijuana", Material.DRIED_KELP);
        drugTypes.put("cocaine", Material.SUGAR);
        drugTypes.put("meth", Material.GLOWSTONE_DUST);
        drugTypes.put("heroin", Material.GUNPOWDER);
        
        // Проверяем каждый предмет в инвентаре
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            
            for (Map.Entry<String, Material> drugType : drugTypes.entrySet()) {
                if (item.getType() == drugType.getValue() && item.hasItemMeta()) {
                    // Проверяем метаданные, чтобы убедиться, что это наркотик
                    if (item.getItemMeta().getPersistentDataContainer().has(
                            new NamespacedKey(plugin, "drug_type"), PersistentDataType.STRING) && 
                        item.getItemMeta().getPersistentDataContainer().get(
                                new NamespacedKey(plugin, "drug_type"), PersistentDataType.STRING)
                                .equalsIgnoreCase(drugType.getKey())) {
                        // Удаляем наркотик
                        player.getInventory().remove(item);
                    }
                }
            }
        }
        
        // Обновляем инвентарь
        player.updateInventory();
    }
    
    /**
     * Рассчитывает штраф в зависимости от уровня розыска
     */
    private double calculateFine(int wantedLevel) {
        switch (wantedLevel) {
            case 1: return 250.0;
            case 2: return 500.0;
            case 3: return 1000.0;
            case 4: return 2000.0;
            case 5: return 4000.0;
            default: return 100.0;
        }
    }
    
    /**
     * Класс для хранения уровня розыска
     */
    public static class WantedLevel {
        private int level;
        
        public WantedLevel() {
            this.level = 0;
        }
        
        public int getLevel() {
            return level;
        }
        
        public void setLevel(int level) {
            this.level = Math.max(0, Math.min(5, level));
        }
        
        public void increaseLevel(int amount) {
            this.level = Math.min(5, this.level + amount);
        }
        
        public void decreaseLevel() {
            this.level = Math.max(0, this.level - 1);
        }
    }
    
    /**
     * Класс для хранения информации о контроле района
     */
    public static class DistrictControl {
        private final String name;
        private double policeControl;
        private double gangControl;
        
        public DistrictControl(String name, double policeControl, double gangControl) {
            this.name = name;
            this.policeControl = policeControl;
            this.gangControl = gangControl;
        }
        
        public String getName() {
            return name;
        }
        
        public double getPoliceControl() {
            return policeControl;
        }
        
        public void setPoliceControl(double policeControl) {
            this.policeControl = policeControl;
        }
        
        public double getGangControl() {
            return gangControl;
        }
        
        public void setGangControl(double gangControl) {
            this.gangControl = gangControl;
        }
        
        /**
         * Возвращает риск полицейского обнаружения
         */
        public double getPoliceDetectionRisk() {
            return policeControl;
        }
    }
    
    /**
     * Получает NPC наркомана по ID
     */
    public JunkieNPC getJunkieNPC(UUID npcId) {
        return junkieNPCs.get(npcId);
    }
    
    /**
     * Получает полицейского NPC по ID
     */
    public PoliceNPC getPoliceNPC(UUID npcId) {
        return policeNPCs.get(npcId);
    }
    
    /**
     * Получает NPC гражданина по ID
     */
    public CitizenNPC getCitizenNPC(UUID npcId) {
        return citizenNPCs.get(npcId);
    }
    
    /**
     * Создает и размещает NPC по всему миру
     * @param policeCount Количество полицейских
     * @param junkieCount Количество наркоманов
     * @param citizenCount Количество граждан
     */
    public void populateWorld(int policeCount, int junkieCount, int citizenCount) {
        // Имена для NPC
        String[] policeNames = {
            "Офицер Иванов", "Сержант Петров", "Капитан Сидоров", "Лейтенант Смирнов", 
            "Майор Волков", "Инспектор Морозов", "Офицер Соколов", "Сержант Лебедев"
        };
        
        String[] junkieNames = {
            "Макс", "Толик", "Серый", "Хмурый", "Длинный", "Шустрый", "Лысый", "Коля", 
            "Вася", "Глеб", "Витя", "Димон", "Леха", "Тоха", "Стас", "Женя"
        };
        
        String[] citizenNames = {
            "Анна", "Мария", "Елена", "Ольга", "Ирина", "Татьяна", "Светлана", "Екатерина",
            "Александр", "Дмитрий", "Михаил", "Сергей", "Андрей", "Владимир", "Игорь", "Виктор",
            "Николай", "Евгений", "Алексей", "Василий", "Федор", "Павел", "Егор", "Артем"
        };
        
        // Создаем полицейских в центральных и благополучных районах
        for (int i = 0; i < policeCount; i++) {
            String name = policeNames[i % policeNames.length];
            String district = getRandomPoliceDistrict();
            Location location = getRandomLocationInDistrict(district);
            
            if (location != null) {
                PoliceNPC police = createPoliceNPC(location, name);
                plugin.getLogger().info("Создан полицейский NPC " + name + " в районе " + district);
            }
        }
        
        // Создаем наркоманов в неблагополучных районах
        for (int i = 0; i < junkieCount; i++) {
            String name = junkieNames[i % junkieNames.length];
            String district = getRandomJunkieDistrict();
            Location location = getRandomLocationInDistrict(district);
            
            if (location != null) {
                JunkieNPC junkie = createJunkieNPC(location, name);
                plugin.getLogger().info("Создан NPC наркоман " + name + " в районе " + district);
            }
        }
        
        // Создаем граждан во всех районах
        for (int i = 0; i < citizenCount; i++) {
            String name = citizenNames[i % citizenNames.length];
            String district = getRandomDistrict();
            Location location = getRandomLocationInDistrict(district);
            
            if (location != null) {
                CitizenNPC citizen = createCitizenNPC(location, name);
                plugin.getLogger().info("Создан NPC гражданин " + name + " в районе " + district);
            }
        }
    }
    
    /**
     * Возвращает случайный район для полицейских
     */
    private String getRandomPoliceDistrict() {
        // Полицейские чаще встречаются в районах с высоким контролем полиции
        List<String> districts = new ArrayList<>();
        for (Map.Entry<String, DistrictControl> entry : districtControl.entrySet()) {
            if (entry.getValue().getPoliceControl() > 0.5) {
                // Добавляем район с большей вероятностью, если контроль полиции высокий
                int weight = (int)(entry.getValue().getPoliceControl() * 10);
                for (int i = 0; i < weight; i++) {
                    districts.add(entry.getKey());
                }
            } else {
                // Районы с низким контролем полиции имеют меньший шанс появления полицейских
                districts.add(entry.getKey());
            }
        }
        
        return districts.get(ThreadLocalRandom.current().nextInt(districts.size()));
    }
    
    /**
     * Возвращает случайный район для наркоманов
     */
    private String getRandomJunkieDistrict() {
        // Наркоманы чаще встречаются в районах с высоким контролем банд
        List<String> districts = new ArrayList<>();
        for (Map.Entry<String, DistrictControl> entry : districtControl.entrySet()) {
            if (entry.getValue().getGangControl() > 0.5) {
                // Добавляем район с большей вероятностью, если контроль банд высокий
                int weight = (int)(entry.getValue().getGangControl() * 10);
                for (int i = 0; i < weight; i++) {
                    districts.add(entry.getKey());
                }
            } else {
                // Районы с низким контролем банд имеют меньший шанс появления наркоманов
                districts.add(entry.getKey());
            }
        }
        
        return districts.get(ThreadLocalRandom.current().nextInt(districts.size()));
    }
    
    /**
     * Возвращает случайный район
     */
    private String getRandomDistrict() {
        List<String> districts = new ArrayList<>(districtControl.keySet());
        return districts.get(ThreadLocalRandom.current().nextInt(districts.size()));
    }
    
    /**
     * Возвращает случайную локацию в указанном районе
     */
    private Location getRandomLocationInDistrict(String district) {
        // В реальном плагине здесь была бы логика определения границ района
        // и выбора случайной точки внутри него
        // В данной реализации просто возвращаем случайную локацию в мире
        
        if (plugin.getServer().getWorlds().isEmpty()) {
            return null;
        }
        
        // Берем первый мир
        org.bukkit.World world = plugin.getServer().getWorlds().get(0);
        
        // Генерируем случайные координаты в пределах 200 блоков от центра
        double x = ThreadLocalRandom.current().nextDouble(-200, 200);
        double z = ThreadLocalRandom.current().nextDouble(-200, 200);
        
        // Находим верхний блок для координат (земля или другая поверхность)
        int y = world.getHighestBlockYAt((int)x, (int)z);
        
        return new Location(world, x, y + 1, z);
    }
} 