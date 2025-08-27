package com.Minecraft_server.Narkomanka.ui;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.QuestData;
import com.Minecraft_server.Narkomanka.database.QuestProgress;
import com.Minecraft_server.Narkomanka.npc.QuestGiverNPC.QuestGiverType;
import com.Minecraft_server.Narkomanka.util.ItemBuilder;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Меню для отображения и выбора квестов
 */
public class QuestMenu implements Listener {

    private final Narkomanka plugin;
    private final Map<UUID, Inventory> openMenus = new HashMap<>();
    private final NamespacedKey questMenuKey;
    private final NamespacedKey questIdKey;
    
    public QuestMenu(Narkomanka plugin) {
        this.plugin = plugin;
        this.questMenuKey = new NamespacedKey(plugin, "quest_menu");
        this.questIdKey = new NamespacedKey(plugin, "quest_id");
        
        // Регистрируем обработчик событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Открывает меню квестов для игрока
     */
    public void openMenu(Player player, QuestGiverType npcType) {
        // Создаем инвентарь для меню
        Inventory inventory = Bukkit.createInventory(
                player, 
                27, 
                ChatColor.GOLD + "Квесты - " + getNpcTypeName(npcType));
        
        // Добавляем метаданные в первый слот для идентификации меню
        ItemStack menuIdentifier = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .setPersistentData(questMenuKey, PersistentDataType.STRING, npcType.toString())
                .build();
        
        // Заполняем фон
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, menuIdentifier);
        }
        for (int i = 18; i < 27; i++) {
            inventory.setItem(i, menuIdentifier);
        }
        
        // Получаем доступные квесты
        List<QuestData> availableQuests = getAvailableQuests(player, npcType);
        
        // Добавляем квесты в меню
        int slot = 9;
        for (QuestData quest : availableQuests) {
            if (slot >= 18) break; // Ограничиваем количество отображаемых квестов
            
            // Создаем иконку квеста
            ItemStack questIcon = createQuestIcon(quest, player);
            inventory.setItem(slot++, questIcon);
        }
        
        // Запоминаем открытое меню
        openMenus.put(player.getUniqueId(), inventory);
        
