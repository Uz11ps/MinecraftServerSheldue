package com.Minecraft_server.Narkomanka.listeners;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerQuestProgress;
import com.Minecraft_server.Narkomanka.database.QuestData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.List;
import java.util.UUID;

/**
 * Слушатель для отслеживания прогресса квестов
 */
public class QuestProgressListener implements Listener {
    private final Narkomanka plugin;

    public QuestProgressListener(Narkomanka plugin) {
        this.plugin = plugin;
        
        // Регистрируем слушатель
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("QuestProgressListener registered");
    }

    /**
     * Отслеживание разрушения блоков для квестов
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        
        // Обрабатываем различные типы блоков для квестов
        switch (blockType) {
            case STONE:
                updateBlockBreakProgress(player, "STONE", 1);
                break;
            case OAK_LOG:
            case BIRCH_LOG:
            case SPRUCE_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
                updateBlockBreakProgress(player, "OAK_LOG", 1);
                break;
            default:
                // Ничего не делаем для других блоков
                break;
        }
    }

    /**
     * Отслеживание убийства сущностей для квестов
     */
    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        // Проверяем, что сущность была убита игроком
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        Player player = event.getEntity().getKiller();
        EntityType entityType = event.getEntityType();
        
        // Обрабатываем различные типы сущностей для квестов
        switch (entityType) {
            case ZOMBIE:
                updateEntityKillProgress(player, "ZOMBIE", 1);
                break;
            case SKELETON:
                updateEntityKillProgress(player, "SKELETON", 1);
                break;
            case CREEPER:
                updateEntityKillProgress(player, "CREEPER", 1);
                break;
            default:
                // Ничего не делаем для других сущностей
                break;
        }
    }

    /**
     * Обновляет прогресс квестов на разрушение блоков
     */
    private void updateBlockBreakProgress(Player player, String blockType, int amount) {
        try {
            plugin.getQuestService().updateQuestProgressByType(
                    player.getUniqueId(), "BREAK_BLOCK", blockType, amount);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обновлении прогресса квеста на разрушение блоков: " + e.getMessage());
        }
    }
    
    /**
     * Обновляет прогресс квестов на убийство сущностей
     */
    private void updateEntityKillProgress(Player player, String entityType, int amount) {
        try {
            plugin.getQuestService().updateQuestProgressByType(
                    player.getUniqueId(), "KILL_ENTITY", entityType, amount);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обновлении прогресса квеста на убийство сущностей: " + e.getMessage());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().toString();

        updateQuestsForAction(player, "PLACE_BLOCK", blockType);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        String itemType = event.getItem().getType().toString();

        updateQuestsForAction(player, "CONSUME_ITEM", itemType);
    }

    private void updateQuestsForAction(Player player, String actionType, String targetType) {
        List<PlayerQuestProgress> activeQuests = plugin.getQuestService().getPlayerActiveQuests(player.getUniqueId());

        for (PlayerQuestProgress progress : activeQuests) {
            QuestData quest = progress.getQuest();

            // Check if quest matches the action type and target
            if (quest.getQuestType().equalsIgnoreCase(actionType) &&
                    (quest.getTargetItem().equalsIgnoreCase(targetType) || quest.getTargetItem().equalsIgnoreCase("ANY"))) {

                // Update progress
                boolean updated = true;
                try {
                    plugin.getQuestService().updateQuestProgressByType(player.getUniqueId(), actionType, targetType, 1);
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при обновлении прогресса квеста: " + e.getMessage());
                    updated = false;
                }

                if (updated) {
                    // Check if quest was just completed
                    progress = plugin.getQuestService().getPlayerQuestProgress(player.getUniqueId(), quest.getId());

                    if (progress != null && progress.isCompleted()) {
                        // Send completion message
                        player.sendMessage(Component.text("Задание выполнено: ")
                                .color(NamedTextColor.GREEN)
                                .append(Component.text(quest.getTitle())
                                        .color(NamedTextColor.YELLOW)));

                        // Show rewards
                        if (quest.getRewardCash() > 0) {
                            player.sendMessage(Component.text("Получено наличных: ")
                                    .color(NamedTextColor.GREEN)
                                    .append(Component.text("$" + quest.getRewardCash())
                                            .color(NamedTextColor.GOLD)));
                        }

                        if (quest.getRewardCardMoney() > 0) {
                            player.sendMessage(Component.text("Получено на карту: ")
                                    .color(NamedTextColor.AQUA)
                                    .append(Component.text("$" + quest.getRewardCardMoney())
                                            .color(NamedTextColor.GOLD)));
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Обновляет прогресс квестов определенного типа
     * @param playerUuid UUID игрока
     * @param questType Тип квеста (BREAK_BLOCK, KILL_ENTITY и т.д.)
     * @param targetType Тип цели (STONE, ZOMBIE и т.д.)
     * @param amount Количество для обновления прогресса
     */
    private void updateQuestProgressByType(UUID playerUuid, String questType, String targetType, int amount) {
        try {
            plugin.getQuestService().updateQuestProgressByType(playerUuid, questType, targetType, amount);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обновлении прогресса квеста: " + e.getMessage());
        }
    }
}