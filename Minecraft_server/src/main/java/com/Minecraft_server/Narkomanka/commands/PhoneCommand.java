package com.Minecraft_server.Narkomanka.commands;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.npc.PhoneBoothNPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PhoneCommand implements CommandExecutor, TabCompleter {

    private final Narkomanka plugin;
    private final PhoneBoothNPC phoneBoothNPC;

    public PhoneCommand(Narkomanka plugin, PhoneBoothNPC phoneBoothNPC) {
        this.plugin = plugin;
        this.phoneBoothNPC = phoneBoothNPC;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(sender);
                return true;

            case "simulate_call":
                if (!sender.hasPermission("narkomanka.admin") && !sender.hasPermission("narkomanka.phone.simulate")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав на выполнение этой команды.");
                    return true;
                }

                if (args.length > 1) {
                    // Simulate call for another player
                    String playerName = args[1];
                    Player targetPlayer = Bukkit.getPlayer(playerName);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        phoneBoothNPC.simulatePhoneCall(targetPlayer);
                        sender.sendMessage(ChatColor.GREEN + "Звонок симулирован для игрока " + targetPlayer.getName() + ".");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Игрок " + playerName + " не найден или не в сети.");
                    }
                } else if (sender instanceof Player) {
                    // Simulate call for the sender
                    phoneBoothNPC.simulatePhoneCall((Player) sender);
                    sender.sendMessage(ChatColor.GREEN + "Звонок симулирован.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Укажите имя игрока для симуляции звонка.");
                }
                return true;

            case "give":
                if (!sender.hasPermission("narkomanka.admin") && !sender.hasPermission("narkomanka.phone.give")) {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав на выполнение этой команды.");
                    return true;
                }

                // Проверяем аргументы для качества телефона
                int quality = 1; // По умолчанию самое низкое качество
                if (args.length > 1) {
                    try {
                        quality = Integer.parseInt(args[1]);
                        if (quality < 1) quality = 1;
                        if (quality > 4) quality = 4;
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Некорректное качество. Используйте числа от 1 до 4.");
                        return true;
                    }
                }
                
                // Определяем получателя
                Player targetPlayer;
                if (args.length > 2) {
                    String playerName = args[2];
                    targetPlayer = Bukkit.getPlayer(playerName);
                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        sender.sendMessage(ChatColor.RED + "Игрок " + playerName + " не найден или не в сети.");
                        return true;
                    }
                } else if (sender instanceof Player) {
                    targetPlayer = (Player) sender;
                } else {
                    sender.sendMessage(ChatColor.RED + "Укажите имя игрока для выдачи телефона.");
                    return true;
                }

                // Создаем и выдаем телефон
                ItemStack phoneItem = plugin.getPhoneItem().createPhone(quality);
                targetPlayer.getInventory().addItem(phoneItem);
                
                String qualityName = getQualityName(quality);
                sender.sendMessage(ChatColor.GREEN + "Телефон (" + qualityName + ") выдан игроку " + targetPlayer.getName() + ".");
                if (targetPlayer != sender) {
                    targetPlayer.sendMessage(ChatColor.GREEN + "Вы получили телефон (" + qualityName + ")!");
                }
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Неизвестная команда. Используйте /phone help для справки.");
        return true;
    }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - subcommand
            List<String> subcommands = new ArrayList<>();
            subcommands.add("help");
            
            if (sender.hasPermission("narkomanka.admin") || sender.hasPermission("narkomanka.phone.simulate")) {
                subcommands.add("simulate_call");
            }
            
            if (sender.hasPermission("narkomanka.admin") || sender.hasPermission("narkomanka.phone.give")) {
                subcommands.add("give");
            }
            
            for (String subcmd : subcommands) {
                if (subcmd.startsWith(args[0].toLowerCase())) {
                    completions.add(subcmd);
                }
            }
        } else if (args.length == 2) {
            // Second argument
            if (args[0].equalsIgnoreCase("simulate_call")) {
                // Player name for simulate_call
                return null; // Return null for default player name completion
            } else if (args[0].equalsIgnoreCase("give")) {
                // Quality for give
                List<String> qualities = Arrays.asList("1", "2", "3", "4");
                for (String quality : qualities) {
                    if (quality.startsWith(args[1])) {
                        completions.add(quality);
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                // Player name for give
                return null; // Return null for default player name completion
            }
        }

        return completions;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Помощь по команде /phone ===");
        sender.sendMessage(ChatColor.YELLOW + "/phone help" + ChatColor.WHITE + " - Показать это сообщение");
        
        if (sender.hasPermission("narkomanka.admin") || sender.hasPermission("narkomanka.phone.simulate")) {
            sender.sendMessage(ChatColor.YELLOW + "/phone simulate_call [игрок]" + ChatColor.WHITE + " - Симулировать телефонный звонок");
        }
        
        if (sender.hasPermission("narkomanka.admin") || sender.hasPermission("narkomanka.phone.give")) {
            sender.sendMessage(ChatColor.YELLOW + "/phone give [качество] [игрок]" + ChatColor.WHITE + " - Выдать телефон");
            sender.sendMessage(ChatColor.GRAY + "  Качество: 1 - Простой, 2 - Улучшенный, 3 - Продвинутый, 4 - Премиальный");
        }
    }
    
    /**
     * Возвращает название качества телефона
     */
    private String getQualityName(int quality) {
        switch (quality) {
            case 1:
                return "Простой";
            case 2:
                return "Улучшенный";
            case 3:
                return "Продвинутый";
            case 4:
                return "Премиальный";
            default:
                return "Неизвестное качество";
        }
    }
}