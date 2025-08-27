package com.Minecraft_server.Narkomanka.ui;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.npc.PhoneBoothNPC;
import com.Minecraft_server.Narkomanka.npc.PhoneMissionManager;
import com.Minecraft_server.Narkomanka.npc.PhoneMissionManager.Mission;
import com.Minecraft_server.Narkomanka.npc.PhoneMissionManager.MissionType;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Меню для взаимодействия с телефонной будкой
 */
public class PhoneBoothMenu implements Listener {

    private final Narkomanka plugin;
    private final PhoneBoothNPC phoneBoothNPC;
    private final Map<UUID, Inventory> openMenus = new HashMap<>();

    // Ключи для метаданных
    private final NamespacedKey acceptKey;
    private final NamespacedKey declineKey;
    
    /**
     * Конструктор меню телефонной будки
     */
    public PhoneBoothMenu(Narkomanka plugin, PhoneBoothNPC phoneBoothNPC) {
        this.plugin = plugin;
        this.phoneBoothNPC = phoneBoothNPC;

        // Создаем ключи для метаданных
        this.acceptKey = new NamespacedKey(plugin, "phone_accept_mission");
        this.declineKey = new NamespacedKey(plugin, "phone_decline_mission");
        
        // Регистрируем слушатель событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Открывает меню для игрока
     */
    public void openMenu(Player player) {
        UUID playerUuid = player.getUniqueId();

        // Создаем инвентарь
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + "Телефонная Будка");

        // Верхняя панель декорации
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, createDecorationItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        }

        // Получаем активные задания игрока
        List<Mission> missions = phoneBoothNPC.getPlayerMissions(playerUuid);

