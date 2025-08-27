package com.Minecraft_server.Narkomanka.commands;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.world.GrowSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GrowCommands implements CommandExecutor, TabCompleter {

    private final Narkomanka plugin;
    private final GrowSystem growSystem;

    public GrowCommands(Narkomanka plugin, GrowSystem growSystem) {
        this.plugin = plugin;
        this.growSystem = growSystem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда доступна только для игроков.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "help":
                sendHelpMessage(player);
                return true;
            case "box":
                return handleGrowBoxCommand(player, args);
            case "seed":
                return handleSeedCommand(player, args);
            case "soil":
                return handleSoilCommand(player, args);
            case "water":
                return handleWaterCommand(player);
            case "fertilizer":
                return handleFertilizerCommand(player, args);
            default:
                player.sendMessage(ChatColor.RED + "Неизвестная подкоманда. Используйте /grow help для справки.");
                return true;
        }
    }

    /**
     * Отправляет справочное сообщение
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== Система выращивания ===");
        player.sendMessage(ChatColor.YELLOW + "/grow help " + ChatColor.GRAY + "- Показать эту справку");
        player.sendMessage(ChatColor.YELLOW + "/grow box " + ChatColor.GRAY + "- Получить гроубокс");
        player.sendMessage(ChatColor.YELLOW + "/grow seed <тип> [качество] " + ChatColor.GRAY + "- Получить семена (marijuana/coca/poppy)");
        player.sendMessage(ChatColor.YELLOW + "/grow soil [качество] " + ChatColor.GRAY + "- Получить почву");
        player.sendMessage(ChatColor.YELLOW + "/grow water " + ChatColor.GRAY + "- Получить воду для полива");
        player.sendMessage(ChatColor.YELLOW + "/grow fertilizer [качество] " + ChatColor.GRAY + "- Получить удобрение");
    }

    /**
     * Обрабатывает команду получения гроубокса
     */
    private boolean handleGrowBoxCommand(Player player, String[] args) {
        if (!player.hasPermission("narkomanka.admin") && !player.hasPermission("narkomanka.grow.box")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return true;
        }

        ItemStack growBox = growSystem.createGrowBox();
        player.getInventory().addItem(growBox);
        player.sendMessage(ChatColor.GREEN + "Вы получили гроубокс.");
        return true;
    }

    /**
     * Обрабатывает команду получения семян
     */
    private boolean handleSeedCommand(Player player, String[] args) {
        if (!player.hasPermission("narkomanka.admin") && !player.hasPermission("narkomanka.grow.seed")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /grow seed <тип> [качество]");
            player.sendMessage(ChatColor.GRAY + "Типы: marijuana, coca, poppy");
            player.sendMessage(ChatColor.GRAY + "Качество: 1-4 (по умолчанию 1)");
            return true;
        }

        String type = args[1].toLowerCase();
        if (!Arrays.asList("marijuana", "coca", "poppy").contains(type)) {
            player.sendMessage(ChatColor.RED + "Неизвестный тип семян. Доступные типы: marijuana, coca, poppy");
            return true;
        }

        int quality = 1;
        if (args.length >= 3) {
            try {
                quality = Integer.parseInt(args[2]);
                if (quality < 1 || quality > 4) {
                    player.sendMessage(ChatColor.RED + "Качество должно быть от 1 до 4.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Неверный формат качества. Используйте число от 1 до 4.");
                return true;
            }
        }

        ItemStack seeds = growSystem.createSeeds(type, quality);
        player.getInventory().addItem(seeds);
        player.sendMessage(ChatColor.GREEN + "Вы получили семена " + type + " качества " + quality + ".");
        return true;
    }

    /**
     * Обрабатывает команду получения почвы
     */
    private boolean handleSoilCommand(Player player, String[] args) {
        if (!player.hasPermission("narkomanka.admin") && !player.hasPermission("narkomanka.grow.soil")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return true;
        }

        int quality = 1;
        if (args.length >= 2) {
            try {
                quality = Integer.parseInt(args[1]);
                if (quality < 1 || quality > 3) {
                    player.sendMessage(ChatColor.RED + "Качество почвы должно быть от 1 до 3.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Неверный формат качества. Используйте число от 1 до 3.");
                return true;
            }
        }

        ItemStack soil = growSystem.createSoil(quality);
        player.getInventory().addItem(soil);
        player.sendMessage(ChatColor.GREEN + "Вы получили почву качества " + quality + ".");
        return true;
    }

    /**
     * Обрабатывает команду получения воды
     */
    private boolean handleWaterCommand(Player player) {
        if (!player.hasPermission("narkomanka.admin") && !player.hasPermission("narkomanka.grow.water")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return true;
        }

        ItemStack water = growSystem.createWater();
        player.getInventory().addItem(water);
        player.sendMessage(ChatColor.GREEN + "Вы получили воду для полива растений.");
        return true;
    }

    /**
     * Обрабатывает команду получения удобрения
     */
    private boolean handleFertilizerCommand(Player player, String[] args) {
        if (!player.hasPermission("narkomanka.admin") && !player.hasPermission("narkomanka.grow.fertilizer")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return true;
        }

        int quality = 1;
        if (args.length >= 2) {
            try {
                quality = Integer.parseInt(args[1]);
                if (quality < 1 || quality > 3) {
                    player.sendMessage(ChatColor.RED + "Качество удобрения должно быть от 1 до 3.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Неверный формат качества. Используйте число от 1 до 3.");
                return true;
            }
        }

        ItemStack fertilizer = growSystem.createFertilizer(quality);
        player.getInventory().addItem(fertilizer);
        player.sendMessage(ChatColor.GREEN + "Вы получили удобрение качества " + quality + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "box", "seed", "soil", "water", "fertilizer"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("seed")) {
                completions.addAll(Arrays.asList("marijuana", "coca", "poppy"));
            } else if (args[0].equalsIgnoreCase("soil") || args[0].equalsIgnoreCase("fertilizer")) {
                completions.addAll(Arrays.asList("1", "2", "3"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("seed")) {
                completions.addAll(Arrays.asList("1", "2", "3", "4"));
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
} 