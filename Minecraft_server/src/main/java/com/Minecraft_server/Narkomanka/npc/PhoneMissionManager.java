package com.Minecraft_server.Narkomanka.npc;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerData;
import com.Minecraft_server.Narkomanka.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PhoneMissionManager {

    private final Narkomanka plugin;
    private final NamespacedKey missionItemKey;

    // Карта игроков и их текущей репутации с разными клиентами
    private final Map<UUID, Map<String, Integer>> playerReputation = new HashMap<>();
    
    // Категории миссий
    private final List<MissionCategory> missionCategories = new ArrayList<>();
    
    // Локации для доставки (будут генерироваться на лету)
    private final List<Location> deliveryLocations = new ArrayList<>();
    
    // Случайные имена клиентов
    private final String[] clientNames = {
        "Александр", "Иван", "Михаил", "Дмитрий", "Сергей", 
        "Андрей", "Максим", "Владимир", "Павел", "Николай",
        "Юрий", "Василий", "Артем", "Роман", "Олег",
        "Анна", "Елена", "Мария", "Ольга", "Татьяна",
        "Наталья", "Екатерина", "Ирина", "Светлана", "Юлия"
    };
    
    // Районы города
    private final String[] districtNames = {
        "Центральный район", "Южный район", "Северный район", "Западный район", "Восточный район",
        "Промзона", "Старый город", "Новостройки", "Речной район", "Университетский городок"
    };

    // Диалоги клиентов при звонке
    private final Map<String, List<String>> clientGreetings = new HashMap<>();
    private final Map<String, List<String>> clientRequests = new HashMap<>();
    private final Map<String, List<String>> clientSuccessMessages = new HashMap<>();
    private final Map<String, List<String>> clientFailureMessages = new HashMap<>();

    public PhoneMissionManager(Narkomanka plugin) {
        this.plugin = plugin;
        this.missionItemKey = new NamespacedKey(plugin, "mission_item");
        
        // Инициализируем категории миссий
        initMissionCategories();
        
        // Запускаем периодическое обновление репутации
        startReputationCooldown();

        // Инициализируем диалоги клиентов
        initializeDialogs();
    }
    
    /**
     * Инициализирует категории миссий
     */
    private void initMissionCategories() {
        // Базовые миссии - доступны сразу
        MissionCategory basicCategory = new MissionCategory("basic", 0);
        
        // Доставка марихуаны
        basicCategory.addMission(new Mission(
            "marijuana_delivery_small",
            "Доставка марихуаны",
            "Клиент хочет небольшое количество марихуаны.",
            MissionType.DELIVERY,
            "marijuana",
            2,
            150.0,
            50.0,
            0.15,
            null
        ));
        
        basicCategory.addMission(new Mission(
            "marijuana_delivery_medium",
            "Доставка на вечеринку",
            "Группа друзей собирается на вечеринку и им нужна травка.",
            MissionType.DELIVERY,
            "marijuana",
            5,
            300.0,
            100.0,
            0.2,
            null
        ));
        
        // Доставка мелких партий кокаина
        basicCategory.addMission(new Mission(
            "cocaine_delivery_small",
            "Экспресс-доставка",
            "Бизнесмен срочно запрашивает небольшую партию.",
            MissionType.DELIVERY,
            "cocaine",
            1,
            300.0,
            150.0,
            0.25,
            null
        ));
        
        // Сбор мусора - простая миссия для начала
        basicCategory.addMission(new Mission(
            "trash_collection",
            "Уборка территории",
            "Клиент хочет, чтобы вы убрали мусор в определенном районе.",
            MissionType.TRASH,
            "trash",
            10,
            100.0,
            50.0,
            0.05,
            null
        ));
        
        missionCategories.add(basicCategory);
        
        // Продвинутые миссии - требуют репутацию
        MissionCategory advancedCategory = new MissionCategory("advanced", 10);
        
        // Крупные партии марихуаны
        advancedCategory.addMission(new Mission(
            "marijuana_delivery_large",
            "Крупный заказ",
            "Постоянный клиент заказывает большую партию товара.",
            MissionType.DELIVERY,
            "marijuana",
            10,
            600.0,
            200.0,
            0.25,
            Arrays.asList("marijuana_delivery_small", "marijuana_delivery_medium")
        ));
        
        // Средние партии кокаина
        advancedCategory.addMission(new Mission(
            "cocaine_delivery_medium",
            "VIP-вечеринка",
            "Элитная вечеринка в частном секторе требует качественный товар.",
            MissionType.DELIVERY,
            "cocaine",
            3,
            800.0,
            300.0,
            0.3,
            Arrays.asList("cocaine_delivery_small")
        ));
        
        // Миссии по выращиванию
        advancedCategory.addMission(new Mission(
            "grow_marijuana",
            "Вырастить партию",
            "Клиенту нужна свежая марихуана высокого качества.",
            MissionType.GROW,
            "marijuana",
            3,
            500.0,
            200.0,
            0.15,
            Arrays.asList("marijuana_delivery_medium")
        ));
        
        missionCategories.add(advancedCategory);
        
        // Экспертные миссии - требуют высокую репутацию
        MissionCategory expertCategory = new MissionCategory("expert", 25);
        
        // Крупные партии кокаина
        expertCategory.addMission(new Mission(
            "cocaine_delivery_large",
            "Особый клиент",
            "Влиятельная персона запрашивает большую партию высшего качества.",
            MissionType.DELIVERY,
            "cocaine",
            8,
            1500.0,
            500.0,
            0.35,
            Arrays.asList("cocaine_delivery_medium")
        ));
        
        // Миссии с метамфетамином
        expertCategory.addMission(new Mission(
            "meth_delivery",
            "Срочный заказ",
            "Группа зависимых нуждается в метамфетамине, хорошо платят.",
            MissionType.DELIVERY,
            "meth",
            5,
            1200.0,
            400.0,
            0.4,
            null
        ));
        
        // Миссии с героином
        expertCategory.addMission(new Mission(
            "heroin_delivery",
            "Высший уровень",
            "Загадочный клиент ищет героин. Очень рискованно, но выгодно.",
            MissionType.DELIVERY,
            "heroin",
            3,
            2000.0,
            800.0,
            0.5,
            null
        ));
        
        missionCategories.add(expertCategory);
    }
    
    /**
     * Запускает таймер для восстановления репутации
     */
    private void startReputationCooldown() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Для каждого игрока восстанавливаем репутацию
            for (Map<String, Integer> reputationMap : playerReputation.values()) {
                for (Map.Entry<String, Integer> entry : reputationMap.entrySet()) {
                    if (entry.getValue() < 0) {
                        // Восстанавливаем негативную репутацию
                        reputationMap.put(entry.getKey(), Math.min(0, entry.getValue() + 1));
                    }
                }
            }
        }, 72000L, 72000L); // Каждый час игрового времени (3600 секунд * 20 тиков)
    }
    
    /**
     * Генерирует миссию для игрока
     */
    public Mission generateMission(Player player) {
        UUID playerUuid = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerService().getOrCreatePlayerData(player);
        
        // Получаем репутацию игрока
        Map<String, Integer> reputationMap = playerReputation.computeIfAbsent(playerUuid, k -> new HashMap<>());
        
        // Выбираем категорию миссий в зависимости от репутации
        List<MissionCategory> availableCategories = new ArrayList<>();
        int totalReputation = getTotalReputation(reputationMap);
        
        for (MissionCategory category : missionCategories) {
            if (totalReputation >= category.getRequiredReputation()) {
                availableCategories.add(category);
            }
        }
        
        if (availableCategories.isEmpty()) {
            availableCategories.add(missionCategories.get(0)); // Всегда должна быть доступна базовая категория
        }
        
        // Выбираем случайную категорию и миссию
        MissionCategory selectedCategory = availableCategories.get(ThreadLocalRandom.current().nextInt(availableCategories.size()));
        List<Mission> availableMissions = new ArrayList<>(selectedCategory.getMissions());
        
        // Фильтруем миссии, которые игрок не может выполнить из-за требований
        availableMissions.removeIf(mission -> !canPlayerAcceptMission(player, mission, reputationMap));
        
        if (availableMissions.isEmpty()) {
            // Если нет подходящих миссий, берем из базовой категории
            availableMissions = new ArrayList<>(missionCategories.get(0).getMissions());
        }
        
        // Выбираем случайную миссию
        Mission mission = availableMissions.get(ThreadLocalRandom.current().nextInt(availableMissions.size()));
        
        // Настраиваем миссию (клиент, район, и т.д.)
        customizeMission(mission);
        
        return mission;
    }
    
    /**
     * Проверяет, может ли игрок принять миссию
     */
    private boolean canPlayerAcceptMission(Player player, Mission mission, Map<String, Integer> reputationMap) {
        // Проверяем предыдущие миссии
        if (mission.getRequiredMissions() != null && !mission.getRequiredMissions().isEmpty()) {
            // Здесь должна быть проверка, выполнял ли игрок эти миссии ранее
            // В данном примере простая реализация, в реальности нужно отслеживать историю миссий
            
            // Просто проверяем базовую репутацию как показатель опыта
            if (getTotalReputation(reputationMap) < 10) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Настраивает детали миссии
     */
    private void customizeMission(Mission mission) {
        // Генерируем случайного клиента
        String clientName = clientNames[ThreadLocalRandom.current().nextInt(clientNames.length)];
        mission.setClientName(clientName);
        
        // Генерируем район
        String district = districtNames[ThreadLocalRandom.current().nextInt(districtNames.length)];
        mission.setDeliveryDistrict(district);
        
        // Небольшая случайность в количестве и награде
        int quantityVariation = ThreadLocalRandom.current().nextInt(-1, 2);
        mission.setQuantity(Math.max(1, mission.getQuantity() + quantityVariation));
        
        double rewardVariation = ThreadLocalRandom.current().nextDouble(0.9, 1.1);
        mission.setCashReward(mission.getCashReward() * rewardVariation);
        mission.setCardReward(mission.getCardReward() * rewardVariation);
    }
    
    /**
     * Получает общую репутацию игрока
     */
    private int getTotalReputation(Map<String, Integer> reputationMap) {
        int total = 0;
        for (int rep : reputationMap.values()) {
            total += Math.max(0, rep); // Учитываем только положительную репутацию
        }
        return total;
    }
    
    /**
     * Игрок принимает миссию
     */
    public boolean acceptMission(Player player, Mission mission) {
        // Проверяем тип миссии
        switch (mission.getType()) {
            case DELIVERY:
                // Проверяем, есть ли у игрока необходимые предметы
                if (!hasRequiredItems(player, mission.getItemType(), mission.getQuantity())) {
                    player.sendMessage(ChatColor.RED + "У вас нет необходимых предметов для этой миссии!");
                    return false;
                }
                
                // Удаляем предметы
                removeItems(player, mission.getItemType(), mission.getQuantity());
                
                // Выдаем награду или сообщаем о провале в зависимости от риска
                return completeDeliveryMission(player, mission);
                
            case TRASH:
                // Даем игроку задание собрать мусор
                player.sendMessage(ChatColor.GREEN + "Вы приняли задание на сбор мусора. Соберите " + 
                        mission.getQuantity() + " единиц мусора и вернитесь к телефонной будке.");
                
                // Создаем специальный предмет, который нужно будет показать для выполнения задания
                ItemStack missionItem = createMissionItem(mission);
                player.getInventory().addItem(missionItem);
                return true;
                
            case GROW:
                // Даем игроку задание вырастить растения
                player.sendMessage(ChatColor.GREEN + "Вы приняли задание на выращивание " + 
                        mission.getQuantity() + " растений " + getItemDisplayName(mission.getItemType()) + ".");
                
                // Создаем специальный предмет, который нужно будет показать для выполнения задания
                ItemStack growMissionItem = createMissionItem(mission);
                player.getInventory().addItem(growMissionItem);
                return true;
                
            default:
                player.sendMessage(ChatColor.RED + "Неизвестный тип миссии!");
                return false;
        }
    }
    
    /**
     * Выполняет миссию по доставке
     */
    private boolean completeDeliveryMission(Player player, Mission mission) {
        UUID playerUuid = player.getUniqueId();
        
        // Увеличиваем базовый риск в зависимости от уровня розыска
        double risk = mission.getRiskFactor();
        
        // Проверяем уровень розыска, если NPCManager доступен
        if (plugin.getNPCManager() != null) {
            int wantedLevel = plugin.getNPCManager().getWantedLevel(playerUuid).getLevel();
            
            // Увеличиваем риск в зависимости от уровня розыска (каждый уровень +10% к риску)
            risk += wantedLevel * 0.1;
            
            // Проверяем контроль района
            String district = mission.getDeliveryDistrict();
            NPCManager.DistrictControl districtControl = plugin.getNPCManager().getDistrictControl(district);
            
            // Увеличиваем риск в зависимости от контроля полиции в районе
            risk += districtControl.getPoliceDetectionRisk() * 0.2;
            
            // Проверяем наличие полиции поблизости
            if (isPoliceNearby(player)) {
                // Существенно увеличиваем риск при наличии полиции
                risk += 0.3;
                
                // Уведомляем игрока
                player.sendMessage(ChatColor.RED + "Вы заметили полицию поблизости! Риск значительно возрастает.");
            }
        }
        
        // Снижаем риск в зависимости от качества телефона (если используется)
        PlayerData playerData = plugin.getPlayerService().getOrCreatePlayerData(player);
        int phoneQuality = playerData.getPhoneQuality();
        risk -= (phoneQuality - 1) * 0.05; // Каждый уровень качества выше базового снижает риск на 5%
        
        // Проверяем на риск
        if (ThreadLocalRandom.current().nextDouble() < risk) {
            // Миссия провалена - полиция поймала
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "=== МИССИЯ ПРОВАЛЕНА ===");
            player.sendMessage(ChatColor.RED + "Полиция перехватила вашу доставку!");
            player.sendMessage(ChatColor.RED + "Вы потеряли товар и не получили оплату.");
            
            // Получаем сообщение о неудаче от клиента
            String failureMessage = getRandomFailureMessage(mission.getClientName());
            player.sendMessage(ChatColor.YELLOW + mission.getClientName() + ": " + ChatColor.WHITE + failureMessage);
            
            // Снижаем репутацию с клиентом
            updateReputation(playerUuid, mission.getClientName(), -3);
            player.sendMessage(ChatColor.GRAY + "Ваша репутация с клиентом " + mission.getClientName() + " значительно снизилась.");
            
            // Увеличиваем уровень розыска, если провалились и NPCManager доступен
            if (plugin.getNPCManager() != null) {
                plugin.getNPCManager().increaseWantedLevel(player, 1, "Попытка продажи запрещенных веществ");
                player.sendMessage(ChatColor.RED + "Полиция внесла вас в список разыскиваемых преступников!");
            }
            
            player.sendMessage(ChatColor.RED + "====================");
            player.sendMessage("");
            
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 1.0f, 0.5f);
            return false;
        } else {
            // Миссия успешна
            double cashReward = mission.getCashReward();
            double cardReward = mission.getCardReward();
            
            // Выплачиваем награду
            plugin.getPlayerService().addCash(playerUuid, cashReward);
            plugin.getPlayerService().addCardBalance(playerUuid, cardReward);
            
            // Повышаем репутацию с клиентом
            updateReputation(playerUuid, mission.getClientName(), 2);
            
            // Обновляем прогресс квестов на продажу наркотиков
            updateSellQuestProgress(player, mission.getItemType(), mission.getQuantity());
            
            // Получаем сообщение об успехе от клиента
            String successMessage = getRandomSuccessMessage(mission.getClientName());
            
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "=== МИССИЯ ВЫПОЛНЕНА ===");
            player.sendMessage(ChatColor.YELLOW + mission.getClientName() + ": " + ChatColor.WHITE + successMessage);
            player.sendMessage(ChatColor.GREEN + "Вы успешно доставили " + mission.getQuantity() + " " + 
                    getDrugDisplayName(mission.getItemType()) + " клиенту " + mission.getClientName() + ".");
            player.sendMessage(ChatColor.GREEN + "Получено: " +
                    ChatColor.GOLD + "$" + String.format("%.2f", cashReward) + ChatColor.GREEN + " наличными и " +
                    ChatColor.GOLD + "$" + String.format("%.2f", cardReward) + ChatColor.GREEN + " на карту.");
            player.sendMessage(ChatColor.GRAY + "Ваша репутация с клиентом " + mission.getClientName() + " повысилась.");
            player.sendMessage(ChatColor.GREEN + "====================");
            player.sendMessage("");
            
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            return true;
        }
    }
    
    /**
     * Проверяет наличие полиции поблизости
     */
    private boolean isPoliceNearby(Player player) {
        if (plugin.getNPCManager() == null) {
            return false;
        }
        
        // Ищем ближайшего полицейского
        return plugin.getNPCManager().getNearestPolice(player, 30.0) != null;
    }
    
    /**
     * Обновляет прогресс квестов на продажу наркотиков
     */
    private void updateSellQuestProgress(Player player, String drugType, int quantity) {
        if (!plugin.isDatabaseAvailable()) {
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        // Сообщаем сервису квестов о продаже наркотиков для обновления прогресса соответствующих квестов
        plugin.getQuestService().notifyDrugSale(playerUuid, drugType, quantity);
        
        // Сервис сам обработает обновление прогресса и выполнение квестов, поэтому здесь не требуется дополнительной логики
    }
    
    /**
     * Обновляет репутацию игрока с клиентом
     */
    public void updateReputation(UUID playerUuid, String clientName, int change) {
        Map<String, Integer> reputationMap = playerReputation.computeIfAbsent(playerUuid, k -> new HashMap<>());
        
        int currentRep = reputationMap.getOrDefault(clientName, 0);
        reputationMap.put(clientName, currentRep + change);
    }
    
    /**
     * Получает репутацию игрока с клиентом
     */
    public int getReputation(UUID playerUuid, String clientName) {
        Map<String, Integer> reputationMap = playerReputation.getOrDefault(playerUuid, new HashMap<>());
        return reputationMap.getOrDefault(clientName, 0);
    }
    
    /**
     * Проверяет, есть ли у игрока необходимые предметы
     */
    private boolean hasRequiredItems(Player player, String itemType, int quantity) {
        int count = 0;
        Material material = getMaterialForItemType(itemType);
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                // Проверяем, что это правильный тип наркотика через метаданные
                if (isDrugItem(item, itemType)) {
                    count += item.getAmount();
                }
            }
        }
        
        return count >= quantity;
    }
    
    /**
     * Удаляет предметы из инвентаря игрока
     */
    private void removeItems(Player player, String itemType, int quantity) {
        int remaining = quantity;
        Material material = getMaterialForItemType(itemType);
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            
            if (item != null && item.getType() == material) {
                // Проверяем, что это правильный тип наркотика через метаданные
                if (isDrugItem(item, itemType)) {
                    if (item.getAmount() <= remaining) {
                        remaining -= item.getAmount();
                        player.getInventory().remove(item);
                    } else {
                        item.setAmount(item.getAmount() - remaining);
                        remaining = 0;
                    }
                }
            }
        }
        
        // Обновляем инвентарь
        player.updateInventory();
    }
    
    /**
     * Проверяет, является ли предмет наркотиком нужного типа
     */
    private boolean isDrugItem(ItemStack item, String drugType) {
        if (!item.hasItemMeta()) return false;
        
        return item.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey(plugin, "drug_type"), 
                PersistentDataType.STRING) && 
               item.getItemMeta().getPersistentDataContainer().get(
                       new NamespacedKey(plugin, "drug_type"), 
                       PersistentDataType.STRING).equalsIgnoreCase(drugType);
    }
    
    /**
     * Создает предмет миссии
     */
    private ItemStack createMissionItem(Mission mission) {
        Material material;
        switch (mission.getType()) {
            case TRASH:
                material = Material.PAPER;
                break;
            case GROW:
                material = Material.MAP;
                break;
            default:
                material = Material.PAPER;
        }
        
        return new ItemBuilder(material)
                .setName(ChatColor.GOLD + "Задание: " + mission.getTitle())
                .setLore(
                        ChatColor.GRAY + "Клиент: " + mission.getClientName(),
                        ChatColor.GRAY + "Описание: " + mission.getDescription(),
                        ChatColor.GRAY + "Требуется: " + mission.getQuantity() + " " + getItemDisplayName(mission.getItemType()),
                        ChatColor.GRAY + "Награда: $" + String.format("%.2f", mission.getCashReward()) + " наличными, $" + 
                                String.format("%.2f", mission.getCardReward()) + " на карту"
                )
                .setPersistentData(missionItemKey, PersistentDataType.STRING, mission.getId())
                .build();
    }
    
    /**
     * Получает материал для типа предмета
     */
    private Material getMaterialForItemType(String itemType) {
        switch (itemType.toLowerCase()) {
            case "marijuana":
                return Material.DRIED_KELP;
            case "cocaine":
                return Material.SUGAR;
            case "meth":
                return Material.GLOWSTONE_DUST;
            case "heroin":
                return Material.GUNPOWDER;
            case "trash":
                return Material.ROTTEN_FLESH; // Временно используем это как "мусор"
            default:
                return Material.DRIED_KELP;
        }
    }
    
    /**
     * Получает отображаемое имя для типа предмета
     */
    private String getItemDisplayName(String itemType) {
        switch (itemType.toLowerCase()) {
            case "marijuana":
                return "марихуаны";
            case "cocaine":
                return "кокаина";
            case "meth":
                return "метамфетамина";
            case "heroin":
                return "героина";
            case "trash":
                return "мусора";
            default:
                return itemType;
        }
    }
    
    /**
     * Инициализирует диалоги клиентов
     */
    private void initializeDialogs() {
        // Александр - серьезный бизнесмен
        List<String> alexGreetings = Arrays.asList(
            "Александр здесь. Говори быстро, у меня мало времени.",
            "Это Александр. Надеюсь, у тебя есть то, что мне нужно.",
            "Александр на проводе. Без лишних слов, перейдем к делу.",
            "Слушаю. Александр. Я ожидаю профессионализма."
        );
        
        List<String> alexRequests = Arrays.asList(
            "Мои клиенты ждут. Нужно %quantity% %drug%. Премиальное предложение.",
            "У меня встреча с важными людьми. Нужно %quantity% %drug%. Оплата достойная.",
            "Деловая встреча сегодня вечером. Требуется %quantity% %drug%. Не подведи меня.",
            "Нужно срочно %quantity% %drug%. Клиенты из высших кругов ждут. Цена не проблема."
        );
        
        List<String> alexSuccess = Arrays.asList(
            "Профессионально. Продолжим сотрудничество.",
            "Отлично. Ценю пунктуальность и качество.",
            "Все в порядке. Будут еще предложения.",
            "Хорошая работа. Средства уже переведены."
        );
        
        List<String> alexFailure = Arrays.asList(
            "Неприемлемо. Я теряю время и деньги.",
            "Это серьезно вредит нашим отношениям. Крайне недоволен.",
            "Я рассчитывал на профессионализм. Разочарован.",
            "Когда у тебя бизнес, нельзя допускать таких ошибок."
        );
        
        clientGreetings.put("Александр", alexGreetings);
        clientRequests.put("Александр", alexRequests);
        clientSuccessMessages.put("Александр", alexSuccess);
        clientFailureMessages.put("Александр", alexFailure);
        
        // Михаил - нервный студент
        List<String> mikhailGreetings = Arrays.asList(
            "Э-это Михаил. Н-надеюсь, ты можешь помочь, чувак.",
            "Михаил на связи! Слушай, очень нужна твоя помощь...",
            "Хэй, это Миха! У меня тут... эм... вечеринка намечается...",
            "Привет, это Михаил. Мне бы немного... ну, ты понимаешь..."
        );
        
        List<String> mikhailRequests = Arrays.asList(
            "Чувак, нужно срочно %quantity% %drug%! У нас туса в общаге сегодня!",
            "Слушай, достань %quantity% %drug%, а? С деньгами проблем нет, предки отправили.",
            "Можешь подогнать %quantity% %drug%? Типа для... учебного проекта, да.",
            "Эй, нужна твоя помощь. %quantity% %drug%, и побыстрее. В долгу не останусь!"
        );
        
        List<String> mikhailSuccess = Arrays.asList(
            "Вау! Спасибо, чувак! Ты реально выручил!",
            "Офигенно! Я твой должник, серьезно!",
            "Да-а-а! Вечеринка будет огонь! Спасибо!",
            "Ты лучший, чел! Сегодня все будет супер!"
        );
        
        List<String> mikhailFailure = Arrays.asList(
            "Блин, ты серьезно? Вся вечеринка на тебе держалась!",
            "Вот черт... Что я теперь скажу ребятам? Они рассчитывали на меня...",
            "Да ладно тебе... Ты же обещал, чувак! Не круто так...",
            "Это реально удар ниже пояса... Я же на тебя надеялся..."
        );
        
        clientGreetings.put("Михаил", mikhailGreetings);
        clientRequests.put("Михаил", mikhailRequests);
        clientSuccessMessages.put("Михаил", mikhailSuccess);
        clientFailureMessages.put("Михаил", mikhailFailure);
        
        // Иван - агрессивный дальнобойщик
        List<String> ivanGreetings = Arrays.asList(
            "Иван на связи. Слушай внимательно.",
            "Здорово. Иван говорит. У меня заказ для тебя.",
            "Иван здесь. Разговор короткий, дело серьезное.",
            "Это Иван. Я по делу, давай без лишних слов."
        );
        
        List<String> ivanRequests = Arrays.asList(
            "Мне нужно %quantity% %drug%. Дорога дальняя, сам понимаешь. Плачу хорошо.",
            "Срочно нужно %quantity% %drug%. Рейс долгий, без этого никак. Цена не вопрос.",
            "Давай без базара – %quantity% %drug%. Плачу наличкой, вопросов не задаю.",
            "Мне в рейс через пару часов. Нужно %quantity% %drug%. Бабки есть, проблем нет."
        );
        
        List<String> ivanSuccess = Arrays.asList(
            "Вот это по-мужски. Уважаю за четкость.",
            "Нормально сработано. Буду обращаться.",
            "Все путем. Вот твои бабки, заслужил.",
            "Четко и без лишних вопросов. Так и надо работать."
        );
        
        List<String> ivanFailure = Arrays.asList(
            "Ты нарываешься, парень. Я не люблю, когда меня подводят.",
            "Да ты охренел? Я на тебя понадеялся, а ты...",
            "Я тебя запомню. Не думай, что это сойдет с рук.",
            "Ты меня крупно подвел. Это так не оставлю."
        );
        
        clientGreetings.put("Иван", ivanGreetings);
        clientRequests.put("Иван", ivanRequests);
        clientSuccessMessages.put("Иван", ivanSuccess);
        clientFailureMessages.put("Иван", ivanFailure);
        
        // Анна - светская дама
        List<String> annaGreetings = Arrays.asList(
            "Анна на связи, дорогой. Надеюсь, ты свободен.",
            "Привет, милый. Это Анна. У меня есть небольшая просьба.",
            "Анечка беспокоит. Мне нужна твоя помощь, сладкий.",
            "О, это ты? Анна звонит. Есть минутка для меня?"
        );
        
        List<String> annaRequests = Arrays.asList(
            "Дорогой, у меня сегодня небольшой приём. Нужно %quantity% %drug%. Самого лучшего качества.",
            "Милый, будь добр, организуй %quantity% %drug%. Вечеринка у подруги, хочу произвести впечатление.",
            "Сладкий, нужно %quantity% %drug% для особого вечера. Ты же не откажешь даме?",
            "У меня сегодня важные гости. Привези %quantity% %drug%, я щедро отблагодарю, обещаю."
        );
        
        List<String> annaSuccess = Arrays.asList(
            "Ты просто прелесть! Всегда знала, что на тебя можно положиться.",
            "О, спасибо, дорогой! Ты настоящий джентльмен.",
            "Мммм, именно то, что нужно. Ты заслуживаешь особой благодарности...",
            "Идеально! Ты знаешь, как сделать женщину счастливой."
        );
        
        List<String> annaFailure = Arrays.asList(
            "Какое разочарование... Я ожидала большего от тебя.",
            "О, дорогой, как неприятно... Теперь весь вечер испорчен.",
            "Не думала, что ты так поступишь со мной. Очень жаль.",
            "Это так неэлегантно – подводить даму. Я расстроена."
        );
        
        clientGreetings.put("Анна", annaGreetings);
        clientRequests.put("Анна", annaRequests);
        clientSuccessMessages.put("Анна", annaSuccess);
        clientFailureMessages.put("Анна", annaFailure);
        
        // Елена - врач
        List<String> elenaGreetings = Arrays.asList(
            "Елена говорит. Это конфиденциальный звонок.",
            "Здравствуйте. Это доктор Елена. Мне нужна консультация.",
            "Елена на связи. Надеюсь, наш разговор останется между нами.",
            "Это Елена из клиники. Нужна срочная помощь."
        );
        
        List<String> elenaRequests = Arrays.asList(
            "Мне нужно %quantity% %drug% для... исследовательских целей. Конфиденциальность гарантирую.",
            "Требуется %quantity% %drug%. Чисто в медицинских целях, разумеется. Оплата наличными.",
            "Срочно нужно %quantity% %drug% для особого пациента. Никаких вопросов, только дискретность.",
            "У меня запрос на %quantity% %drug%. Это для специального медицинского применения. Хорошо заплачу."
        );
        
        List<String> elenaSuccess = Arrays.asList(
            "Превосходно. Все прошло по протоколу.",
            "Именно то, что требовалось. Вы очень помогли нашему... исследованию.",
            "Благодарю за вашу дискретность. Сотрудничество будет продолжено.",
            "Очень профессионально. Наш пациент будет... удовлетворен."
        );
        
        List<String> elenaFailure = Arrays.asList(
            "Это серьезное нарушение наших договоренностей. Крайне непрофессионально.",
            "Мне придется найти более надежного консультанта. Разочарована.",
            "Наш пациент не получит необходимого... лечения. Это недопустимо.",
            "Я рассчитывала на вашу помощь. Это большое разочарование."
        );
        
        clientGreetings.put("Елена", elenaGreetings);
        clientRequests.put("Елена", elenaRequests);
        clientSuccessMessages.put("Елена", elenaSuccess);
        clientFailureMessages.put("Елена", elenaFailure);
    }
    
    /**
     * Получает случайное приветствие клиента
     */
    public String getRandomClientGreeting(String clientName) {
        if (!clientGreetings.containsKey(clientName)) {
            return "Привет, это " + clientName + ". У меня заказ для тебя.";
        }
        
        List<String> greetings = clientGreetings.get(clientName);
        return greetings.get(ThreadLocalRandom.current().nextInt(greetings.size()));
    }
    
    /**
     * Получает случайный запрос от клиента
     */
    public String getRandomClientRequest(String clientName, String drugType, int quantity) {
        if (!clientRequests.containsKey(clientName)) {
            return "Мне нужно " + quantity + " " + getDrugDisplayName(drugType) + ". Обычная цена.";
        }
        
        List<String> requests = clientRequests.get(clientName);
        String request = requests.get(ThreadLocalRandom.current().nextInt(requests.size()));
        
        // Заменяем плейсхолдеры
        request = request.replace("%quantity%", String.valueOf(quantity))
                         .replace("%drug%", getDrugDisplayName(drugType));
        
        return request;
    }
    
    /**
     * Получает случайное сообщение успеха от клиента
     */
    public String getRandomSuccessMessage(String clientName) {
        if (!clientSuccessMessages.containsKey(clientName)) {
            return "Отлично. Вот твои деньги.";
        }
        
        List<String> messages = clientSuccessMessages.get(clientName);
        return messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
    }
    
    /**
     * Получает случайное сообщение неудачи от клиента
     */
    public String getRandomFailureMessage(String clientName) {
        if (!clientFailureMessages.containsKey(clientName)) {
            return "Ты подвел меня. Больше не звони.";
        }
        
        List<String> messages = clientFailureMessages.get(clientName);
        return messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
    }
    
    /**
     * Получает отображаемое имя наркотика
     */
    private String getDrugDisplayName(String drugType) {
        switch (drugType.toLowerCase()) {
            case "marijuana":
                return "травы";
            case "cocaine":
                return "кокса";
            case "meth":
                return "мета";
            case "heroin":
                return "герыча";
            default:
                return drugType;
        }
    }
    
    /**
     * Класс категории миссий
     */
    public static class MissionCategory {
        private final String id;
        private final int requiredReputation;
        private final List<Mission> missions = new ArrayList<>();
        
        public MissionCategory(String id, int requiredReputation) {
            this.id = id;
            this.requiredReputation = requiredReputation;
        }
        
        public void addMission(Mission mission) {
            missions.add(mission);
        }
        
        public String getId() {
            return id;
        }
        
        public int getRequiredReputation() {
            return requiredReputation;
        }
        
        public List<Mission> getMissions() {
            return missions;
        }
    }
    
    /**
     * Класс миссии
     */
    public static class Mission {
        private final String id;
        private final String title;
        private final String description;
        private final MissionType type;
        private final String itemType;
        private int quantity;
        private double cashReward;
        private double cardReward;
        private final double riskFactor;
        private final List<String> requiredMissions;
        
        private String clientName;
        private String deliveryDistrict;
        
        public Mission(String id, String title, String description, MissionType type, 
                String itemType, int quantity, double cashReward, double cardReward, 
                double riskFactor, List<String> requiredMissions) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.type = type;
            this.itemType = itemType;
            this.quantity = quantity;
            this.cashReward = cashReward;
            this.cardReward = cardReward;
            this.riskFactor = riskFactor;
            this.requiredMissions = requiredMissions;
        }
        
        // Геттеры и сеттеры
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public MissionType getType() { return type; }
        public String getItemType() { return itemType; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        
        public double getCashReward() { return cashReward; }
        public void setCashReward(double cashReward) { this.cashReward = cashReward; }
        
        public double getCardReward() { return cardReward; }
        public void setCardReward(double cardReward) { this.cardReward = cardReward; }
        
        public double getRiskFactor() { return riskFactor; }
        public List<String> getRequiredMissions() { return requiredMissions; }
        
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        
        public String getDeliveryDistrict() { return deliveryDistrict; }
        public void setDeliveryDistrict(String deliveryDistrict) { this.deliveryDistrict = deliveryDistrict; }
    }
    
    /**
     * Типы миссий
     */
    public enum MissionType {
        DELIVERY, // Доставка наркотиков
        TRASH,    // Сбор мусора
        GROW      // Выращивание растений
    }
} 