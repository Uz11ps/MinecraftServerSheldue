package com.Minecraft_server.Narkomanka.ui;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.npc.PhoneBoothNPC;
import com.Minecraft_server.Narkomanka.npc.PhoneMissionManager;
import com.Minecraft_server.Narkomanka.npc.PhoneMissionManager.Mission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Класс для создания и управления интерфейсом мобильного телефона
 */
public class PhoneUI implements Listener {
    
    private final Narkomanka plugin;
    private final PhoneBoothNPC phoneBoothNPC;
    private final Map<UUID, PhoneMenu> playerMenus = new HashMap<>();
    
    // Ключи для метаданных
    private final NamespacedKey functionKey;
    private final NamespacedKey menuKey;
    private final NamespacedKey indexKey;
    
    // Звуки интерфейса
    private static final Sound BUTTON_SOUND = Sound.UI_BUTTON_CLICK;
    private static final Sound ERROR_SOUND = Sound.ENTITY_ENDERMAN_TELEPORT;
    private static final Sound SUCCESS_SOUND = Sound.ENTITY_PLAYER_LEVELUP;
    
    /**
     * Создает менеджер интерфейса телефона
     */
    public PhoneUI(Narkomanka plugin, PhoneBoothNPC phoneBoothNPC) {
        this.plugin = plugin;
        this.phoneBoothNPC = phoneBoothNPC;
        
        // Создаем ключи для метаданных
        this.functionKey = new NamespacedKey(plugin, "phone_function");
        this.menuKey = new NamespacedKey(plugin, "phone_menu");
        this.indexKey = new NamespacedKey(plugin, "phone_index");
        
        // Регистрируем слушатель событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Открывает телефонный интерфейс для игрока
     */
    public void openPhone(Player player, int phoneQuality) {
        // Закрываем предыдущее меню, если оно было открыто
        if (playerMenus.containsKey(player.getUniqueId())) {
            playerMenus.get(player.getUniqueId()).close();
        }
        
        // Создаем новое главное меню
        PhoneMenu mainMenu = new PhoneMenu(player, "Мобильный телефон", 54, phoneQuality);
        playerMenus.put(player.getUniqueId(), mainMenu);
        
        // Создаем элементы главного меню
        createMainMenuItems(mainMenu);
        
        // Открываем меню
        mainMenu.open();
    }
    
    /**
     * Создает элементы главного меню телефона
     */
    private void createMainMenuItems(PhoneMenu menu) {
        Player player = menu.getPlayer();
        
        // Фон телефона - черные панели
        for (int i = 0; i < 54; i++) {
            menu.setItem(i, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        
        // Верхняя панель (статус-бар)
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        
        // Иконка сигнала сети
        int signalStrength = Math.max(1, Math.min(4, menu.getPhoneQuality()));
        String signalDisplay = "";
        for (int i = 0; i < signalStrength; i++) {
            signalDisplay += "▮";
        }
        for (int i = signalStrength; i < 4; i++) {
            signalDisplay += "▯";
        }
        menu.setItem(0, createGuiItem(Material.REDSTONE_TORCH, 
                ChatColor.GREEN + "Сигнал: " + signalDisplay,
                ChatColor.GRAY + "Качество сигнала: " + getSignalQualityName(signalStrength)));
        
        // Текущее время
        menu.setItem(4, createFunctionItem(
                Material.CLOCK, 
                ChatColor.YELLOW + "Время: " + getCurrentTime(),
                "time_display",
                Arrays.asList(ChatColor.GRAY + "Текущее время на сервере")
        ));
        
        // Индикатор батареи
        menu.setItem(8, createGuiItem(Material.GLOWSTONE, 
                ChatColor.GREEN + "Батарея: " + ChatColor.YELLOW + "100%",
                ChatColor.GRAY + "Заряд батареи"));
        
        // Иконка "Контакты"
        menu.setItem(19, createFunctionItem(
                Material.WRITABLE_BOOK, 
                ChatColor.GREEN + "Контакты",
                "contacts",
                Arrays.asList(
                        ChatColor.GRAY + "Список ваших контактов",
                        ChatColor.GRAY + "и телефонных номеров",
                        "",
                        ChatColor.YELLOW + "Нажмите, чтобы открыть"
                )
        ));
        
        // Иконка "Задания"
        menu.setItem(22, createFunctionItem(
                Material.PAPER, 
                ChatColor.AQUA + "Миссии",
                "missions",
                Arrays.asList(
                        ChatColor.GRAY + "Посмотрите доступные миссии",
                        ChatColor.GRAY + "и активные задания",
                        "",
                        ChatColor.YELLOW + "Нажмите, чтобы открыть"
                )
        ));
        
        // Иконка "Карта"
        menu.setItem(25, createFunctionItem(
                Material.MAP, 
                ChatColor.GOLD + "Карта города",
                "map",
                Arrays.asList(
                        ChatColor.GRAY + "Навигация по районам города",
                        ChatColor.GRAY + "и отслеживание заданий",
                        "",
                        ChatColor.YELLOW + "Нажмите, чтобы открыть"
                )
        ));
        
        // Иконка "Репутация"
        menu.setItem(31, createFunctionItem(
                Material.PLAYER_HEAD, 
                ChatColor.LIGHT_PURPLE + "Репутация",
                "reputation",
                Arrays.asList(
                        ChatColor.GRAY + "Ваша репутация с клиентами",
                        ChatColor.GRAY + "и преступными группировками",
                        "",
                        ChatColor.YELLOW + "Нажмите, чтобы открыть"
                )
        ));
        
        // Иконка "Банк"
        menu.setItem(30, createFunctionItem(
                Material.GOLD_INGOT, 
                ChatColor.GOLD + "Банк",
                "bank",
                Arrays.asList(
                        ChatColor.GRAY + "Управление наличными",
                        ChatColor.GRAY + "и банковским счетом",
                        "",
                        ChatColor.YELLOW + "Нажмите, чтобы открыть"
                )
        ));
        
        // Иконка "Настройки"
        menu.setItem(32, createFunctionItem(
                Material.COMPARATOR, 
                ChatColor.WHITE + "Настройки",
                "settings",
                Arrays.asList(
                        ChatColor.GRAY + "Настройки телефона",
                        ChatColor.GRAY + "и оформление интерфейса",
                        "",
                        ChatColor.YELLOW + "Нажмите, чтобы открыть"
                )
        ));
        
        // Индикатор качества телефона
        menu.setItem(40, createQualityIndicator(menu.getPhoneQuality()));
        
        // Иконка "Выход" (виртуальная кнопка "Домой")
        menu.setItem(49, createFunctionItem(
                Material.BARRIER, 
                ChatColor.RED + "Закрыть телефон",
                "close",
                Arrays.asList(
                        ChatColor.GRAY + "Выход из интерфейса телефона",
                        "",
                        ChatColor.YELLOW + "Нажмите, чтобы закрыть"
                )
        ));
    }
    
    /**
     * Открывает меню заданий
     */
    private void openMissionsMenu(PhoneMenu parentMenu) {
        Player player = parentMenu.getPlayer();
        
        // Создаем новое меню
        PhoneMenu missionMenu = new PhoneMenu(player, "Телефон - Миссии", 54, parentMenu.getPhoneQuality());
        missionMenu.setParent(parentMenu);
        playerMenus.put(player.getUniqueId(), missionMenu);
        
        // Верхняя панель
        for (int i = 0; i < 9; i++) {
            missionMenu.setItem(i, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        
        // Заголовок меню
        missionMenu.setItem(4, createGuiItem(
                Material.PAPER,
                ChatColor.AQUA + "Активные миссии",
                ChatColor.GRAY + "Список текущих заданий"
        ));
        
        // Получаем активные миссии
        List<Mission> missions = phoneBoothNPC.getPlayerMissions(player.getUniqueId());
        
        if (missions.isEmpty()) {
            // Если нет активных миссий
            missionMenu.setItem(22, createGuiItem(
                    Material.BARRIER,
                    ChatColor.RED + "Нет активных миссий",
                    ChatColor.GRAY + "У вас нет активных заданий.",
                    ChatColor.GRAY + "Подождите новых звонков или",
                    ChatColor.GRAY + "посетите телефонную будку."
            ));
        } else {
            // Отображаем активные миссии
            int slot = 10;
            for (int i = 0; i < missions.size(); i++) {
                Mission mission = missions.get(i);
                missionMenu.setItem(slot++, createMissionItem(mission, i));
                
                // Переходим на следующую строку каждые 7 предметов
                if ((slot - 10) % 7 == 0) {
                    slot += 2;
                }
                
                // Максимум отображаем 21 миссию (3 строки)
                if (i >= 20) {
                    break;
                }
            }
        }
        
        // Кнопка "Симулировать звонок" (если у игрока есть права)
        if (player.hasPermission("narkomanka.admin") || player.hasPermission("narkomanka.phone.simulate")) {
            missionMenu.setItem(48, createFunctionItem(
                    Material.ENDER_EYE,
                    ChatColor.LIGHT_PURPLE + "Симулировать звонок",
                    "simulate_call",
                    Arrays.asList(
                            ChatColor.GRAY + "Вызывает звонок с новым заданием",
                            ChatColor.GRAY + "(Только для админов)",
                            "",
                            ChatColor.YELLOW + "Нажмите, чтобы симулировать"
                    )
            ));
        }
        
        // Кнопка "Назад"
        missionMenu.setItem(49, createFunctionItem(
                Material.ARROW,
                ChatColor.YELLOW + "Назад",
                "back",
                Arrays.asList(
                        ChatColor.GRAY + "Вернуться в главное меню",
                        "",
                        ChatColor.YELLOW + "Нажмите, чтобы вернуться"
                )
        ));
        
        // Нижняя панель
        for (int i = 45; i < 54; i++) {
            if (i != 48 && i != 49) {
                missionMenu.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }
        
        // Открываем меню
        missionMenu.open();
    }
    
    /**
     * Создает предмет миссии для меню
     */
    private ItemStack createMissionItem(Mission mission, int index) {
        Material material;
        
        // Выбираем иконку в зависимости от типа
        switch (mission.getType()) {
            case DELIVERY:
                switch (mission.getItemType().toLowerCase()) {
                    case "marijuana":
                        material = Material.DRIED_KELP;
                        break;
                    case "cocaine":
                        material = Material.SUGAR;
                        break;
                    case "meth":
                        material = Material.GLOWSTONE_DUST;
                        break;
                    case "heroin":
                        material = Material.GUNPOWDER;
                        break;
                    default:
                        material = Material.PAPER;
                }
                break;
            case TRASH:
                material = Material.ROTTEN_FLESH;
                break;
            case GROW:
                material = Material.WHEAT_SEEDS;
                break;
            default:
                material = Material.PAPER;
        }
        
        // Создаем базовый предмет
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + mission.getTitle());
        
        // Форматируем описание
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "От: " + ChatColor.WHITE + mission.getClientName());
        lore.add(ChatColor.GRAY + "Район: " + ChatColor.WHITE + mission.getDeliveryDistrict());
        lore.add("");
        lore.add(ChatColor.GRAY + mission.getDescription());
        lore.add("");
        
        // Требования и награда
        String itemName = getItemDisplayName(mission.getItemType());
        lore.add(ChatColor.GRAY + "Требуется: " + ChatColor.WHITE + mission.getQuantity() + " " + itemName);
        lore.add("");
        lore.add(ChatColor.GRAY + "Награда:");
        lore.add(ChatColor.GOLD + "  $" + String.format("%.2f", mission.getCashReward()) + ChatColor.GRAY + " наличными");
        lore.add(ChatColor.GOLD + "  $" + String.format("%.2f", mission.getCardReward()) + ChatColor.GRAY + " на карту");
        
        // Риск
        String riskText;
        ChatColor riskColor;
        double risk = mission.getRiskFactor();
        
        if (risk < 0.2) {
            riskText = "Низкий";
            riskColor = ChatColor.GREEN;
        } else if (risk < 0.35) {
            riskText = "Средний";
            riskColor = ChatColor.YELLOW;
        } else if (risk < 0.5) {
            riskText = "Высокий";
            riskColor = ChatColor.RED;
        } else {
            riskText = "Очень высокий";
            riskColor = ChatColor.DARK_RED;
        }
        
        lore.add(ChatColor.GRAY + "Риск: " + riskColor + riskText);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Нажмите, чтобы принять миссию");
        
        meta.setLore(lore);
        
        // Добавляем метаданные для идентификации
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(functionKey, PersistentDataType.STRING, "accept_mission");
        container.set(indexKey, PersistentDataType.INTEGER, index);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Обрабатывает клик в инвентаре
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUuid = player.getUniqueId();
        
        // Проверяем, что это наше меню
        if (!playerMenus.containsKey(playerUuid)) {
            return;
        }
        
        PhoneMenu openMenu = playerMenus.get(playerUuid);
        if (event.getInventory() != openMenu.getInventory()) {
            return;
        }
        
        // Отменяем событие - нельзя брать предметы из меню
        event.setCancelled(true);
        
        // Проверяем клик на элемент с функцией
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        
        PersistentDataContainer container = clickedItem.getItemMeta().getPersistentDataContainer();
        
        // Проверяем, есть ли функция у этого предмета
        if (container.has(functionKey, PersistentDataType.STRING)) {
            String function = container.get(functionKey, PersistentDataType.STRING);
            
            // Воспроизводим звук нажатия кнопки
            player.playSound(player.getLocation(), BUTTON_SOUND, 0.5f, 1.0f);
            
            // Обрабатываем функцию
            switch (function) {
                case "missions":
                    openMissionsMenu(openMenu);
                    break;
                    
                case "contacts":
                    // Временно не реализовано
                    player.sendMessage(ChatColor.YELLOW + "Функция 'Контакты' временно недоступна.");
                    break;
                    
                case "map":
                    // Временно не реализовано
                    player.sendMessage(ChatColor.YELLOW + "Функция 'Карта' временно недоступна.");
                    break;
                    
                case "info":
                    // Временно не реализовано
                    player.sendMessage(ChatColor.YELLOW + "Функция 'Информация' временно недоступна.");
                    break;
                    
                case "settings":
                    // Временно не реализовано
                    player.sendMessage(ChatColor.YELLOW + "Функция 'Настройки' временно недоступна.");
                    break;
                    
                case "close":
                    player.closeInventory();
                    break;
                    
                case "back":
                    if (openMenu.getParent() != null) {
                        playerMenus.put(playerUuid, openMenu.getParent());
                        openMenu.getParent().open();
                    } else {
                        player.closeInventory();
                    }
                    break;
                    
                case "accept_mission":
                    if (container.has(indexKey, PersistentDataType.INTEGER)) {
                        int missionIndex = container.get(indexKey, PersistentDataType.INTEGER);
                        handleAcceptMission(player, missionIndex);
                    }
                    break;
                    
                case "simulate_call":
                    if (player.hasPermission("narkomanka.admin") || player.hasPermission("narkomanka.phone.simulate")) {
                        phoneBoothNPC.simulatePhoneCall(player);
                        player.closeInventory();
                    } else {
                        player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой функции.");
                        player.playSound(player.getLocation(), ERROR_SOUND, 0.5f, 1.0f);
                    }
                    break;
            }
        }
    }
    
    /**
     * Обрабатывает принятие миссии
     */
    private void handleAcceptMission(Player player, int missionIndex) {
        boolean success = phoneBoothNPC.acceptMission(player, missionIndex);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Вы приняли задание!");
            player.playSound(player.getLocation(), SUCCESS_SOUND, 1.0f, 1.0f);
            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось принять задание. Проверьте, есть ли у вас необходимые предметы.");
            player.playSound(player.getLocation(), ERROR_SOUND, 1.0f, 1.0f);
        }
    }
    
    /**
     * Обработчик закрытия инвентаря
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        // Удаляем запись о меню, когда игрок закрывает его
        if (playerMenus.containsKey(playerUuid) && playerMenus.get(playerUuid).getInventory() == event.getInventory()) {
            playerMenus.remove(playerUuid);
        }
    }
    
    /**
     * Создает стандартный элемент интерфейса
     */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Создает элемент с функцией
     */
    private ItemStack createFunctionItem(Material material, String name, String function, List<String> lore) {
        ItemStack item = createGuiItem(material, name);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(lore);
        
        // Добавляем метаданные для функции
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(functionKey, PersistentDataType.STRING, function);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Создает индикатор качества телефона
     */
    private ItemStack createQualityIndicator(int quality) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();
        
        switch (quality) {
            case 1:
                material = Material.IRON_INGOT;
                name = ChatColor.WHITE + "Качество телефона: Низкое";
                lore.add(ChatColor.GRAY + "Базовая модель телефона");
                break;
            case 2:
                material = Material.GOLD_INGOT;
                name = ChatColor.GREEN + "Качество телефона: Среднее";
                lore.add(ChatColor.GRAY + "Улучшенная модель телефона");
                lore.add(ChatColor.GRAY + "Снижение риска: 5%");
                break;
            case 3:
                material = Material.DIAMOND;
                name = ChatColor.BLUE + "Качество телефона: Высокое";
                lore.add(ChatColor.GRAY + "Продвинутая модель телефона");
                lore.add(ChatColor.GRAY + "Снижение риска: 10%");
                lore.add(ChatColor.GRAY + "Доступно больше миссий");
                break;
            case 4:
                material = Material.NETHERITE_INGOT;
                name = ChatColor.GOLD + "Качество телефона: Премиум";
                lore.add(ChatColor.GRAY + "Премиальная модель телефона");
                lore.add(ChatColor.GRAY + "Снижение риска: 15%");
                lore.add(ChatColor.GRAY + "Доступ к эксклюзивным миссиям");
                lore.add(ChatColor.GRAY + "Повышенные награды");
                break;
            default:
                material = Material.IRON_NUGGET;
                name = ChatColor.WHITE + "Качество телефона: Базовое";
                lore.add(ChatColor.GRAY + "Простая модель телефона");
        }
        
        return createGuiItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * Возвращает отображаемое имя для типа предмета
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
     * Возвращает текущее время на сервере
     */
    private String getCurrentTime() {
        // Получаем время мира
        long time = plugin.getServer().getWorlds().get(0).getTime();
        
        // Преобразуем к часам и минутам
        int hours = (int) ((time / 1000 + 6) % 24); // Minecraft день начинается с 6:00
        int minutes = (int) ((time % 1000) * 60 / 1000);
        
        return String.format("%02d:%02d", hours, minutes);
    }
    
    /**
     * Возвращает название качества сигнала
     */
    private String getSignalQualityName(int quality) {
        switch (quality) {
            case 1:
                return "Слабый";
            case 2: 
                return "Нормальный";
            case 3:
                return "Хороший";
            case 4:
                return "Отличный";
            default:
                return "Неизвестно";
        }
    }
    
    /**
     * Внутренний класс для представления меню телефона
     */
    private class PhoneMenu {
        private final Player player;
        private final Inventory inventory;
        private final int phoneQuality;
        private PhoneMenu parent;
        
        public PhoneMenu(Player player, String title, int size, int phoneQuality) {
            this.player = player;
            this.inventory = Bukkit.createInventory(null, size, title);
            this.phoneQuality = phoneQuality;
        }
        
        public Player getPlayer() {
            return player;
        }
        
        public Inventory getInventory() {
            return inventory;
        }
        
        public int getPhoneQuality() {
            return phoneQuality;
        }
        
        public PhoneMenu getParent() {
            return parent;
        }
        
        public void setParent(PhoneMenu parent) {
            this.parent = parent;
        }
        
        public void setItem(int slot, ItemStack item) {
            inventory.setItem(slot, item);
        }
        
        public void open() {
            player.openInventory(inventory);
        }
        
        public void close() {
            if (player.getOpenInventory().getTopInventory() == inventory) {
                player.closeInventory();
            }
        }
    }
} 