        // Открываем меню
        player.openInventory(inventory);
    }
    
    /**
     * Создает иконку квеста
     */
    private ItemStack createQuestIcon(QuestData quest, Player player) {
        Material material;
        
        // Выбираем материал в зависимости от типа квеста
        switch (quest.getQuestType().toUpperCase()) {
            case "BREAK_BLOCK":
                material = Material.IRON_PICKAXE;
                break;
            case "KILL_ENTITY":
                material = Material.IRON_SWORD;
                break;
            case "BUY_DRUG":
                material = Material.DRIED_KELP;
                break;
            case "SELL_DRUG":
                material = Material.EMERALD;
                break;
            case "GROW_PLANT":
                material = Material.WHEAT;
                break;
            case "TRASH_COLLECT":
                material = Material.ROTTEN_FLESH;
                break;
            default:
                material = Material.PAPER;
        }
        
        // Проверяем, активен ли уже этот квест
        boolean isActive = plugin.getQuestService().isQuestActive(player.getUniqueId(), quest.getId().intValue());
        
        // Строим список описания
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + quest.getDescription());
        lore.add("");
        lore.add(ChatColor.GRAY + "Цель: " + quest.getTargetAmount() + " " + 
                getTargetItemDisplayName(quest.getTargetItem()));
        lore.add("");
        lore.add(ChatColor.GOLD + "Награда:");
        if (quest.getRewardCash() > 0) {
            lore.add(ChatColor.YELLOW + "- $" + quest.getRewardCash() + " наличными");
        }
        if (quest.getRewardCardMoney() > 0) {
            lore.add(ChatColor.YELLOW + "- $" + quest.getRewardCardMoney() + " на карту");
        }
        lore.add("");
        
        if (isActive) {
            lore.add(ChatColor.GREEN + "✓ Квест принят");
            // Добавляем информацию о прогрессе
            QuestProgress progress = plugin.getQuestService().getQuestProgress(player.getUniqueId(), quest.getId());
            if (progress != null) {
                int current = progress.getCurrentAmount();
                int target = quest.getTargetAmount();
                lore.add(ChatColor.GRAY + "Прогресс: " + current + "/" + target);
            }
        } else {
            lore.add(ChatColor.YELLOW + "Нажмите, чтобы принять квест");
        }
        
        // Создаем иконку
        return new ItemBuilder(material)
                .setName(ChatColor.GOLD + quest.getTitle())
                .setLore(lore)
                .setPersistentData(questIdKey, PersistentDataType.STRING, String.valueOf(quest.getId()))
                .build();
    }
    
    /**
     * Получает доступные квесты для NPC
     */
    private List<QuestData> getAvailableQuests(Player player, QuestGiverType npcType) {
        UUID playerUuid = player.getUniqueId();
        List<QuestData> allQuests = plugin.getQuestService().getAllQuests();
        List<QuestData> availableQuests = new ArrayList<>();
        
        for (QuestData quest : allQuests) {
            // Проверяем, подходит ли квест для этого типа NPC
            if (!isQuestSuitableForNpcType(quest, npcType)) {
                continue;
            }
            
            // Проверяем, не выполнен ли уже неповторяемый квест
            if (!quest.isRepeatable() && plugin.getQuestService().isQuestCompleted(playerUuid, quest.getId().intValue())) {
                continue;
            }
            
            // Проверяем, не активен ли уже этот квест
            boolean isActive = plugin.getQuestService().isQuestActive(playerUuid, quest.getId().intValue());
            
            // Если квест активен, показываем его для отслеживания прогресса
            // Если не активен, добавляем как доступный для принятия
            availableQuests.add(quest);
        }
        
        return availableQuests;
    }
    
    /**
     * Проверяет, подходит ли квест для данного типа NPC
     */
    private boolean isQuestSuitableForNpcType(QuestData quest, QuestGiverType npcType) {
        switch (npcType) {
            case STREET_DEALER:
                // Уличный торговец предлагает базовые квесты на продажу и доставку
                return quest.getQuestType().equalsIgnoreCase("BUY_DRUG") ||
                       quest.getQuestType().equalsIgnoreCase("SELL_DRUG") ||
                       quest.getQuestType().equalsIgnoreCase("TRASH_COLLECT");
                
            case BUSINESSMAN:
                // Бизнесмен предлагает более сложные квесты на доставку и выращивание
                return quest.getQuestType().equalsIgnoreCase("SELL_DRUG") ||
                       quest.getQuestType().equalsIgnoreCase("GROW_PLANT") ||
                       quest.getQuestType().equalsIgnoreCase("GROW_PLANT_QUALITY");
                
            case JUNKIE:
                // Наркоман предлагает простые квесты на покупку и доставку
                return quest.getQuestType().equalsIgnoreCase("BUY_DRUG") ||
                       quest.getQuestType().equalsIgnoreCase("SELL_DRUG");
                
            default:
                return true;
        }
    }
    
    /**
     * Обработчик клика по инвентарю
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Проверяем, что это меню квестов
        if (!isQuestMenu(inventory)) {
            return;
        }
        
        // Отменяем стандартное поведение
        event.setCancelled(true);
        
        // Обрабатываем клик по предмету
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || 
                clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }
        
        // Получаем ID квеста из String
        if (!clickedItem.getItemMeta().getPersistentDataContainer().has(questIdKey, PersistentDataType.STRING)) {
            return;
        }
        
        String questIdStr = clickedItem.getItemMeta().getPersistentDataContainer().get(questIdKey, PersistentDataType.STRING);
        int questId;
        try {
            questId = Integer.parseInt(questIdStr);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Некорректный ID квеста: " + questIdStr);
            return;
        }
        
        // Проверяем, активен ли уже этот квест
        if (plugin.getQuestService().isQuestActive(player.getUniqueId(), questId)) {
            player.sendMessage(ChatColor.YELLOW + "Этот квест уже активен.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Активируем квест
        boolean success = plugin.getQuestService().startQuest(player, questId);
        
        if (success) {
            // Получаем информацию о квесте
            QuestData quest = plugin.getQuestService().getQuestById(questId);
            
            // Уведомляем игрока
            player.sendMessage(ChatColor.GREEN + "Вы приняли квест: " + quest.getTitle());
            player.sendMessage(ChatColor.GRAY + quest.getDescription());
            player.sendMessage(ChatColor.GRAY + "Цель: " + quest.getTargetAmount() + " " + 
                    getTargetItemDisplayName(quest.getTargetItem()));
            
            // Воспроизводим звук
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // Закрываем инвентарь
            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось принять квест.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
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
        Inventory inventory = event.getInventory();
        
        // Проверяем, что это меню квестов
        if (!isQuestMenu(inventory)) {
            return;
        }
        
        // Удаляем меню из списка открытых
        openMenus.remove(player.getUniqueId());
    }
    
    /**
     * Проверяет, является ли инвентарь меню квестов
     */
    private boolean isQuestMenu(Inventory inventory) {
        ItemStack firstItem = inventory.getItem(0);
        if (firstItem == null || !firstItem.hasItemMeta()) {
            return false;
        }
        
        return firstItem.getItemMeta().getPersistentDataContainer().has(questMenuKey, PersistentDataType.STRING);
    }
    
    /**
     * Возвращает отображаемое имя цели квеста
     */
    private String getTargetItemDisplayName(String targetItem) {
        switch (targetItem.toUpperCase()) {
            case "STONE":
                return "камня";
            case "OAK_LOG":
                return "дубовых бревен";
            case "ZOMBIE":
                return "зомби";
            case "MARIJUANA":
                return "марихуаны";
            case "COCAINE":
                return "кокаина";
            case "METH":
                return "метамфетамина";
            case "HEROIN":
                return "героина";
            case "ANY":
                return "любого наркотика";
            case "TRASH":
                return "мусора";
            default:
                return targetItem.toLowerCase();
        }
    }
    
    /**
     * Возвращает отображаемое имя типа NPC
     */
    private String getNpcTypeName(QuestGiverType type) {
        switch (type) {
            case STREET_DEALER:
                return "Уличный торговец";
            case BUSINESSMAN:
                return "Бизнесмен";
            case JUNKIE:
                return "Наркоман";
            default:
                return "Неизвестный";
        }
    }
} 