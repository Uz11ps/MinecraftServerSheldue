package com.Minecraft_server.Narkomanka.ui;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerData;
import com.Minecraft_server.Narkomanka.database.PlayerQuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UIManager {
    private final Narkomanka plugin;
    private final Map<UUID, BukkitTask> hudTasks = new HashMap<>();
    private final DecimalFormat moneyFormat = new DecimalFormat("$#,##0.00");

    public UIManager(Narkomanka plugin) {
        this.plugin = plugin;
    }

    public void setupPlayerHUD(Player player) {
        // Cancel any existing task
        BukkitTask existingTask = hudTasks.get(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Create new task to update HUD every second
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            updateQuestSidebar(player);
            updateBalanceActionBar(player);
        }, 0L, 20L); // Update every second (20 ticks)

        // Fixed: Added the task as the value in the HashMap
        hudTasks.put(player.getUniqueId(), task);
    }

    public void removePlayerHUD(Player player) {
        BukkitTask task = hudTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        // Remove sidebar scoreboard
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    private void updateQuestSidebar(Player player) {
        try {
            List<PlayerQuestProgress> activeQuests = plugin.getQuestService().getPlayerActiveQuests(player.getUniqueId());
            if (activeQuests.isEmpty()) {
                return; // No quests to display
            }

            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("quests", "dummy",
                    Component.text("АКТИВНЫЕ ЗАДАНИЯ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            int scoreValue = activeQuests.size() * 3; // Space for title, progress, and empty line for each quest

            for (PlayerQuestProgress progress : activeQuests) {
                String questTitle = progress.getQuest().getTitle();
                int currentProgress = progress.getCurrentProgress();
                int targetAmount = progress.getQuest().getTargetAmount();

                // Register team for the quest title (to avoid 16 char limit)
                String entryTitle = "quest_title_" + progress.getQuest().getId();
                if (entryTitle.length() > 16) {
                    entryTitle = entryTitle.substring(0, 16); // Ensure entry is not too long
                }

                Team titleTeam = scoreboard.registerNewTeam(entryTitle);
                titleTeam.addEntry(entryTitle);
                titleTeam.prefix(Component.text(questTitle).color(NamedTextColor.YELLOW));
                objective.getScore(entryTitle).setScore(scoreValue--);

                // Add progress line
                String entryProgress = "quest_prog_" + progress.getQuest().getId();
                if (entryProgress.length() > 16) {
                    entryProgress = entryProgress.substring(0, 16); // Ensure entry is not too long
                }

                Team progressTeam = scoreboard.registerNewTeam(entryProgress);
                progressTeam.addEntry(entryProgress);
                progressTeam.prefix(Component.text("  Прогресс: " + currentProgress + "/" + targetAmount)
                        .color(currentProgress >= targetAmount ? NamedTextColor.GREEN : NamedTextColor.GRAY));
                objective.getScore(entryProgress).setScore(scoreValue--);

                // Empty line
                String entryEmpty = "quest_emp_" + progress.getQuest().getId();
                if (entryEmpty.length() > 16) {
                    entryEmpty = entryEmpty.substring(0, 16); // Ensure entry is not too long
                }

                scoreboard.registerNewTeam(entryEmpty).addEntry(entryEmpty);
                objective.getScore(entryEmpty).setScore(scoreValue--);
            }

            player.setScoreboard(scoreboard);
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating quest sidebar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateBalanceActionBar(Player player) {
        try {
            PlayerData playerData = plugin.getPlayerService().getOrCreatePlayerData(player);

            if (playerData == null) {
                return;
            }

            double cashBalance = playerData.getCashBalance();
            double cardBalance = playerData.getCardBalance();

            Component balanceBar = Component.text("Наличные: ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(moneyFormat.format(cashBalance))
                            .color(NamedTextColor.GOLD))
                    .append(Component.text("  |  "))
                    .append(Component.text("Карта: ")
                            .color(NamedTextColor.AQUA))
                    .append(Component.text(moneyFormat.format(cardBalance))
                            .color(NamedTextColor.GOLD));

            player.sendActionBar(balanceBar);
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating balance action bar: " + e.getMessage());
        }
    }
}