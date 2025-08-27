package com.Minecraft_server.Narkomanka.commands;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.QuestData;
import com.Minecraft_server.Narkomanka.database.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class QuestCommand implements CommandExecutor, TabCompleter {

    private final Narkomanka plugin;

    public QuestCommand(Narkomanka plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Эту команду может использовать только игрок").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                listQuests(player);
                break;
            case "progress":
                showProgress(player);
                break;
            case "info":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /quest info <id>").color(NamedTextColor.RED));
                    return true;
                }
                try {
                    int questId = Integer.parseInt(args[1]);
                    showQuestInfo(player, questId);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("ID квеста должен быть числом").color(NamedTextColor.RED));
                }
                break;
            case "start":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /quest start <id>").color(NamedTextColor.RED));
                    return true;
                }
                try {
                    int questId = Integer.parseInt(args[1]);
                    startQuest(player, questId);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("ID квеста должен быть числом").color(NamedTextColor.RED));
                }
                break;
            case "abandon":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /quest abandon <id>").color(NamedTextColor.RED));
                    return true;
                }
                try {
                    int questId = Integer.parseInt(args[1]);
                    abandonQuest(player, questId);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("ID квеста должен быть числом").color(NamedTextColor.RED));
                }
                break;
            case "help":
                showHelp(player);
                break;
            default:
                player.sendMessage(Component.text("Неизвестная подкоманда. Используйте /quest help для справки").color(NamedTextColor.RED));
                break;
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== Помощь по квестам ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/quest list").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Показать список доступных квестов").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/quest progress").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Показать прогресс ваших активных квестов").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/quest info <id>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Показать информацию о квесте").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/quest start <id>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Начать выполнение квеста").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/quest abandon <id>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Отказаться от квеста").color(NamedTextColor.GRAY)));
    }

    private void listQuests(Player player) {
        List<QuestData> quests = plugin.getQuestService().getAllQuests();

        if (quests.isEmpty()) {
            player.sendMessage(Component.text("Квесты не найдены").color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("=== Доступные квесты ===").color(NamedTextColor.GOLD));
        for (QuestData quest : quests) {
            Component questComponent = Component.text("#" + quest.getId() + ": ").color(NamedTextColor.YELLOW)
                    .append(Component.text(quest.getTitle()).color(NamedTextColor.GREEN));
            
            // Проверяем, активен ли квест для игрока
            boolean isActive = plugin.getQuestService().isQuestActive(player.getUniqueId(), quest.getId().intValue());
            boolean isCompleted = plugin.getQuestService().isQuestCompleted(player.getUniqueId(), quest.getId().intValue());
            
            if (isActive) {
                questComponent = questComponent.append(Component.text(" [В процессе]").color(NamedTextColor.AQUA));
            } else if (isCompleted && !quest.isRepeatable()) {
                questComponent = questComponent.append(Component.text(" [Завершен]").color(NamedTextColor.GRAY));
            } else if (isCompleted && quest.isRepeatable()) {
                questComponent = questComponent.append(Component.text(" [Завершен, повторяемый]").color(NamedTextColor.LIGHT_PURPLE));
            }
            
            player.sendMessage(questComponent);
        }
    }

    private void showProgress(Player player) {
        List<QuestProgress> activeQuests = plugin.getQuestService().getActiveQuests(player.getUniqueId());

        if (activeQuests.isEmpty()) {
            player.sendMessage(Component.text("У вас нет активных квестов").color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("=== Ваши активные квесты ===").color(NamedTextColor.GOLD));
        for (QuestProgress progress : activeQuests) {
            QuestData quest = plugin.getQuestService().getQuestById(progress.getQuestId().intValue());
            if (quest != null) {
                player.sendMessage(Component.text("#" + quest.getId() + ": ").color(NamedTextColor.YELLOW)
                        .append(Component.text(quest.getTitle()).color(NamedTextColor.GREEN))
                        .append(Component.text(" - Прогресс: " + progress.getCurrentAmount() + "/" + quest.getTargetAmount()).color(NamedTextColor.AQUA)));
            }
        }
    }

    private void showQuestInfo(Player player, int questId) {
        QuestData quest = plugin.getQuestService().getQuestById(questId);
        if (quest == null) {
            player.sendMessage(Component.text("Квест с ID " + questId + " не найден").color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("=== Информация о квесте ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("ID: ").color(NamedTextColor.YELLOW)
                .append(Component.text("#" + quest.getId()).color(NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Название: ").color(NamedTextColor.YELLOW)
                .append(Component.text(quest.getTitle()).color(NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Описание: ").color(NamedTextColor.YELLOW)
                .append(Component.text(quest.getDescription()).color(NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Тип: ").color(NamedTextColor.YELLOW)
                .append(Component.text(quest.getQuestType()).color(NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Цель: ").color(NamedTextColor.YELLOW)
                .append(Component.text(quest.getTargetAmount() + " " + quest.getTargetItem()).color(NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Повторяемый: ").color(NamedTextColor.YELLOW)
                .append(Component.text(quest.isRepeatable() ? "Да" : "Нет").color(NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Награда: ").color(NamedTextColor.YELLOW)
                .append(Component.text("$" + quest.getRewardCash() + " наличными, $" + quest.getRewardCardMoney() + " на карту")
                        .color(NamedTextColor.GREEN)));

        // Проверяем статус квеста для игрока
        boolean isActive = plugin.getQuestService().isQuestActive(player.getUniqueId(), questId);
        boolean isCompleted = plugin.getQuestService().isQuestCompleted(player.getUniqueId(), questId);
        
        if (isActive) {
            QuestProgress progress = plugin.getQuestService().getQuestProgress(player.getUniqueId(), Long.valueOf(questId));
            if (progress != null) {
                player.sendMessage(Component.text("Статус: ").color(NamedTextColor.YELLOW)
                        .append(Component.text("В процессе - " + progress.getCurrentAmount() + "/" + quest.getTargetAmount())
                                .color(NamedTextColor.AQUA)));
            } else {
                player.sendMessage(Component.text("Статус: ").color(NamedTextColor.YELLOW)
                        .append(Component.text("В процессе").color(NamedTextColor.AQUA)));
            }
        } else if (isCompleted) {
            player.sendMessage(Component.text("Статус: ").color(NamedTextColor.YELLOW)
                    .append(Component.text("Завершен").color(NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Статус: ").color(NamedTextColor.YELLOW)
                    .append(Component.text("Не начат").color(NamedTextColor.GRAY)));
        }
    }

    private void startQuest(Player player, int questId) {
        // Проверяем, существует ли квест
        QuestData quest = plugin.getQuestService().getQuestById(questId);
        if (quest == null) {
            player.sendMessage(Component.text("Квест с ID " + questId + " не найден").color(NamedTextColor.RED));
            return;
        }

        // Проверяем, не активен ли уже квест
        if (plugin.getQuestService().isQuestActive(player.getUniqueId(), questId)) {
            player.sendMessage(Component.text("Вы уже выполняете этот квест").color(NamedTextColor.RED));
            return;
        }

        // Проверяем, может ли квест быть начат снова
        if (!quest.isRepeatable() && plugin.getQuestService().isQuestCompleted(player.getUniqueId(), questId)) {
            player.sendMessage(Component.text("Вы уже выполнили этот квест, и он не может быть повторен").color(NamedTextColor.RED));
            return;
        }

        // Начинаем квест
        boolean started = plugin.getQuestService().startQuest(player, questId);
        
        if (started) {
            player.sendMessage(Component.text("Вы начали выполнение квеста: ").color(NamedTextColor.GREEN)
                    .append(Component.text(quest.getTitle()).color(NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("Цель: ").color(NamedTextColor.GREEN)
                    .append(Component.text(quest.getTargetAmount() + " " + quest.getTargetItem()).color(NamedTextColor.YELLOW)));
        } else {
            player.sendMessage(Component.text("Не удалось начать выполнение квеста").color(NamedTextColor.RED));
        }
    }

    private void abandonQuest(Player player, int questId) {
        // Проверяем, активен ли квест
        if (!plugin.getQuestService().isQuestActive(player.getUniqueId(), questId)) {
            player.sendMessage(Component.text("У вас нет активного квеста с ID " + questId).color(NamedTextColor.RED));
            return;
        }

        // Проверяем существование квеста
        QuestData quest = plugin.getQuestService().getQuestById(questId);
        if (quest == null) {
            player.sendMessage(Component.text("Квест с ID " + questId + " не найден").color(NamedTextColor.RED));
            return;
        }

        // Отменяем квест (метод нужно реализовать в QuestService)
        boolean abandoned = plugin.getQuestService().abandonQuest(player.getUniqueId(), Long.valueOf(questId));
        
        if (abandoned) {
            player.sendMessage(Component.text("Вы отказались от выполнения квеста: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(quest.getTitle()).color(NamedTextColor.RED)));
        } else {
            player.sendMessage(Component.text("Не удалось отказаться от квеста").color(NamedTextColor.RED));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("list", "progress", "info", "start", "abandon", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "info":
                case "start":
                case "abandon":
                    // Предлагаем список ID квестов для этих команд
                    List<String> questIds = new ArrayList<>();
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        List<QuestData> quests = plugin.getQuestService().getAllQuests();
                        for (QuestData quest : quests) {
                            questIds.add(String.valueOf(quest.getId()));
                        }
                    }
                    return questIds.stream()
                            .filter(s -> s.startsWith(args[1]))
                            .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}