        if (missions.isEmpty()) {
            // Если нет активных заданий
            ItemStack noMissionsItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = noMissionsItem.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Нет активных заданий");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "У вас нет активных телефонных заданий.",
                    ChatColor.GRAY + "Подождите, пока вам позвонят, или",
                    ChatColor.GRAY + "используйте команду /phone simulate_call для тестирования."
            ));
            noMissionsItem.setItemMeta(meta);
            menu.setItem(22, noMissionsItem);
        } else {
            // Отображаем активные задания
            int slot = 19;
            for (int i = 0; i < missions.size(); i++) {
                Mission mission = missions.get(i);
                
                // Предмет задания
                ItemStack missionItem = createMissionItem(mission, i);
                menu.setItem(slot++, missionItem);
                
                // Кнопка принять
                ItemStack acceptItem = new ItemStack(Material.LIME_WOOL);
                ItemMeta acceptMeta = acceptItem.getItemMeta();
                acceptMeta.setDisplayName(ChatColor.GREEN + "Принять задание");
                acceptMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Нажмите, чтобы принять задание",
                        ChatColor.GRAY + "от " + mission.getClientName()
                ));
                // Добавляем метаданные для идентификации кнопки
                PersistentDataContainer acceptContainer = acceptMeta.getPersistentDataContainer();
                acceptContainer.set(acceptKey, PersistentDataType.INTEGER, i);
                acceptItem.setItemMeta(acceptMeta);
                menu.setItem(slot++, acceptItem);
                
                // Кнопка отклонить
                ItemStack declineItem = new ItemStack(Material.RED_WOOL);
                ItemMeta declineMeta = declineItem.getItemMeta();
                declineMeta.setDisplayName(ChatColor.RED + "Отклонить задание");
                declineMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Нажмите, чтобы отклонить задание",
                        ChatColor.GRAY + "от " + mission.getClientName(),
                        ChatColor.RED + "Внимание: " + ChatColor.GRAY + "это снизит вашу репутацию"
                ));
                // Добавляем метаданные для идентификации кнопки
                PersistentDataContainer declineContainer = declineMeta.getPersistentDataContainer();
                declineContainer.set(declineKey, PersistentDataType.INTEGER, i);
                declineItem.setItemMeta(declineMeta);
                menu.setItem(slot++, declineItem);
                
                // Переходим на следующую строку после каждого задания
                if (i < missions.size() - 1) {
                    slot = (slot / 9 + 1) * 9 + 1;
                }
            }
        }
        
        // Добавляем информацию о репутации
        ItemStack reputationItem = createReputationItem(player);
        menu.setItem(49, reputationItem);

        // Нижняя панель декорации
        for (int i = 45; i < 54; i++) {
            if (i != 49) { // Пропускаем слот с информацией о репутации
                menu.setItem(i, createDecorationItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
            }
        }

        // Открываем меню
        player.openInventory(menu);

        // Сохраняем открытое меню
        openMenus.put(playerUuid, menu);
    }
    
    /**
     * Создает предмет с информацией о репутации игрока
     */
    private ItemStack createReputationItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Ваша репутация");
        
        // Получаем общую репутацию
        Map<String, Integer> clientReputations = new HashMap<>();
        PhoneMissionManager missionManager = phoneBoothNPC.getMissionManager();
        
        // Для каждого клиента получаем репутацию
        for (String clientName : Arrays.asList("Александр", "Михаил", "Иван", "Анна", "Елена")) {
            int reputation = missionManager.getReputation(player.getUniqueId(), clientName);
            clientReputations.put(clientName, reputation);
        }
        
        // Создаем описание
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Ваша репутация с разными клиентами:");
        lore.add("");
        
        // Сортируем клиентов по репутации (от высшей к низшей)
        clientReputations.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    String clientName = entry.getKey();
                    int reputation = entry.getValue();
                    ChatColor color;
                    String reputationText;
                    
                    if (reputation <= -10) {
                        color = ChatColor.DARK_RED;
                        reputationText = "Враждебно";
                    } else if (reputation < 0) {
                        color = ChatColor.RED;
                        reputationText = "Негативно";
                    } else if (reputation == 0) {
                        color = ChatColor.YELLOW;
                        reputationText = "Нейтрально";
                    } else if (reputation < 10) {
                        color = ChatColor.GREEN;
                        reputationText = "Дружелюбно";
                    } else if (reputation < 25) {
                        color = ChatColor.DARK_GREEN;
                        reputationText = "Уважительно";
                    } else {
                        color = ChatColor.GOLD;
                        reputationText = "Почетно";
                    }
                    
                    lore.add(ChatColor.GRAY + clientName + ": " + color + reputationText + 
                            ChatColor.GRAY + " (" + (reputation >= 0 ? "+" : "") + reputation + ")");
                });
        
        lore.add("");
        lore.add(ChatColor.GRAY + "Высокая репутация открывает доступ");
        lore.add(ChatColor.GRAY + "к более выгодным заданиям.");
        
        meta.setLore(lore);
        meta.setOwningPlayer(player);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Создает предмет с информацией о задании
     */
    private ItemStack createMissionItem(Mission mission, int index) {
        Material material;
        if (mission.getType() == MissionType.DELIVERY) {
            // Выбираем материал в зависимости от типа наркотика
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
        } else if (mission.getType() == MissionType.TRASH) {
            material = Material.ROTTEN_FLESH;
        } else if (mission.getType() == MissionType.GROW) {
            material = Material.WHEAT_SEEDS;
        } else {
            material = Material.PAPER;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + mission.getTitle());
        
        // Создаем описание
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Клиент: " + ChatColor.WHITE + mission.getClientName());
        lore.add(ChatColor.GRAY + "Район: " + ChatColor.WHITE + mission.getDeliveryDistrict());
        lore.add("");
        lore.add(ChatColor.GRAY + mission.getDescription());
        lore.add("");
        
        // Информация о требованиях
        String itemDisplayName = getItemDisplayName(mission.getItemType());
        lore.add(ChatColor.GRAY + "Требуется: " + ChatColor.WHITE + 
                mission.getQuantity() + " " + itemDisplayName);
        
        // Информация о награде
        lore.add(ChatColor.GRAY + "Награда:");
        lore.add(ChatColor.GOLD + "  $" + String.format("%.2f", mission.getCashReward()) + ChatColor.GRAY + " наличными");
        lore.add(ChatColor.GOLD + "  $" + String.format("%.2f", mission.getCardReward()) + ChatColor.GRAY + " на карту");
        
        // Информация о риске
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
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
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
     * Создает декоративный предмет для меню
     */
    private ItemStack createDecorationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Обработчик клика в инвентаре
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUuid = player.getUniqueId();
        Inventory inventory = event.getInventory();

        // Проверяем, что это наше меню
        if (!openMenus.containsKey(playerUuid) || inventory != openMenus.get(playerUuid)) {
            return;
        }
        
        // Отменяем событие - нельзя брать предметы из меню
        event.setCancelled(true);

        // Проверяем клик на кнопку "Принять задание"
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.hasItemMeta()) {
            PersistentDataContainer container = clickedItem.getItemMeta().getPersistentDataContainer();
            
            // Кнопка "Принять"
            if (container.has(acceptKey, PersistentDataType.INTEGER)) {
                int missionIndex = container.get(acceptKey, PersistentDataType.INTEGER);
                handleAcceptMission(player, missionIndex);
            }
            // Кнопка "Отклонить"
            else if (container.has(declineKey, PersistentDataType.INTEGER)) {
                int missionIndex = container.get(declineKey, PersistentDataType.INTEGER);
                handleDeclineMission(player, missionIndex);
            }
        }
    }

    /**
     * Обрабатывает принятие задания
     */
    private void handleAcceptMission(Player player, int missionIndex) {
        boolean success = phoneBoothNPC.acceptMission(player, missionIndex);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Вы приняли задание!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось принять задание. Проверьте, есть ли у вас необходимые предметы.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * Обрабатывает отклонение задания
     */
    private void handleDeclineMission(Player player, int missionIndex) {
        boolean success = phoneBoothNPC.declineMission(player, missionIndex);
        
        if (success) {
            player.sendMessage(ChatColor.YELLOW + "Вы отклонили задание.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.closeInventory();
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

        // Удаляем запись о меню, когда игрок закрывает его
        openMenus.remove(player.getUniqueId());
    }
}