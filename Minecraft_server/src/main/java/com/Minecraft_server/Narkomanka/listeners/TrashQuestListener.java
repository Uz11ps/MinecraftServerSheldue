package com.Minecraft_server.Narkomanka.listeners;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerQuestProgress;
import com.Minecraft_server.Narkomanka.database.QuestData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Слушатель для обработки квестов на сбор мусора
 */
public class TrashQuestListener implements Listener {
    private final Narkomanka plugin;

    // Кэш для отслеживания сданного мусора игроками
    private final Map<UUID, Integer> trashCollectedCount = new HashMap<>();

    public TrashQuestListener(Narkomanka plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("TrashQuestListener registered");
    }

    /**
     * Обрабатывает взаимодействие с мусорным баком
     */
    @EventHandler
    public void onPlayerInteractWithTrashStation(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) {
            return;
        }

        // Проверяем, является ли блок мусорным баком
        if (!isTrashStation(clickedBlock)) {
            return;
        }

        // Если игрок не сдает мусор (не правая кнопка), то выходим
        if (!event.getAction().isRightClick()) {
            return;
        }

        // Считаем, сколько мусора сдает игрок
        int trashCount = countTrashItems(player);
        if (trashCount <= 0) {
            return;
        }

        // Обновляем прогресс квестов на сбор мусора
        updateTrashQuestProgress(player, trashCount);
    }

    /**
     * Проверяет, является ли блок мусорным баком
     */
    private boolean isTrashStation(Block block) {
        if (block.hasMetadata("trash_station")) {
            return true;
        }
        return false;
    }

    /**
     * Считает количество мусора у игрока
     */
    private int countTrashItems(Player player) {
        int count = 0;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getTrashManager().isTrash(item)) {
                count += item.getAmount();
            }
        }
        
        return count;
    }

    /**
     * Обновляет прогресс квестов на сбор мусора
     */
    private void updateTrashQuestProgress(Player player, int amount) {
        UUID playerUuid = player.getUniqueId();
        
        try {
            // Уведомляем QuestService о прогрессе в квестах на сбор мусора
            plugin.getQuestService().updateQuestProgressByType(playerUuid, "TRASH_COLLECT", "TRASH", amount);
            
            player.sendMessage(ChatColor.GREEN + "Вы сдали " + amount + " мусора.");
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обновлении прогресса квеста на сбор мусора: " + e.getMessage());
        }
    }

    /**
     * Получает количество собранного мусора для игрока
     */
    public int getCollectedTrashCount(UUID playerUuid) {
        return trashCollectedCount.getOrDefault(playerUuid, 0);
    }

    /**
     * Сбрасывает счетчик собранного мусора для игрока
     */
    public void resetCollectedTrashCount(UUID playerUuid) {
        trashCollectedCount.remove(playerUuid);
    }

    /**
     * Обрабатывает сдачу мусора на станции переработки
     */
    public void onTrashTurnIn(Player player, int trashCount) {
        if (trashCount <= 0) {
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        // Обновляем счетчик собранного мусора
        int currentCount = trashCollectedCount.getOrDefault(playerUuid, 0);
        trashCollectedCount.put(playerUuid, currentCount + trashCount);
        
        // Обновляем прогресс квестов
        updateTrashQuestProgress(player, trashCount);
    }
}