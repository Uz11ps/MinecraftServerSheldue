package com.Minecraft_server.Narkomanka.commands;

import com.Minecraft_server.Narkomanka.Narkomanka;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Команда для управления NPCs в стиле Schedule I
 */
public class NPCCommand implements CommandExecutor, TabCompleter {
    
    private final Narkomanka plugin;
    
    public NPCCommand(Narkomanka plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда доступна только для игроков.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Проверяем права доступа
        if (!player.hasPermission("narkomanka.admin")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды.");
            return true;
        }
        
        // Проверяем аргументы
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "populate":
                return handlePopulateCommand(player, args);
            case "clear":
                return handleClearCommand(player);
            case "spawn":
                return handleSpawnCommand(player, args);
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    /**
     * Обрабатывает команду на заполнение мира NPC
     */
    private boolean handlePopulateCommand(Player player, String[] args) {
        if (plugin.getNPCManager() == null) {
            player.sendMessage(ChatColor.RED + "NPCManager не инициализирован.");
            return true;
        }
        
        int policeCount = 5;
        int junkieCount = 10;
        int citizenCount = 20;
        
        // Проверяем, указаны ли пользовательские значения
        if (args.length > 1) {
            try {
                policeCount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Некорректное количество полицейских.");
                return true;
            }
        }
        
        if (args.length > 2) {
            try {
                junkieCount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Некорректное количество наркоманов.");
                return true;
            }
        }
        
        if (args.length > 3) {
            try {
                citizenCount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Некорректное количество граждан.");
                return true;
            }
        }
        
        // Заполняем мир NPC
        plugin.getNPCManager().populateWorld(policeCount, junkieCount, citizenCount);
        
        player.sendMessage(ChatColor.GREEN + "Мир заполнен NPC: " + 
                ChatColor.BLUE + policeCount + " полицейских, " + 
                ChatColor.DARK_PURPLE + junkieCount + " наркоманов, " + 
                ChatColor.GREEN + citizenCount + " граждан.");
        
        return true;
    }
    
    /**
     * Обрабатывает команду на очистку NPC
     */
    private boolean handleClearCommand(Player player) {
        // В реальном плагине здесь была бы логика удаления всех NPC
        player.sendMessage(ChatColor.YELLOW + "Эта функция еще не реализована.");
        return true;
    }
    
    /**
     * Обрабатывает команду на создание отдельного NPC
     */
    private boolean handleSpawnCommand(Player player, String[] args) {
        if (plugin.getNPCManager() == null) {
            player.sendMessage(ChatColor.RED + "NPCManager не инициализирован.");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Укажите тип NPC: police, junkie, citizen");
            return true;
        }
        
        String type = args[1].toLowerCase();
        String name = args.length > 2 ? 
                Arrays.stream(args, 2, args.length).collect(Collectors.joining(" ")) : 
                "NPC";
        
        switch (type) {
            case "police":
                plugin.getNPCManager().createPoliceNPC(player.getLocation(), name);
                player.sendMessage(ChatColor.GREEN + "Создан полицейский NPC: " + name);
                break;
            case "junkie":
                plugin.getNPCManager().createJunkieNPC(player.getLocation(), name);
                player.sendMessage(ChatColor.GREEN + "Создан NPC наркоман: " + name);
                break;
            case "citizen":
                plugin.getNPCManager().createCitizenNPC(player.getLocation(), name);
                player.sendMessage(ChatColor.GREEN + "Создан NPC гражданин: " + name);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Неизвестный тип NPC: " + type);
                return true;
        }
        
        return true;
    }
    
    /**
     * Отправляет сообщение с подсказкой
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "=== Помощь по команде /npc ===");
        player.sendMessage(ChatColor.YELLOW + "/npc populate [police] [junkie] [citizen]" + ChatColor.WHITE + " - Заполняет мир NPC");
        player.sendMessage(ChatColor.YELLOW + "/npc spawn <тип> [имя]" + ChatColor.WHITE + " - Создает NPC выбранного типа");
        player.sendMessage(ChatColor.YELLOW + "/npc clear" + ChatColor.WHITE + " - Удаляет всех NPC");
        player.sendMessage("");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("populate");
            completions.add("spawn");
            completions.add("clear");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            completions.add("police");
            completions.add("junkie");
            completions.add("citizen");
        }
        
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
} 