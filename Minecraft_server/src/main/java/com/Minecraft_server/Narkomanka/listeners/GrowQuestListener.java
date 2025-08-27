package com.Minecraft_server.Narkomanka.listeners;

import com.Minecraft_server.Narkomanka.Narkomanka;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class GrowQuestListener implements Listener {
    
    private final Narkomanka plugin;
    private final NamespacedKey plantQualityKey;
    
    public GrowQuestListener(Narkomanka plugin) {
        this.plugin = plugin;
        this.plantQualityKey = new NamespacedKey(plugin, "plant_quality");
        
        // Регистрируем слушатель
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("GrowQuestListener зарегистрирован");
    }
    
    /**
     * Обрабатывает сбор растений для прогресса квестов
     */
    @EventHandler
    public void onPlantHarvest(InventoryClickEvent event) {
        // Проверяем, что это инвентарь гроубокса
        if (!event.getView().getTitle().equals(ChatColor.GREEN + "Гроубокс")) {
            return;
        }
        
        // Проверяем, что клик был по растению
        if (event.getRawSlot() != 13) {
            return;
        }
        
        // Проверяем, что игрок забирает растение пустой рукой
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        if (currentItem == null || event.isCancelled() || cursorItem.getType() != Material.AIR) {
            return;
        }

        // Проверяем, что растение полностью выросло
        if (!isFullyGrownPlant(currentItem)) {
            return;
        }
        
        // Получаем тип и качество растения
        String plantType = getPlantType(currentItem);
        int plantQuality = getPlantQuality(currentItem);
        
        // Обновляем прогресс квестов на выращивание
        Player player = (Player) event.getWhoClicked();
        updateGrowQuestProgress(player, plantType, plantQuality);
    }
    
    /**
     * Обновляет прогресс квестов на выращивание
     */
    private void updateGrowQuestProgress(Player player, String plantType, int plantQuality) {
        UUID playerUuid = player.getUniqueId();
        
        if (!plugin.isDatabaseAvailable()) {
            return;
        }
        
        // Сообщаем сервису квестов о сборе растения
        // Передаем всю необходимую информацию - тип растения и его качество
        plugin.getQuestService().notifyPlantHarvested(playerUuid, plantType, plantQuality);
        
        // QuestService сам обработает обновление прогресса и выполнение соответствующих квестов
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
        if (item == null || !item.hasItemMeta()) {
            return 1; // По умолчанию - низкое качество
        }
        
        // Сначала проверим метаданные
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        if (container.has(plantQualityKey, PersistentDataType.INTEGER)) {
            return container.get(plantQualityKey, PersistentDataType.INTEGER);
        }
        
        // Если метаданных нет, ищем в описании
        if (item.getItemMeta().hasLore()) {
            for (String line : item.getItemMeta().getLore()) {
                if (line.contains(ChatColor.GRAY + "Качество: ")) {
                    String qualityName = ChatColor.stripColor(line).replace("Качество: ", "");
                    
                    // Преобразуем отображаемое имя в числовое значение
                    if (qualityName.startsWith("Низкое")) return 1;
                    if (qualityName.startsWith("Среднее")) return 2;
                    if (qualityName.startsWith("Высокое")) return 3;
                    if (qualityName.startsWith("Превосходное")) return 4;
                }
            }
        }
        
        return 1; // По умолчанию
    }
} 