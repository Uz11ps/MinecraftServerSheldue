package com.Minecraft_server.Narkomanka.listeners;

import com.Minecraft_server.Narkomanka.Narkomanka;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Sound;
import java.util.HashMap;
import java.util.Random;

public class GrowBoxListener implements Listener {
    
    private final Narkomanka plugin;
    private final NamespacedKey growBoxInventoryKey;
    private final NamespacedKey seedTypeKey;
    private final NamespacedKey plantQualityKey;
    private final Random random = new Random();
    
    public GrowBoxListener(Narkomanka plugin) {
        this.plugin = plugin;
        this.growBoxInventoryKey = new NamespacedKey(plugin, "grow_box_inventory_id");
        this.seedTypeKey = new NamespacedKey(plugin, "seed_type");
        this.plantQualityKey = new NamespacedKey(plugin, "plant_quality");
        
        // Регистрируем слушатель
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GREEN + "Гроубокс")) {
            // Проверяем, что игрок кликнул в верхний инвентарь
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                // Проверяем, что это действительно инвентарь гроубокса
                ItemStack firstItem = event.getView().getTopInventory().getItem(0);
                if (firstItem != null && firstItem.hasItemMeta() && 
                        firstItem.getItemMeta().getPersistentDataContainer().has(growBoxInventoryKey, PersistentDataType.STRING)) {
                    
                    // Обрабатываем клик в гроубокс
                    handleGrowBoxClick(event);
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GREEN + "Гроубокс")) {
            // Запрещаем перетаскивание в верхний инвентарь
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    /**
     * Обрабатывает клики в инвентаре гроубокса
     */
    private void handleGrowBoxClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        Player player = (Player) event.getWhoClicked();
        
        // По умолчанию отменяем действие
        event.setCancelled(true);
        
        // Обрабатываем клики в специальные слоты
        switch (slot) {
            case 10: // Слот для почвы
                handleSoilSlot(event, currentItem, cursorItem, player);
                break;
                
            case 13: // Слот для растения/семян
                handlePlantSlot(event, currentItem, cursorItem, player);
                break;
                
            case 16: // Слот для воды
                handleWaterSlot(event, currentItem, cursorItem, player);
                break;
                
            case 22: // Слот для удобрения
                handleFertilizerSlot(event, currentItem, cursorItem, player);
                break;
                
            default:
                // Для остальных слотов не делаем ничего
                break;
        }
    }
    
    /**
     * Обрабатывает слот для почвы
     */
    private void handleSoilSlot(InventoryClickEvent event, ItemStack currentItem, ItemStack cursorItem, Player player) {
        // Если слот не занят стеклянной панелью
        if (currentItem != null && currentItem.getType() != Material.BROWN_STAINED_GLASS_PANE) {
            // Если игрок кликнул пустой рукой, забираем почву
            if (cursorItem.getType() == Material.AIR) {
                event.setCancelled(false);
                player.sendMessage(ChatColor.YELLOW + "Вы забрали почву из гроубокса.");
            }
        } else {
            // Если у игрока в руке почва
            if (cursorItem != null && cursorItem.getType() == Material.DIRT && isSoil(cursorItem)) {
                event.setCancelled(false);
                player.sendMessage(ChatColor.GREEN + "Вы добавили почву в гроубокс.");
            }
        }
    }
    
    /**
     * Обрабатывает слот для растения/семян
     */
    private void handlePlantSlot(InventoryClickEvent event, ItemStack currentItem, ItemStack cursorItem, Player player) {
        // Если в слоте уже есть растение
        if (currentItem != null && currentItem.getType() != Material.LIME_STAINED_GLASS_PANE) {
            // Если растение полностью выросло и игрок пытается его собрать
            if (isFullyGrownPlant(currentItem) && cursorItem.getType() == Material.AIR) {
                // Получаем тип и качество растения
                String plantType = getPlantType(currentItem);
                int plantQuality = getPlantQuality(currentItem);
                
                // Получаем базовое количество продукта (3-7 единиц)
                int baseAmount = 3 + random.nextInt(5);
                
                // Увеличиваем количество в зависимости от качества
                baseAmount += (plantQuality - 1);
                
                // Создаем предмет с наркотиком
                ItemStack drugProduct = plugin.getGrowSystem().createDrugProduct(
                        plantType, 
                        plantQuality, 
                        baseAmount
                );
                
                // Выдаем продукт игроку
                HashMap<Integer, ItemStack> notAdded = player.getInventory().addItem(drugProduct);
                
                // Если в инвентаре не хватило места, выбрасываем предметы на землю
                if (!notAdded.isEmpty()) {
                    for (ItemStack item : notAdded.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
                
                // Информируем игрока
                player.sendMessage(ChatColor.GREEN + "Вы собрали " + baseAmount + " ед. " + 
                        getDisplayNameForType(plantType) + " " + getQualityDisplayName(plantQuality) + " качества!");
                
                // Иногда даем семена (50% шанс)
                if (random.nextDouble() < 0.5) {
                    int seedQuality = Math.min(4, plantQuality + (random.nextDouble() < 0.3 ? 1 : 0));
                    ItemStack seeds = plugin.getGrowSystem().createSeeds(plantType, seedQuality);
                    player.getInventory().addItem(seeds);
                    player.sendMessage(ChatColor.GREEN + "Вы получили семена " + getQualityDisplayName(seedQuality) + " качества!");
                }
                
                // Очищаем слот растения
                event.getClickedInventory().setItem(13, createEmptyPlantSlot());
                
                // Воспроизводим звуковой эффект сбора
                player.playSound(player.getLocation(), Sound.ITEM_CROP_PLANT, 1.0f, 1.0f);
            } else if (cursorItem.getType() == Material.AIR) {
                player.sendMessage(ChatColor.YELLOW + "Растение еще не выросло.");
            }
        } else {
            // Если у игрока в руке семена
            if (cursorItem != null && isSeeds(cursorItem)) {
                event.setCancelled(false);
                player.sendMessage(ChatColor.GREEN + "Вы посадили семена в гроубокс.");
                
                // Воспроизводим звуковой эффект посадки
                player.playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.0f, 1.0f);
            }
        }
    }
    
    /**
     * Получает тип растения из ItemStack
     */
    private String getPlantType(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return "marijuana"; // По умолчанию
        }
        
        // Ищем строку с типом в описании
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains(ChatColor.GRAY + "Тип: ")) {
                String typeName = ChatColor.stripColor(line).replace("Тип: ", "");
                
                // Преобразуем отображаемое имя обратно в идентификатор
                if (typeName.equals("Марихуана")) return "marijuana";
                if (typeName.equals("Кока")) return "coca";
                if (typeName.equals("Мак")) return "poppy";
                
                return "marijuana"; // По умолчанию, если не удалось определить
            }
        }
        
        return "marijuana"; // По умолчанию
    }
    
    /**
     * Получает качество растения из ItemStack
     */
    private int getPlantQuality(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return 1; // По умолчанию - низкое качество
        }
        
        // Ищем строку с качеством в описании
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains(ChatColor.GRAY + "Качество: ")) {
                String qualityName = ChatColor.stripColor(line).replace("Качество: ", "");
                
                // Преобразуем отображаемое имя в числовое значение
                if (qualityName.startsWith("Низкое")) return 1;
                if (qualityName.startsWith("Среднее")) return 2;
                if (qualityName.startsWith("Высокое")) return 3;
                if (qualityName.startsWith("Превосходное")) return 4;
                
                return 1; // По умолчанию, если не удалось определить
            }
        }
        
        return 1; // По умолчанию
    }
    
    /**
     * Возвращает отображаемое имя для типа растения
     */
    private String getDisplayNameForType(String type) {
        switch (type) {
            case "marijuana":
                return "марихуаны";
            case "coca":
                return "листьев коки";
            case "poppy":
                return "опиума";
            default:
                return "растения";
        }
    }
    
    /**
     * Возвращает отображаемое имя для качества
     */
    private String getQualityDisplayName(int quality) {
        switch (quality) {
            case 1:
                return "низкого";
            case 2:
                return "среднего";
            case 3:
                return "высокого";
            case 4:
                return "превосходного";
            default:
                return "обычного";
        }
    }
    
    /**
     * Обрабатывает слот для воды
     */
    private void handleWaterSlot(InventoryClickEvent event, ItemStack currentItem, ItemStack cursorItem, Player player) {
        // Если слот не занят стеклянной панелью
        if (currentItem != null && currentItem.getType() != Material.BLUE_STAINED_GLASS_PANE) {
            // Если игрок кликнул пустой рукой, забираем воду
            if (cursorItem.getType() == Material.AIR) {
                event.setCancelled(false);
                player.sendMessage(ChatColor.YELLOW + "Вы забрали воду из гроубокса.");
            }
        } else {
            // Если у игрока в руке вода
            if (cursorItem != null && cursorItem.getType() == Material.WATER_BUCKET && isWaterForPlants(cursorItem)) {
                event.setCancelled(false);
                player.sendMessage(ChatColor.GREEN + "Вы добавили воду в гроубокс.");
            }
        }
    }
    
    /**
     * Обрабатывает слот для удобрения
     */
    private void handleFertilizerSlot(InventoryClickEvent event, ItemStack currentItem, ItemStack cursorItem, Player player) {
        // Если слот не занят стеклянной панелью
        if (currentItem != null && currentItem.getType() != Material.YELLOW_STAINED_GLASS_PANE) {
            // Если игрок кликнул пустой рукой, забираем удобрение
            if (cursorItem.getType() == Material.AIR) {
                event.setCancelled(false);
                player.sendMessage(ChatColor.YELLOW + "Вы забрали удобрение из гроубокса.");
            }
        } else {
            // Если у игрока в руке удобрение
            if (cursorItem != null && cursorItem.getType() == Material.BONE_MEAL && isFertilizer(cursorItem)) {
                event.setCancelled(false);
                player.sendMessage(ChatColor.GREEN + "Вы добавили удобрение в гроубокс.");
            }
        }
    }
    
    /**
     * Создает пустой слот для растения
     */
    private ItemStack createEmptyPlantSlot() {
        ItemStack emptySlot = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        
        org.bukkit.inventory.meta.ItemMeta meta = emptySlot.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Слот для семян");
        
        java.util.ArrayList<String> lore = new java.util.ArrayList<>();
        lore.add(ChatColor.GRAY + "Поместите сюда семена");
        meta.setLore(lore);
        
        emptySlot.setItemMeta(meta);
        
        return emptySlot;
    }
    
    /**
     * Проверяет, является ли предмет семенами
     */
    private boolean isSeeds(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(seedTypeKey, PersistentDataType.STRING);
    }
    
    /**
     * Проверяет, является ли предмет почвой
     */
    private boolean isSoil(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(new NamespacedKey(plugin, "soil_quality"), PersistentDataType.INTEGER);
    }
    
    /**
     * Проверяет, является ли предмет водой для растений
     */
    private boolean isWaterForPlants(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(new NamespacedKey(plugin, "water_for_plants"), PersistentDataType.BYTE);
    }
    
    /**
     * Проверяет, является ли предмет удобрением
     */
    private boolean isFertilizer(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(new NamespacedKey(plugin, "fertilizer_quality"), PersistentDataType.INTEGER);
    }
    
    /**
     * Проверяет, полностью ли выросло растение
     */
    private boolean isFullyGrownPlant(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return false;
        }
        
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains(ChatColor.GREEN + "✓ Готово к сбору")) {
                return true;
            }
        }
        
        return false;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GREEN + "Гроубокс")) {
            // Здесь можно добавить логику сохранения состояния гроубокса
            // после закрытия инвентаря
        }
    }
} 